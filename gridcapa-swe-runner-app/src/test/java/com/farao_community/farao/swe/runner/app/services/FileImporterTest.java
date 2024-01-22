/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.craccreation.creator.api.CracCreationContext;
import com.powsybl.openrao.data.craccreation.creator.cim.CimCrac;
import com.farao_community.farao.gridcapa_swe_commons.resource.ProcessType;
import com.farao_community.farao.swe.runner.api.resource.SweFileResource;
import com.farao_community.farao.swe.runner.api.resource.SweRequest;
import com.google.common.base.Suppliers;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.glsk.cim.CimGlskDocument;
import com.powsybl.glsk.commons.ZonalData;

import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.ImportConfig;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@SpringBootTest
class FileImporterTest {

    @Autowired
    private FileImporter fileImporter;
    private final String testDirectory = "/20210209/";
    private final String cimCracFilename = "CIM_21_1_1.xml";
    private final String jsonCracFilename = "cracCimJson.json";
    private final String glskFilename = "cim_Glsk.xml";
    private final String networkFileName = "MicroGrid.zip";
    private final OffsetDateTime dateTime = OffsetDateTime.parse("2021-02-09T19:30Z");
    private Network network;

    @BeforeEach
    void setUp() {
        Properties importParams = new Properties();
        importParams.put("iidm.import.cgmes.source-for-iidm-id", "rdfID");
        network = Network.read(
                Paths.get(new File(getClass().getResource(testDirectory + networkFileName).getFile()).toString()),
                LocalComputationManager.getDefault(),
                Suppliers.memoize(ImportConfig::load).get(),
                importParams);
    }

    @Test
    void testImportCimCrac() {
        SweRequest req = createEmptySweRequest();
        CimCrac cimCrac = fileImporter.importCimCrac(req);
        Assertions.assertNotNull(cimCrac);
    }

    @Test
    void testImportCimCracFromUrlWithNetwork() {
        Properties importParams = new Properties();
        importParams.put("iidm.import.cgmes.source-for-iidm-id", "rdfID");
        Network network = Network.read(
                Paths.get(new File(getClass().getResource(testDirectory + networkFileName).getFile()).toString()),
                LocalComputationManager.getDefault(),
                Suppliers.memoize(ImportConfig::load).get(),
                importParams
        );
        SweRequest req = createEmptySweRequest();
        CracCreationContext cracFrEs = fileImporter.importCracFromCimCracAndNetwork(fileImporter.importCimCrac(req), dateTime, network, null);
        Assertions.assertNotNull(cracFrEs);
        CracCreationContext cracEsPt = fileImporter.importCracFromCimCracAndNetwork(fileImporter.importCimCrac(req), dateTime, network, FilesService.CRAC_CIM_CRAC_CREATION_PARAMETERS_PT_ES_IDCC_JSON);
        Assertions.assertNotNull(cracEsPt);
    }

    @Test
    void testImportCracFromJson() {
        Crac cracFromJson = fileImporter.importCracFromJson(Objects.requireNonNull(getClass().getResource(testDirectory + jsonCracFilename)).toString());
        assertNotNull(cracFromJson);
    }

    SweRequest createEmptySweRequest() {
        return new SweRequest("id", ProcessType.D2CC, dateTime, null, null, null, null, null, null, null, null, null, null,
                new SweFileResource("cracfile", getClass().getResource(testDirectory + cimCracFilename).toExternalForm()), null, null, null);
    }

    @Test
    void importGlskTest() {
        /* Generators
        _9c3b8f97-7972-477d-9dc8-87365cc0ad0e NL G1 minP = 300 maxP = 1000, targetP = 600.49
        _2844585c-0d35-488d-a449-685bcd57afbf NL G2 minP = 130 maxP = 250, targetP = 140.0
        _1dc9afba-23b5-41a0-8540-b479ed8baf4b NL G3 minP = 130 maxP = 250, targetP = 150.0

        _550ebe0d-f2b2-48c1-991f-cebea43a21aa BE G2 minP = 50 maxP = 200, targetP = 118
        _3a3b27be-b18b-4385-b557-6735d733baf0 BE G1 minP = -100 maxP = 200, targetP = 90
         */
        Instant instant = Instant.parse("2021-02-09T19:30:00Z");
        ZonalData<Scalable> zonalScalables = fileImporter.importGlsk(getClass().getResource(testDirectory + glskFilename).toString(), network, instant);
        assertEquals(2, zonalScalables.getDataPerZone().size());
        Scalable scalableNL = zonalScalables.getData("10YNL----------L"); //type B45 curve type A03
        assertEquals(3, scalableNL.filterInjections(network).size());
        Scalable scalableBE = zonalScalables.getData("10YBE----------2"); //type B45 curve type A01
        assertEquals(4, scalableBE.filterInjections(network).size());
        assertEquals(192., scalableBE.scale(network, 192), 0.001);
        assertEquals(-450., scalableBE.scale(network, -500), 0.001);

    }

    @Test
    void importCimGlskTest() {
        Instant instant = Instant.parse("2021-02-09T19:30:00Z");
        CimGlskDocument cimGlskDocument = fileImporter.importCimGlskDocument(getClass().getResource(testDirectory + glskFilename).toString());
        ZonalData<Scalable> zonalScalables = cimGlskDocument.getZonalScalable(network, instant);
        assertEquals(2, zonalScalables.getDataPerZone().size());
        Scalable scalableNL = zonalScalables.getData("10YNL----------L"); //type B45 curve type A03
        assertEquals(3, scalableNL.filterInjections(network).size());
        Scalable scalableBE = zonalScalables.getData("10YBE----------2"); //type B45 curve type A01
        assertEquals(4, scalableBE.filterInjections(network).size());
        assertEquals(192., scalableBE.scale(network, 192), 0.001);
        assertEquals(-450., scalableBE.scale(network, -500), 0.001);

    }
}

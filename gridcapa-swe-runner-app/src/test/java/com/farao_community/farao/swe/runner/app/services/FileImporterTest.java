/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.cim.CimCrac;
import com.farao_community.farao.swe.runner.api.resource.SweFileResource;
import com.farao_community.farao.swe.runner.api.resource.SweRequest;
import com.google.common.base.Suppliers;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.import_.ImportConfig;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class FileImporterTest {

    @Autowired
    private FileImporter fileImporter;
    private final String testDirectory = "/20210401/";
    private final String cimCracFilename = "CIM_21_1_1.xml";
    private final String jsonCracFilename = "cracCimJson.json";
    private final String networkFileName = "MicroGrid.zip";
    private final OffsetDateTime dateTime = OffsetDateTime.parse("2021-04-01T22:00Z");

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
        Network network = Importers.loadNetwork(
                Paths.get(new File(getClass().getResource(testDirectory + networkFileName).getFile()).toString()),
                LocalComputationManager.getDefault(),
                Suppliers.memoize(ImportConfig::load).get(),
                importParams
        );
        SweRequest sweRequest = createEmptySweRequest();
        SweRequest req = createEmptySweRequest();
        Crac cracFrEs = fileImporter.importCracFromCimCracAndNetwork(fileImporter.importCimCrac(req), dateTime, network, null);
        Assertions.assertNotNull(cracFrEs);
        Crac cracEsPt = fileImporter.importCracFromCimCracAndNetwork(fileImporter.importCimCrac(req), dateTime, network, SweRunner.CRAC_CIM_CRAC_CREATION_PARAMETERS_PT_ES_JSON);
        Assertions.assertNotNull(cracEsPt);
    }

    @Test
    void testImportCracFromJson() {
        Crac cracFromJson = fileImporter.importCracFromJson(Objects.requireNonNull(getClass().getResource(testDirectory + jsonCracFilename)).toString());
        assertNotNull(cracFromJson);
    }

    SweRequest createEmptySweRequest() {
        return new SweRequest("id", dateTime, null, null, null, null, null, null, null, null, null, null,
                new SweFileResource("cracfile", getClass().getResource(testDirectory + cimCracFilename).toExternalForm()), null, null);
    }
}

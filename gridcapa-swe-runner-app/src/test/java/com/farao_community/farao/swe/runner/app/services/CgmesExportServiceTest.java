/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.swe.runner.api.resource.ProcessType;
import com.farao_community.farao.swe.runner.app.dichotomy.DichotomyDirection;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.google.common.base.Suppliers;
import com.powsybl.cgmes.conversion.CgmesImport;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.mergingview.MergingView;
import com.powsybl.iidm.network.ImportConfig;
import com.powsybl.iidm.network.Network;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@SpringBootTest
class CgmesExportServiceTest {

    @Autowired
    private CgmesExportService cgmesExportService;

    @Test
    void testBuildCgmesFilename() {
        SweData sweData = mock(SweData.class);
        when(sweData.getTimestamp()).thenReturn(OffsetDateTime.ofInstant(Instant.parse("2022-11-30T00:00:00Z"), ZoneId.of("UTC")));
        when(sweData.getProcessType()).thenReturn(ProcessType.D2CC);
        String result = cgmesExportService.buildCgmesFilename(sweData, "FR", "ESFR");
        assertEquals("20221130T0000Z_2D_FR_ESFR_001.xml", result);
    }

    @Test
    void testCreateFileType() {
        DichotomyDirection directionEsFr = DichotomyDirection.ES_FR;
        String resultEsFr = cgmesExportService.createFileType(directionEsFr);
        assertEquals("CGM_ESFR", resultEsFr);

        DichotomyDirection directionFrEs = DichotomyDirection.FR_ES;
        String resultFrEs = cgmesExportService.createFileType(directionFrEs);
        assertEquals("CGM_FRES", resultFrEs);

        DichotomyDirection directionEsPt = DichotomyDirection.ES_PT;
        String resultEsPt = cgmesExportService.createFileType(directionEsPt);
        assertEquals("CGM_ESPT", resultEsPt);

        DichotomyDirection directionPtEs = DichotomyDirection.PT_ES;
        String resultPtEs = cgmesExportService.createFileType(directionPtEs);
        assertEquals("CGM_PTES", resultPtEs);
    }

    @Test
    void testApplyingShiftToCgm() throws URISyntaxException {
        Network inputNetwork = importFromZip(Paths.get(Objects.requireNonNull(getClass().getResource("/export_cgmes/MicroGrid.zip")).toURI()).toString());
        assertEquals(140, inputNetwork.getGenerator("_2844585c-0d35-488d-a449-685bcd57afbf").getTargetP());
        assertEquals(90., inputNetwork.getLoad("_69add5b4-70bd-4360-8a93-286256c0d38b").getP0());
        MergingView mergingView = MergingView.create("imported_network", "iidm");
        mergingView.merge(inputNetwork);
        mergingView.setCaseDate(DateTime.parse("2030-01-25T19:00:00Z"));
        String networkWithPraUrl = getClass().getResource("/export_cgmes/microGrid.xiidm").toString();
        cgmesExportService.applyNetworkWithPraResultToMergingView(networkWithPraUrl, mergingView);
        assertEquals(200, inputNetwork.getGenerator("_2844585c-0d35-488d-a449-685bcd57afbf").getTargetP());
        assertEquals(50., mergingView.getLoad("_69add5b4-70bd-4360-8a93-286256c0d38b").getP0());
    }

    private Network importFromZip(String zipPath) {
        Properties importParams = new Properties();
        importParams.put(CgmesImport.SOURCE_FOR_IIDM_ID, CgmesImport.SOURCE_FOR_IIDM_ID_RDFID);
        return Network.read(Paths.get(zipPath), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);
    }

}

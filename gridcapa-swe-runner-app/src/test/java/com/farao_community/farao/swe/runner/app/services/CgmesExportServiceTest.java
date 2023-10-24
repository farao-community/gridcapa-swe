/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.swe.runner.api.resource.ProcessType;
import com.farao_community.farao.swe.runner.app.dichotomy.DichotomyDirection;
import com.farao_community.farao.swe.runner.app.domain.CgmesFileType;
import com.farao_community.farao.swe.runner.app.domain.MergedNetworkData;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.powsybl.iidm.network.Network;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    void buildCgmesFilenameTest() {
        SoftAssertions assertions = new SoftAssertions();
        SweData sweData = mock(SweData.class);
        when(sweData.getTimestamp()).thenReturn(OffsetDateTime.ofInstant(Instant.parse("2022-11-30T00:00:00Z"), ZoneId.of("UTC")));

        when(sweData.getProcessType()).thenReturn(ProcessType.D2CC);
        String d2ccResult = cgmesExportService.buildCgmesFilename(sweData, "FR", "ESFR");
        assertions.assertThat(d2ccResult).isEqualTo("20221130T0000Z_2D_FR_ESFR_001.xml");

        when(sweData.getProcessType()).thenReturn(ProcessType.IDCC);
        String idccResult = cgmesExportService.buildCgmesFilename(sweData, "FR", "ESFR");
        assertions.assertThat(idccResult).isEqualTo("20221130T0000Z_ID_FR_ESFR_001.xml");

        when(sweData.getProcessType()).thenReturn(ProcessType.IDCC_IDCF);
        String idccIdcfResult = cgmesExportService.buildCgmesFilename(sweData, "FR", "ESFR");
        assertions.assertThat(idccIdcfResult).isEqualTo("20221130T0000Z_IDCF_FR_ESFR_001.xml");

        assertions.assertAll();
    }

    @Test
    void buildFileTypeTest() {
        assertEquals("CGM_ESFR", cgmesExportService.buildFileType(DichotomyDirection.ES_FR));
        assertEquals("CGM_FRES", cgmesExportService.buildFileType(DichotomyDirection.FR_ES));
        assertEquals("CGM_ESPT", cgmesExportService.buildFileType(DichotomyDirection.ES_PT));
        assertEquals("CGM_PTES", cgmesExportService.buildFileType(DichotomyDirection.PT_ES));
    }

    @Test
    void exportCgmesSshTest() throws IOException {
        MergedNetworkData mergedNetworkData = createMergedNetworkData();
        SweData sweData = new SweData("id", OffsetDateTime.parse("2023-07-31T00:30:00Z"), ProcessType.D2CC, null, null, null, null, mergedNetworkData, null, null, "glskUrl", "CracEsPt", "CracFrEs", "raoParametersEsFrUrl", "raoParametersEsPtUrl", new EnumMap<>(CgmesFileType.class));
        Map<String, ByteArrayOutputStream> sshFiles = cgmesExportService.createAllSshFiles(mergedNetworkData.getMergedNetwork(), sweData);
        assertEquals(3, sshFiles.size());
        assertTrue(sshFiles.containsKey("20230731T0030Z_2D_REE_SSH_001.xml"));
    }

    @Test
    void exportCgmesSvTest() throws IOException {
        MergedNetworkData mergedNetworkData = createMergedNetworkData();
        SweData sweData = new SweData("id", OffsetDateTime.parse("2023-07-31T00:30:00Z"), ProcessType.D2CC, null, null, null, null, mergedNetworkData, null, null, "glskUrl", "CracEsPt", "CracFrEs", "raoParametersEsFrUrl", "raoParametersEsPtUrl", new EnumMap<>(CgmesFileType.class));
        Map<String, ByteArrayOutputStream> file = cgmesExportService.createCommonFile(mergedNetworkData.getMergedNetwork(), sweData);
        assertEquals(1, file.size());
        assertTrue(file.containsKey("20230731T0030Z_2D_CGMSWE_SV_001.xml"));
    }

    private MergedNetworkData createMergedNetworkData() {
        String networkFileName = "/export_cgmes/TestCase_with_swe_countries.xiidm";
        Network network = Network.read(networkFileName, getClass().getResourceAsStream(networkFileName));
        Map<String, String> subNetworkIds = new HashMap<>();
        subNetworkIds.put("ES", "urn:uuid:563eadb1-4dfa-9784-a7ad-c8eddaaf3103");
        subNetworkIds.put("FR", "urn:uuid:6cde6aab-942e-4af6-b087-c559bf0c67b4");
        subNetworkIds.put("PT", "urn:uuid:26ac088c-2e06-11ee-816e-00155d38aa10");
        return new MergedNetworkData(network, subNetworkIds);
    }
}

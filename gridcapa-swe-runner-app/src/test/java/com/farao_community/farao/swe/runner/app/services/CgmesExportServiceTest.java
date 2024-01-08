/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import com.farao_community.farao.gridcapa_swe_commons.resource.ProcessType;
import com.farao_community.farao.swe.runner.api.resource.SweFileResource;
import com.farao_community.farao.swe.runner.app.domain.CgmesFileType;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.powsybl.iidm.network.Network;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void exportCgmesFilesTest() throws IOException, XMLStreamException {
        String networkFileName = "/export_cgmes/TestCase_with_swe_countries.xiidm";
        Network network = Network.read(networkFileName, getClass().getResourceAsStream(networkFileName));
        String  emptyXmlFile = "/export_cgmes/emptyXmlFile.xml";
        Map<CgmesFileType, SweFileResource> cgmesInputFiles = new EnumMap<>(CgmesFileType.class);
        SweFileResource fileReeEq = new SweFileResource("test.xml", getClass().getResource(emptyXmlFile).toExternalForm());
        cgmesInputFiles.put(CgmesFileType.REE_EQ, fileReeEq);
        cgmesInputFiles.put(CgmesFileType.REE_TP, fileReeEq);
        cgmesInputFiles.put(CgmesFileType.REN_EQ, fileReeEq);
        cgmesInputFiles.put(CgmesFileType.REN_TP, fileReeEq);
        cgmesInputFiles.put(CgmesFileType.RTE_EQ, fileReeEq);
        cgmesInputFiles.put(CgmesFileType.RTE_TP, fileReeEq);
        SweData sweData = new SweData("id", OffsetDateTime.parse("2023-07-31T00:30:00Z"), ProcessType.D2CC, null, null, null, null, null, null, "glskUrl", "CracEsPt", "CracFrEs", "raoParametersEsFrUrl", "raoParametersEsPtUrl", cgmesInputFiles);
        Map<String, ByteArrayOutputStream> cgmesFiles = cgmesExportService.generateCgmesFile(network, sweData);
        assertEquals(10, cgmesFiles.size());
        assertTrue(cgmesFiles.containsKey("20230731T0030Z_2D_REE_SSH_001.xml"));
        assertTrue(cgmesFiles.containsKey("20230731T0030Z_2D_REE_EQ_001.xml"));
        assertTrue(cgmesFiles.containsKey("20230731T0030Z_2D_REE_TP_001.xml"));
        assertTrue(cgmesFiles.containsKey("20230731T0030Z_2D_REN_SSH_001.xml"));
        assertTrue(cgmesFiles.containsKey("20230731T0030Z_2D_REN_EQ_001.xml"));
        assertTrue(cgmesFiles.containsKey("20230731T0030Z_2D_REN_TP_001.xml"));
        assertTrue(cgmesFiles.containsKey("20230731T0030Z_2D_RTEFRANCE_SSH_001.xml"));
        assertTrue(cgmesFiles.containsKey("20230731T0030Z_2D_RTEFRANCE_EQ_001.xml"));
        assertTrue(cgmesFiles.containsKey("20230731T0030Z_2D_RTEFRANCE_TP_001.xml"));
        assertTrue(cgmesFiles.containsKey("20230731T0030Z_2D_CGMSWE_SV_001.xml"));
    }

    @Test
    void exportCgmesSshTest() throws IOException, XMLStreamException {
        String networkFileName = "/export_cgmes/TestCase_with_swe_countries.xiidm";
        Network network = Network.read(networkFileName, getClass().getResourceAsStream(networkFileName));
        SweData sweData = new SweData("id", OffsetDateTime.parse("2023-07-31T00:30:00Z"), ProcessType.D2CC, null, null, null, null, null, null, "glskUrl", "CracEsPt", "CracFrEs", "raoParametersEsFrUrl", "raoParametersEsPtUrl", new EnumMap<>(CgmesFileType.class));
        Map<String, ByteArrayOutputStream> sshFiles = cgmesExportService.createAllSshFiles(network, sweData);
        assertEquals(3, sshFiles.size());
        assertTrue(sshFiles.containsKey("20230731T0030Z_2D_REE_SSH_001.xml"));
        assertTrue(sshFiles.containsKey("20230731T0030Z_2D_REN_SSH_001.xml"));
        assertTrue(sshFiles.containsKey("20230731T0030Z_2D_RTEFRANCE_SSH_001.xml"));
    }

    @Test
    void exportCgmesFilesWithMissingCountryTest() throws IOException, XMLStreamException {
        //In cas of subnetwork contains many countries it will not be exported
        String networkFileName = "/export_cgmes/TestCase_with_swe_countries_error.xiidm";
        Network network = Network.read(networkFileName, getClass().getResourceAsStream(networkFileName));
        SweData sweData = new SweData("id", OffsetDateTime.parse("2023-07-31T00:30:00Z"), ProcessType.D2CC, null, null, null, null, null, null, "glskUrl", "CracEsPt", "CracFrEs", "raoParametersEsFrUrl", "raoParametersEsPtUrl", new EnumMap<>(CgmesFileType.class));
        Map<String, ByteArrayOutputStream> sshFiles = cgmesExportService.createAllSshFiles(network, sweData);
        assertEquals(2, sshFiles.size());
        assertFalse(sshFiles.containsKey("20230731T0030Z_2D_REE_SSH_001.xml"));
        assertTrue(sshFiles.containsKey("20230731T0030Z_2D_RTEFRANCE_SSH_001.xml"));
    }

}

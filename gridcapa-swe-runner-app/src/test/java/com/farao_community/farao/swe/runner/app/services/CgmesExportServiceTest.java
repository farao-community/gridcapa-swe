/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
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
import com.google.common.base.Suppliers;
import com.powsybl.cgmes.conversion.CgmesImport;
import com.powsybl.cgmes.extensions.CgmesMetadataModels;
import com.powsybl.cgmes.extensions.CgmesMetadataModelsAdder;
import com.powsybl.cgmes.model.CgmesMetadataModel;
import com.powsybl.cgmes.model.CgmesSubset;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.ImportConfig;
import com.powsybl.iidm.network.Network;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        String d2ccResult = cgmesExportService.buildCgmesFilename(sweData, "FR", "ESFR", "001");
        assertions.assertThat(d2ccResult).isEqualTo("20221130T0000Z_2D_FR_ESFR_001");

        when(sweData.getProcessType()).thenReturn(ProcessType.IDCC);
        String idccResult = cgmesExportService.buildCgmesFilename(sweData, "FR", "ESFR", "002");
        assertions.assertThat(idccResult).isEqualTo("20221130T0000Z_1D_FR_ESFR_002");

        when(sweData.getProcessType()).thenReturn(ProcessType.IDCC_IDCF);
        String idccIdcfResult = cgmesExportService.buildCgmesFilename(sweData, "FR", "ESFR", "003");
        assertions.assertThat(idccIdcfResult).isEqualTo("20221130T0000Z_IDCF_FR_ESFR_003");

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
    void exportCgmesFilesTest() throws IOException {
        String networkFileName = "/export_cgmes/TestCase_with_swe_countries.xiidm";
        Network network = Network.read(networkFileName, getClass().getResourceAsStream(networkFileName));
        network.getSubnetwork("urn:uuid:563eadb1-4dfa-9784-a7ad-c8eddaaf3103") //subnetwork of REE
                .newExtension(CgmesMetadataModelsAdder.class)
                .newModel()
                .setSubset(CgmesSubset.STEADY_STATE_HYPOTHESIS)
                .setId("ssh-id-test1")
                .setVersion(5)
                .addProfile("fakeProfile")
                .setModelingAuthoritySet("fakeAuthority")
                .add()
                .newModel()
                .setSubset(CgmesSubset.STATE_VARIABLES)
                .setId("sv-id-test1")
                .setVersion(5)
                .addDependentOn("tp-id-test1")
                .addDependentOn("ssh-id-test1")
                .addProfile("fakeProfile")
                .setModelingAuthoritySet("fakeAuthority")
                .add()
                .add();
        Map<CgmesFileType, SweFileResource> cgmesInputFiles = new EnumMap<>(CgmesFileType.class);

        cgmesInputFiles.put(CgmesFileType.REE_EQ, new SweFileResource("REE_EQ.xml", Objects.requireNonNull(getClass().getResource("/network/MicroGrid_SWE/network_ES_EQ.xml")).toExternalForm()));
        cgmesInputFiles.put(CgmesFileType.REE_TP, new SweFileResource("REE_TP.xml", Objects.requireNonNull(getClass().getResource("/network/MicroGrid_SWE/network_ES_TP.xml")).toExternalForm()));
        cgmesInputFiles.put(CgmesFileType.REN_EQ, new SweFileResource("REN_EQ.xml", Objects.requireNonNull(getClass().getResource("/network/MicroGrid_SWE/network_PT_EQ.xml")).toExternalForm()));
        cgmesInputFiles.put(CgmesFileType.REN_TP, new SweFileResource("REN_TP.xml", Objects.requireNonNull(getClass().getResource("/network/MicroGrid_SWE/network_PT_TP.xml")).toExternalForm()));
        cgmesInputFiles.put(CgmesFileType.RTE_EQ,  new SweFileResource("RTE_EQ.xml", Objects.requireNonNull(getClass().getResource("/network/MicroGrid_SWE/network_FR_EQ.xml")).toExternalForm()));
        cgmesInputFiles.put(CgmesFileType.RTE_TP, new SweFileResource("RTE_TP.xml", Objects.requireNonNull(getClass().getResource("/network/MicroGrid_SWE/network_FR_TP.xml")).toExternalForm()));
        SweData sweData = new SweData("id", OffsetDateTime.parse("2023-07-31T00:30:00Z"), ProcessType.D2CC, null, null, null, null, null, null, "glskUrl", "CracEsPt", "CracFrEs", "raoParametersEsFrUrl", "raoParametersEsPtUrl", cgmesInputFiles);
        Map<String, ByteArrayOutputStream> cgmesFiles = cgmesExportService.generateCgmesFile(network, sweData);
        assertEquals(10, cgmesFiles.size());
        assertTrue(cgmesFiles.containsKey("20230731T0030Z_2D_REE_SSH_006"));
        assertTrue(cgmesFiles.containsKey("20230731T0030Z_2D_REE_EQ_001"));
        assertTrue(cgmesFiles.containsKey("20230731T0030Z_2D_REE_TP_001"));
        assertTrue(cgmesFiles.containsKey("20230731T0030Z_2D_REN_SSH_001"));
        assertTrue(cgmesFiles.containsKey("20230731T0030Z_2D_REN_EQ_001"));
        assertTrue(cgmesFiles.containsKey("20230731T0030Z_2D_REN_TP_001"));
        assertTrue(cgmesFiles.containsKey("20230731T0030Z_2D_RTEFRANCE_SSH_001"));
        assertTrue(cgmesFiles.containsKey("20230731T0030Z_2D_RTEFRANCE_EQ_001"));
        assertTrue(cgmesFiles.containsKey("20230731T0030Z_2D_RTEFRANCE_TP_001"));
        assertTrue(cgmesFiles.containsKey("20230731T0030Z_2D_CGMSWE_SV_001"));

        String tmp = Files.createTempDirectory("pref_").toAbsolutePath() + "/network_output.zip";
        exportCgmesZipFile(cgmesFiles, tmp);

        // Checking that extension was added to network
        CgmesMetadataModels modelsExtension = network.getExtension(CgmesMetadataModels.class);
        assertNotNull(modelsExtension);
        CgmesMetadataModel svModel = modelsExtension.getModelForSubset(CgmesSubset.STATE_VARIABLES).get();
        assertEquals("sv-id-test1", svModel.getId());
        assertFalse(svModel.getDependentOn().contains("ssh-id-test1")); //initial ssh id removed from sv dependencies
        assertTrue(svModel.getDependentOn().contains("tp-id-test1")); // Initial tp id is kept
        assertEquals(4, svModel.getDependentOn().size()); // Initial tp id + 3 new ssh ids generated during export

        // Checking that the extension was correctly exported in the SV
        Properties importParams = new Properties();
        importParams.put(CgmesImport.SOURCE_FOR_IIDM_ID, CgmesImport.SOURCE_FOR_IIDM_ID_RDFID);
        Network outputNetwork = Network.read(Paths.get(tmp), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);
        assertNotNull(outputNetwork);
        Network subnetwork = (Network) outputNetwork.getSubnetworks().toArray()[0];
        CgmesMetadataModels modelsExtensionOutput = subnetwork.getExtension(CgmesMetadataModels.class);
        assertNotNull(modelsExtension);
        CgmesMetadataModel svModelOutput = modelsExtensionOutput.getModelForSubset(CgmesSubset.STATE_VARIABLES).get();
        assertTrue(svModelOutput.getSupersedes().toArray()[0].toString().contains("sv-id-test1"));
        assertEquals(4, svModelOutput.getDependentOn().size()); // Initial tp id + 3 new ssh ids generated during export

        Files.deleteIfExists(Paths.get(tmp));
    }

    @Test
    void exportCgmesSshTest() throws IOException {
        String networkFileName = "/export_cgmes/TestCase_with_swe_countries.xiidm";
        Network network = Network.read(networkFileName, getClass().getResourceAsStream(networkFileName));
        network.getSubnetwork("urn:uuid:563eadb1-4dfa-9784-a7ad-c8eddaaf3103") //subnetwork of REE
                .newExtension(CgmesMetadataModelsAdder.class)
                .newModel()
                .setSubset(CgmesSubset.STEADY_STATE_HYPOTHESIS)
                .setId("sshId")
                .setVersion(5)
                .addProfile("fakeProfile")
                .setModelingAuthoritySet("fakeAuthority")
                .add()
                .add();
        SweData sweData = new SweData("id", OffsetDateTime.parse("2023-07-31T00:30:00Z"), ProcessType.D2CC, null, null, null, null, null, null, "glskUrl", "CracEsPt", "CracFrEs", "raoParametersEsFrUrl", "raoParametersEsPtUrl", new EnumMap<>(CgmesFileType.class));
        Map<String, ByteArrayOutputStream> sshFiles = cgmesExportService.createAllSshFiles(network, sweData, new ArrayList<>());
        assertEquals(3, sshFiles.size());
        assertTrue(sshFiles.containsKey("20230731T0030Z_2D_REE_SSH_006"));
        assertTrue(sshFiles.containsKey("20230731T0030Z_2D_REN_SSH_001"));
        assertTrue(sshFiles.containsKey("20230731T0030Z_2D_RTEFRANCE_SSH_001"));
    }

    @Test
    void exportCgmesFilesWithMissingCountryTest() throws IOException {
        //In cas of subnetwork contains many countries it will not be exported
        String networkFileName = "/export_cgmes/TestCase_with_swe_countries_error.xiidm";
        Network network = Network.read(networkFileName, getClass().getResourceAsStream(networkFileName));
        SweData sweData = new SweData("id", OffsetDateTime.parse("2023-07-31T00:30:00Z"), ProcessType.D2CC, null, null, null, null, null, null, "glskUrl", "CracEsPt", "CracFrEs", "raoParametersEsFrUrl", "raoParametersEsPtUrl", new EnumMap<>(CgmesFileType.class));
        Map<String, ByteArrayOutputStream> sshFiles = cgmesExportService.createAllSshFiles(network, sweData, new ArrayList<>());
        assertEquals(2, sshFiles.size());
        assertFalse(sshFiles.containsKey("20230731T0030Z_2D_REE_SSH_001"));
        assertTrue(sshFiles.containsKey("20230731T0030Z_2D_RTEFRANCE_SSH_001"));
    }

    private void exportCgmesZipFile(Map<String, ByteArrayOutputStream> mapCgmesFiles, String path) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zipOs = new ZipOutputStream(baos)) {

            for (var entry : mapCgmesFiles.entrySet()) {
                zipOs.putNextEntry(new ZipEntry(entry.getKey() + ".xml"));
                byte[] bytes = new byte[1024];
                int length;
                InputStream is = new ByteArrayInputStream(entry.getValue().toByteArray());
                while ((length = is.read(bytes)) >= 0) {
                    zipOs.write(bytes, 0, length);
                }
                is.close();
            }
            zipOs.close();
            baos.close();

            Files.write(Path.of(path), baos.toByteArray());
        }
    }
}

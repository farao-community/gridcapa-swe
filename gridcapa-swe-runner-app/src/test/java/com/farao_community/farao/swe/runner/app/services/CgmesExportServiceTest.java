/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.dichotomy.api.results.DichotomyStepResult;
import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import com.farao_community.farao.gridcapa_swe_commons.resource.ProcessType;
import com.farao_community.farao.rao_runner.api.resource.RaoSuccessResponse;
import com.farao_community.farao.swe.runner.api.resource.SweFileResource;
import com.farao_community.farao.swe.runner.api.resource.SweRequest;
import com.farao_community.farao.swe.runner.app.domain.CgmesFileType;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyValidationData;
import com.farao_community.farao.swe.runner.app.domain.SweTaskParameters;
import com.farao_community.farao.swe.runner.app.utils.UrlValidationService;
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
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.LoggerFactory;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@SpringBootTest
class CgmesExportServiceTest {

    private static Path temporaryBoundariesPath;

    @Autowired
    private CgmesExportService cgmesExportService;

    @Mock
    private UrlValidationService urlValidationService;

    @Mock
    SweData sweData;

    @Mock
    Path temporaryBoundariesDirectory;

    @Mock
    List<SweFileResource> boundariesFiles;

    @BeforeEach
    void setUp() {
        temporaryBoundariesPath = FilesService.createTemporaryBoundariesDirectory(sweData.getBoundariesFiles());
    }

    @Test
    void buildCgmesFilenameTest() {
        SoftAssertions assertions = new SoftAssertions();
        when(sweData.getTimestamp()).thenReturn(OffsetDateTime.ofInstant(Instant.parse("2022-11-30T00:00:00Z"), ZoneId.of("UTC")));

        when(sweData.getProcessType()).thenReturn(ProcessType.D2CC);
        final String d2ccResult = cgmesExportService.buildCgmesFilename(sweData, "FR", "ESFR", "001");
        assertions.assertThat(d2ccResult).isEqualTo("20221130T0000Z_2D_FR_ESFR_001");

        when(sweData.getProcessType()).thenReturn(ProcessType.IDCC);
        final String idccResult = cgmesExportService.buildCgmesFilename(sweData, "FR", "ESFR", "002");
        assertions.assertThat(idccResult).isEqualTo("20221130T0000Z_1D_FR_ESFR_002");

        assertions.assertAll();
    }

    @Test
    void buildCgmesFilenameTestIDCF() {
        OffsetDateTime mockTimestamp = OffsetDateTime.now().plusHours(5).plusSeconds(1);
        when(sweData.getTimestamp()).thenReturn(mockTimestamp);
        when(sweData.getProcessType()).thenReturn(ProcessType.IDCC_IDCF);
        final String tso = "fakeTso";
        final String type = "fakeType";
        final String version = "fakeExample";

        // Expected time when difference is between 0 and 23 : +5 hours
        String expectedTime = "_05_";
        String expectedFilename = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm'Z'").format(mockTimestamp) + expectedTime + "fakeTso_fakeType_fakeExample";
        String actualFilename = cgmesExportService.buildCgmesFilename(sweData, tso, type, version);
        assertEquals(expectedFilename, actualFilename);

        // Test that the min value is 0 when value is <0
        mockTimestamp = OffsetDateTime.now().minusHours(30);
        when(sweData.getTimestamp()).thenReturn(mockTimestamp);
        expectedTime = "_00_";
        expectedFilename = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm'Z'").format(mockTimestamp) + expectedTime + "fakeTso_fakeType_fakeExample";
        actualFilename = cgmesExportService.buildCgmesFilename(sweData, tso, type, version);
        assertEquals(expectedFilename, actualFilename);

        // Test that the max value is 23 when value is >23
        mockTimestamp = OffsetDateTime.now().plusHours(30);
        when(sweData.getTimestamp()).thenReturn(mockTimestamp);
        expectedTime = "_23_";
        expectedFilename = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm'Z'").format(mockTimestamp) + expectedTime + "fakeTso_fakeType_fakeExample";
        actualFilename = cgmesExportService.buildCgmesFilename(sweData, tso, type, version);
        assertEquals(expectedFilename, actualFilename);
    }

    @Test
    void buildFileTypeTest() {
        assertEquals("CGM_ESFR_LAST_SECURE", cgmesExportService.buildFileType(DichotomyDirection.ES_FR, true));
        assertEquals("CGM_FRES_LAST_SECURE", cgmesExportService.buildFileType(DichotomyDirection.FR_ES, true));
        assertEquals("CGM_ESPT_LAST_SECURE", cgmesExportService.buildFileType(DichotomyDirection.ES_PT, true));
        assertEquals("CGM_PTES_LAST_SECURE", cgmesExportService.buildFileType(DichotomyDirection.PT_ES, true));
        assertEquals("CGM_ESFR_FIRST_UNSECURE", cgmesExportService.buildFileType(DichotomyDirection.ES_FR, false));
        assertEquals("CGM_FRES_FIRST_UNSECURE", cgmesExportService.buildFileType(DichotomyDirection.FR_ES, false));
        assertEquals("CGM_ESPT_FIRST_UNSECURE", cgmesExportService.buildFileType(DichotomyDirection.ES_PT, false));
        assertEquals("CGM_PTES_FIRST_UNSECURE", cgmesExportService.buildFileType(DichotomyDirection.PT_ES, false));
    }

    @Test
    void buildLastSecureCgmesNoValidStep() {
        final DichotomyResult<SweDichotomyValidationData> dichotomyResult = DichotomyResult.buildFromRaoFailure("test");
        final String result = cgmesExportService.buildAndExportLastSecureCgmesFiles(DichotomyDirection.ES_FR,
                                                                                    sweData,
                                                                                    temporaryBoundariesDirectory,
                                                                                    dichotomyResult,
                                                                                    mock(SweTaskParameters.class));
        assertNull(result);
    }

    @Test
    void buildLastSecureCgmesValidStep() {
        final CgmesExportService spyCgmesExportService = spy(cgmesExportService);
        doReturn("Test").when(spyCgmesExportService).buildAndExportCgmesFiles(any(), any(), any(), any(), any(), eq(true));
        final DichotomyResult<SweDichotomyValidationData> dichotomyResult = mock(DichotomyResult.class);
        when(dichotomyResult.hasValidStep()).thenReturn(true);
        final String result = spyCgmesExportService.buildAndExportLastSecureCgmesFiles(DichotomyDirection.ES_FR,
                                                                                       sweData,
                                                                                       temporaryBoundariesDirectory,
                                                                                       dichotomyResult,
                                                                                       mock(SweTaskParameters.class));
        assertEquals("Test", result);
    }

    @Test
    void buildCgmesFilesTempBoundaryFileTest() {

        final Logger logger = (Logger) LoggerFactory.getLogger(CgmesExportService.class);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        final DichotomyStepResult<SweDichotomyValidationData> mockStepResult = mock(DichotomyStepResult.class);
        when(mockStepResult.getValidationData()).thenReturn(mock(SweDichotomyValidationData.class));
        when(mockStepResult.getValidationData().getRaoResponse()).thenReturn(mock(RaoSuccessResponse.class));

        when(urlValidationService.openUrlStream(anyString()))
                .thenThrow(new RuntimeException("stop here"));
        assertThrows(RuntimeException.class,
                     () -> cgmesExportService.buildAndExportCgmesFiles(DichotomyDirection.ES_FR,
                                                                       sweData,
                                                                       temporaryBoundariesPath,
                                                                       mock(SweTaskParameters.class),
                                                                       mockStepResult,
                                                                       true));
        assertThat(appender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .contains("BOUNDARY_LOCATION: " + temporaryBoundariesPath);

    }

    @Test
    void buildFirstUnsecureCgmesNoInvalidStep() {
        final DichotomyResult<SweDichotomyValidationData> dichotomyResult = mock(DichotomyResult.class);
        when(dichotomyResult.getLowestInvalidStep()).thenReturn(null);
        final String result = cgmesExportService.buildAndExportFirstUnsecureCgmesFiles(DichotomyDirection.ES_FR,
                                                                                       sweData,
                                                                                       temporaryBoundariesDirectory,
                                                                                       dichotomyResult,
                                                                                       mock(SweTaskParameters.class));
        assertNull(result);
    }

    @Test
    void buildFirstUnsecureCgmesInvalidStep() {
        final CgmesExportService spyCgmesExportService = spy(cgmesExportService);
        doReturn("Test").when(spyCgmesExportService).buildAndExportCgmesFiles(any(), any(), any(), any(), any(), eq(false));
        final DichotomyResult<SweDichotomyValidationData> dichotomyResult = mock(DichotomyResult.class);
        final DichotomyStepResult<SweDichotomyValidationData> mockStepResult = mock(DichotomyStepResult.class);
        when(dichotomyResult.getLowestInvalidStep()).thenReturn(mockStepResult);
        when(mockStepResult.getValidationData()).thenReturn(mock(SweDichotomyValidationData.class));
        final String result = spyCgmesExportService.buildAndExportFirstUnsecureCgmesFiles(DichotomyDirection.ES_FR,
                                                                                          sweData,
                                                                                          temporaryBoundariesDirectory,
                                                                                          dichotomyResult,
                                                                                          mock(SweTaskParameters.class));
        assertEquals("Test", result);
    }

    @Test
    void exportCgmesFilesTest() throws IOException {
        final String networkFileName = "/export_cgmes/TestCase_with_swe_countries.xiidm";
        final String subNetwork = "urn:uuid:563eadb1-4dfa-9784-a7ad-c8eddaaf3103"; //subnetwork of REE
        Network network = setTestNetwork(networkFileName, subNetwork);
        Map<CgmesFileType, SweFileResource> cgmesInputFiles = getCgmesInputFiles();
        final SweData sweData = createSweData(cgmesInputFiles, boundariesFiles);

        final Map<String, ByteArrayOutputStream> cgmesFiles = cgmesExportService.generateCgmesFiles(network, sweData, temporaryBoundariesPath);

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

        final String tmp = Files.createTempDirectory("pref_").toAbsolutePath() + "/network_output.zip";
        final Path filePath = Paths.get(tmp);
        exportCgmesZipFile(cgmesFiles, tmp);

        // Checking that extension was added to network
        final CgmesMetadataModels modelsExtension = network.getExtension(CgmesMetadataModels.class);
        assertNotNull(modelsExtension);
        final CgmesMetadataModel svModel = modelsExtension.getModelForSubset(CgmesSubset.STATE_VARIABLES).get();
        assertEquals("sv-id-test1", svModel.getSupersedes().toArray()[0]);
        assertFalse(svModel.getDependentOn().contains("ssh-id-test1")); //initial ssh id removed from sv dependencies
        assertTrue(svModel.getDependentOn().contains("tp-id-test1")); // Initial tp id is kept
        assertEquals(4, svModel.getDependentOn().size()); // Initial tp id + 3 new ssh ids generated during export

        // Checking that the extension was correctly exported in the SV
        Properties importParams = new Properties();
        importParams.put(CgmesImport.SOURCE_FOR_IIDM_ID, CgmesImport.SOURCE_FOR_IIDM_ID_RDFID);
        final Network outputNetwork = Network.read(filePath, LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);
        assertNotNull(outputNetwork);
        final Network subnetwork = (Network) outputNetwork.getSubnetworks().toArray()[0];
        final CgmesMetadataModels modelsExtensionOutput = subnetwork.getExtension(CgmesMetadataModels.class);
        assertNotNull(modelsExtensionOutput);
        final CgmesMetadataModel svModelOutput = modelsExtensionOutput.getModelForSubset(CgmesSubset.STATE_VARIABLES).get();
        assertEquals("http://entsoe.eu/CIM/StateVariables/4/1", svModelOutput.getProfiles().stream().findFirst().get());
        assertEquals(1, svModelOutput.getVersion());
        assertEquals("http://www.coreso.eu/OperationalPlanning", svModelOutput.getModelingAuthoritySet());
        assertEquals(4, svModelOutput.getDependentOn().size()); // Initial tp id + 3 new ssh ids generated during export

        Files.deleteIfExists(filePath);
    }

    @Test
    void retrievingTpFilesTest() throws IOException {
        final Logger logger = (Logger) LoggerFactory.getLogger(CgmesExportService.class);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        // Test Node Breaker Topology for REN only - TP files are retrieved for only RTE and REE
        String networkFileName = "/export_cgmes/TestCase_with_swe_countries_node_breaker_PT.xiidm";
        final String subNetwork = "urn:uuid:563eadb1-4dfa-9784-a7ad-c8eddaaf3103"; //subnetwork of REE
        Network network = setTestNetwork(networkFileName, subNetwork);
        Map<CgmesFileType, SweFileResource> cgmesInputFiles = getCgmesInputFiles();
        final SweData sweData = createSweData(cgmesInputFiles, boundariesFiles);
        cgmesExportService.generateCgmesFiles(network, sweData, temporaryBoundariesPath);

        List<String> matchingLogsNodeBreaker = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .filter(msg -> msg.contains("Retrieving TP file"))
                .toList();
        assertThat(matchingLogsNodeBreaker).hasSize(2);

        appender.list.clear();

        // Test Bus Breaker Topology for all TSO - TP files are retrieved for the 3 TSO
        networkFileName = "/export_cgmes/TestCase_with_swe_countries.xiidm";
        network = setTestNetwork(networkFileName, subNetwork);
        cgmesExportService.generateCgmesFiles(network, sweData, temporaryBoundariesPath);

        List<String> matchingLogsBusBreaker = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .filter(msg -> msg.contains("Retrieving TP file"))
                .toList();
        assertThat(matchingLogsBusBreaker).hasSize(3);
    }

    @Test
    void exportCgmesSshTest() throws IOException {
        final String networkFileName = "/export_cgmes/TestCase_with_swe_countries.xiidm";
        final String subNetwork = "urn:uuid:563eadb1-4dfa-9784-a7ad-c8eddaaf3103"; //subnetwork of REE
        Network network = setTestNetwork(networkFileName, subNetwork);
        final SweData sweData = createSweData(new EnumMap<>(CgmesFileType.class), boundariesFiles);

        final Map<String, ByteArrayOutputStream> sshFiles = cgmesExportService.createSshTpFilesForAllTso(network,
                                                                                                         sweData,
                                                                                                         new ArrayList<>(),
                                                                                                         new ArrayList<>(),
                                                                                                         temporaryBoundariesPath);
        assertEquals(3, sshFiles.size());
        assertTrue(sshFiles.containsKey("20230731T0030Z_2D_REE_SSH_006"));
        assertTrue(sshFiles.containsKey("20230731T0030Z_2D_REN_SSH_001"));
        assertTrue(sshFiles.containsKey("20230731T0030Z_2D_RTEFRANCE_SSH_001"));
    }

    @Test
    void exportCgmesSshTpNodeBreakerTest() throws IOException {
        final String networkFileName = "/export_cgmes/TestCase_with_swe_countries_node_breaker_PT.xiidm";
        final Network network = Network.read(networkFileName, getClass().getResourceAsStream(networkFileName));
        final SweData sweData = createSweData(new EnumMap<>(CgmesFileType.class), boundariesFiles);

        final Map<String, ByteArrayOutputStream> sshFiles = cgmesExportService.createSshTpFilesForAllTso(network,
                                                                                                         sweData,
                                                                                                         new ArrayList<>(),
                                                                                                         new ArrayList<>(),
                                                                                                         temporaryBoundariesPath);
        // As PT (REN) has a Node Beraker Typoology, T file is created for the REN TSO
        assertEquals(4, sshFiles.size());
        assertTrue(sshFiles.containsKey("20230731T0030Z_2D_REE_SSH_001"));
        assertTrue(sshFiles.containsKey("20230731T0030Z_2D_REN_SSH_001"));
        assertTrue(sshFiles.containsKey("20230731T0030Z_2D_REN_TP_001"));
        assertTrue(sshFiles.containsKey("20230731T0030Z_2D_RTEFRANCE_SSH_001"));
    }

    @Test
    void exportCgmesFilesWithMissingCountryTest() throws IOException {
        //In cas of subnetwork contains many countries it will not be exported
        final String networkFileName = "/export_cgmes/TestCase_with_swe_countries_error.xiidm";
        final Network network = Network.read(networkFileName, getClass().getResourceAsStream(networkFileName));

        final SweFileResource sweFileResource = new SweFileResource("name", "url");
        final SweRequest sweRequest = createSweRequest(sweFileResource);
        List<SweFileResource> boundariesFiles = List.of(sweRequest.getBoundaryEq(), sweRequest.getBoundaryTp());
        final SweData sweData = createSweData(new EnumMap<>(CgmesFileType.class), boundariesFiles);

        final Map<String, ByteArrayOutputStream> sshFiles = cgmesExportService.createSshTpFilesForAllTso(network,
                                                                                                         sweData,
                                                                                                         new ArrayList<>(),
                                                                                                         new ArrayList<>(),
                                                                                                         temporaryBoundariesPath);
        assertEquals(2, sshFiles.size());
        assertFalse(sshFiles.containsKey("20230731T0030Z_2D_REE_SSH_001"));
        assertTrue(sshFiles.containsKey("20230731T0030Z_2D_RTEFRANCE_SSH_001"));
    }

    private static @NonNull SweData createSweData(Map<CgmesFileType, SweFileResource> cgmesInputFiles,
                                                  List<SweFileResource> boundariesFiles) {
        return new SweData("id", "runId", OffsetDateTime.parse("2023-07-31T00:30:00Z"),
                           ProcessType.D2CC, null, null, null,
                           null, null, null, "glskUrl",
                           "CracEsPt", "CracFrEs",
                           "raoParametersEsFrUrl", "raoParametersEsPtUrl",
                           Collections.EMPTY_LIST, cgmesInputFiles, boundariesFiles);
    }

    private static @NonNull SweRequest createSweRequest(final SweFileResource sweFileResource) {
        return new SweRequest("id", "runId", ProcessType.D2CC,
                              OffsetDateTime.now(), sweFileResource, sweFileResource, sweFileResource,
                              sweFileResource, sweFileResource, sweFileResource, sweFileResource, sweFileResource,
                              sweFileResource, sweFileResource, sweFileResource,
                              new SweFileResource("boundaryEqName", "boundaryEqUrl"),
                              new SweFileResource("boundaryTpName", "boundaryTpUrl"),
                              sweFileResource, new ArrayList<>());
    }

    private void exportCgmesZipFile(Map<String, ByteArrayOutputStream> mapCgmesFiles,
                                    String path) throws IOException {
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

    private Network setTestNetwork(String networkFileName,
                                   String subNetwork) {
        Network network = Network.read(networkFileName, getClass().getResourceAsStream(networkFileName));
        network.getSubnetwork(subNetwork)
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
        return network;
    }

    private Map<CgmesFileType, SweFileResource> getCgmesInputFiles() {
        Map<CgmesFileType, SweFileResource> cgmesInputFiles = new EnumMap<>(CgmesFileType.class);

        cgmesInputFiles.put(CgmesFileType.REE_EQ, new SweFileResource("REE_EQ.xml", Objects.requireNonNull(getClass().getResource("/network/MicroGrid_SWE/network_ES_EQ.xml")).toExternalForm()));
        cgmesInputFiles.put(CgmesFileType.REE_TP, new SweFileResource("REE_TP.xml", Objects.requireNonNull(getClass().getResource("/network/MicroGrid_SWE/network_ES_TP.xml")).toExternalForm()));
        cgmesInputFiles.put(CgmesFileType.REN_EQ, new SweFileResource("REN_EQ.xml", Objects.requireNonNull(getClass().getResource("/network/MicroGrid_SWE/network_PT_EQ.xml")).toExternalForm()));
        cgmesInputFiles.put(CgmesFileType.REN_TP, new SweFileResource("REN_TP.xml", Objects.requireNonNull(getClass().getResource("/network/MicroGrid_SWE/network_PT_TP.xml")).toExternalForm()));
        cgmesInputFiles.put(CgmesFileType.RTE_EQ, new SweFileResource("RTE_EQ.xml", Objects.requireNonNull(getClass().getResource("/network/MicroGrid_SWE/network_FR_EQ.xml")).toExternalForm()));
        cgmesInputFiles.put(CgmesFileType.RTE_TP, new SweFileResource("RTE_TP.xml", Objects.requireNonNull(getClass().getResource("/network/MicroGrid_SWE/network_FR_TP.xml")).toExternalForm()));
        return cgmesInputFiles;
    }
}

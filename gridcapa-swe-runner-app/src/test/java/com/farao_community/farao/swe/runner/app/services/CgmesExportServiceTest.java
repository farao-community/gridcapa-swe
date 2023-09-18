/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
import com.farao_community.farao.data.crac_impl.CracImpl;
import com.farao_community.farao.data.rao_result_impl.RaoResultImpl;
import com.farao_community.farao.dichotomy.api.index.Index;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.dichotomy.api.results.DichotomyStepResult;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.swe.runner.api.exception.SweInternalException;
import com.farao_community.farao.swe.runner.api.resource.ProcessType;
import com.farao_community.farao.swe.runner.api.resource.SweFileResource;
import com.farao_community.farao.swe.runner.app.dichotomy.DichotomyDirection;
import com.farao_community.farao.swe.runner.app.domain.CgmesFileType;
import com.farao_community.farao.swe.runner.app.domain.MergingViewData;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyValidationData;
import com.farao_community.farao.swe.runner.app.utils.UrlValidationService;
import com.google.common.base.Suppliers;
import com.powsybl.cgmes.conversion.CgmesImport;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.mergingview.MergingView;
import com.powsybl.iidm.network.ImportConfig;
import com.powsybl.iidm.network.Network;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.Objects;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@SpringBootTest
class CgmesExportServiceTest {

    @MockBean
    private FileExporter fileExporter;
    @Autowired
    private CgmesExportService cgmesExportService;
    @Autowired
    private UrlValidationService urlValidationService;

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
    void testApplyingShiftToCgm() throws URISyntaxException {
        Network inputNetwork = importFromZip(Paths.get(Objects.requireNonNull(getClass().getResource("/export_cgmes/MicroGrid.zip")).toURI()).toString());
        assertEquals(140, inputNetwork.getGenerator("_2844585c-0d35-488d-a449-685bcd57afbf").getTargetP());
        assertEquals(90., inputNetwork.getLoad("_69add5b4-70bd-4360-8a93-286256c0d38b").getP0());
        MergingViewData mergingViewData = new MergingViewData(inputNetwork, inputNetwork, inputNetwork, null);
        String networkWithPraUrl = getClass().getResource("/export_cgmes/microGridPra.xiidm").toString();
        try (InputStream networkIs = urlValidationService.openUrlStream(networkWithPraUrl)) {
            Network networkWithPra = Network.read("networkWithPra.xiidm", networkIs);
            assertEquals(210, networkWithPra.getGenerator("_2844585c-0d35-488d-a449-685bcd57afbf").getTargetP());
            assertEquals(50., networkWithPra.getLoad("_69add5b4-70bd-4360-8a93-286256c0d38b").getP0());
            cgmesExportService.applyNetworkWithPraResultToMergingViewData(networkWithPra, mergingViewData);
            assertEquals(210, inputNetwork.getGenerator("_2844585c-0d35-488d-a449-685bcd57afbf").getTargetP());
            assertEquals(50., inputNetwork.getLoad("_69add5b4-70bd-4360-8a93-286256c0d38b").getP0());
        } catch (IOException e) {
            throw new SweInternalException("Could not export CGMES files", e);
        }
    }

    @Test
    void testBuildAndExportCgmesFilesNoValidStep() {
        Index<SweDichotomyValidationData> index = new Index<>(0d, 0d, 0d);
        DichotomyResult<SweDichotomyValidationData> dichotomyResult = DichotomyResult.buildFromIndex(index);
        assertNull(cgmesExportService.buildAndExportCgmesFiles(null, null, dichotomyResult));
    }

    @Test
    void testBuildAndExportCgmesFiles() throws URISyntaxException {
        Network inputNetwork = importFromZip(Paths.get(Objects.requireNonNull(getClass().getResource("/export_cgmes/MicroGrid.zip")).toURI()).toString());
        assertEquals(140, inputNetwork.getGenerator("_2844585c-0d35-488d-a449-685bcd57afbf").getTargetP());
        assertEquals(90., inputNetwork.getLoad("_69add5b4-70bd-4360-8a93-286256c0d38b").getP0());
        MergingView mergingView = MergingView.create("imported_network", "iidm");
        Index<SweDichotomyValidationData> index = new Index<>(0d, 2950d, 0d);
        RaoResultImpl raoResult = Mockito.mock(RaoResultImpl.class);
        RaoResponse raoResponse = Mockito.mock(RaoResponse.class);
        when(raoResponse.getNetworkWithPraFileUrl()).thenReturn(getClass().getResource("/export_cgmes/microGridPra.xiidm").toString());
        DichotomyStepResult<SweDichotomyValidationData> validStep = DichotomyStepResult.fromNetworkValidationResult(raoResult, new SweDichotomyValidationData(raoResponse), true);
        index.addDichotomyStepResult(2950d, validStep);
        DichotomyResult<SweDichotomyValidationData> dichotomyResult = DichotomyResult.buildFromIndex(index);
        CimCracCreationContext cracEsFR = Mockito.mock(CimCracCreationContext.class);
        when(cracEsFR.getCrac()).thenReturn(new CracImpl("test", "testName"));
        SweData sweData = new SweData(
                "test",
                OffsetDateTime.now(),
                ProcessType.D2CC,
                null,
                null,
                null,
                null,
                new MergingViewData(inputNetwork, inputNetwork, inputNetwork, mergingView),
                cracEsFR,
                null,
                null,
                null,
                null,
                null,
                null,
                fillMapCgmesInputFiles()
                );
        String zipFileUrl = cgmesExportService.buildAndExportCgmesFiles(DichotomyDirection.ES_FR, sweData, dichotomyResult);
        assertNull(zipFileUrl);
    }

    private Network importFromZip(String zipPath) {
        Properties importParams = new Properties();
        importParams.put(CgmesImport.SOURCE_FOR_IIDM_ID, CgmesImport.SOURCE_FOR_IIDM_ID_RDFID);
        return Network.read(Paths.get(zipPath), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);
    }

    private EnumMap<CgmesFileType, SweFileResource> fillMapCgmesInputFiles() {
        SweFileResource testRessource = new SweFileResource("test", getClass().getResource("/export_cgmes/emptyTestFile.txt").toString());
        EnumMap<CgmesFileType, SweFileResource> mapCgmesInputFiles = new EnumMap<>(CgmesFileType.class);
        mapCgmesInputFiles.put(CgmesFileType.RTE_EQ, testRessource);
        mapCgmesInputFiles.put(CgmesFileType.RTE_TP, testRessource);
        mapCgmesInputFiles.put(CgmesFileType.REE_EQ, testRessource);
        mapCgmesInputFiles.put(CgmesFileType.REE_TP, testRessource);
        mapCgmesInputFiles.put(CgmesFileType.REN_EQ, testRessource);
        mapCgmesInputFiles.put(CgmesFileType.REN_TP, testRessource);
        return mapCgmesInputFiles;
    }
}

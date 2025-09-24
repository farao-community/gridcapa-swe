/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.gridcapa_swe_commons.configuration.ProcessConfiguration;
import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import com.farao_community.farao.gridcapa_swe_commons.resource.ProcessType;
import com.farao_community.farao.swe.runner.app.domain.SweTaskParameters;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyResult;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyValidationData;
import com.farao_community.farao.swe.runner.app.parallelization.ExecutionResult;
import com.farao_community.farao.swe.runner.app.utils.UrlValidationService;
import com.farao_community.farao.swe.runner.app.voltage.VoltageMonitoringResultTestUtils;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.io.cim.craccreator.CimCracCreationContext;
import com.powsybl.openrao.monitoring.results.RaoResultWithVoltageMonitoring;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */
class OutputServiceTest {

    public static final String TTC_DOCUMENT_URL_STRING = "ttcDocumentUrl";
    public static final String VOLTAGE_DOCUMENT_URL_STRING = "voltageDocumentUrl";

    private static OutputService outputService;
    private static FileExporter fileExporter;
    private static UrlValidationService urlValidationService;
    private static ProcessConfiguration processConfiguration;
    private SweData sweData;
    private static ExecutionResult<SweDichotomyResult> executionResult;

    private final OffsetDateTime dateTime = OffsetDateTime.parse("2021-04-01T21:30Z");

    @BeforeAll
    static void init() {
        fileExporter = Mockito.mock(FileExporter.class);
        processConfiguration = Mockito.mock(ProcessConfiguration.class);
        Crac crac = Mockito.mock(Crac.class);
        Mockito.when(fileExporter.saveVoltageMonitoringResultInJsonZip(any(), any(), any(), any(), Mockito.anyString(), any()))
                .thenReturn(VOLTAGE_DOCUMENT_URL_STRING);
        Mockito.when(processConfiguration.getZoneId()).thenReturn("Europe/Paris");
        outputService = new OutputService(fileExporter, processConfiguration, urlValidationService);
        List<SweDichotomyResult> resultList = new ArrayList<>();
        resultList.add(getSweDichotomyResult(DichotomyDirection.FR_ES, Optional.of(VoltageMonitoringResultTestUtils.getMonitoringResult())));
        resultList.add(getSweDichotomyResult(DichotomyDirection.ES_FR, Optional.of(VoltageMonitoringResultTestUtils.getMonitoringResult())));
        resultList.add(getSweDichotomyResult(DichotomyDirection.PT_ES, Optional.empty()));
        resultList.add(getSweDichotomyResult(DichotomyDirection.ES_FR, Optional.empty()));
        executionResult = new ExecutionResult<>(resultList);
    }

    @BeforeEach
    void beforeEach() {
        Mockito.reset(fileExporter);
        CimCracCreationContext contextMocked = Mockito.mock(CimCracCreationContext.class);
        Crac crac = Mockito.mock(Crac.class);
        sweData = Mockito.mock(SweData.class);
        Mockito.when(sweData.getTimestamp()).thenReturn(dateTime);
        Mockito.when(sweData.getProcessType()).thenReturn(ProcessType.D2CC);
        Mockito.when(sweData.getCracFrEs()).thenReturn(contextMocked);
        Mockito.when(contextMocked.getCrac()).thenReturn(crac);
    }

    @Test
    void buildAndExportTtcDocument() {
        Mockito.when(fileExporter.exportTtcDocument(any(SweData.class), any(InputStream.class), Mockito.anyString())).thenReturn(TTC_DOCUMENT_URL_STRING);
        Assertions.assertEquals(TTC_DOCUMENT_URL_STRING, outputService.buildAndExportTtcDocument(sweData, executionResult));
    }

    @Test
    void buildAndExportEsFrVoltageDoc() {
        SweTaskParameters sweTaskParameters = Mockito.mock(SweTaskParameters.class);
        Mockito.when(sweTaskParameters.isRunVoltageCheck()).thenReturn(true);
        outputService.buildAndExportVoltageDoc(DichotomyDirection.ES_FR, sweData, Optional.of(VoltageMonitoringResultTestUtils.getMonitoringResult()), sweTaskParameters);
        Mockito.verify(fileExporter, Mockito.times(1)).saveVoltageMonitoringResultInJsonZip(any(RaoResultWithVoltageMonitoring.class), Mockito.anyString(), any(OffsetDateTime.class), any(ProcessType.class), Mockito.anyString(), any());
    }

    @Test
    void buildAndExportFrEsFailureVoltageDoc() {
        SweTaskParameters sweTaskParameters = Mockito.mock(SweTaskParameters.class);
        Mockito.when(sweTaskParameters.isRunVoltageCheck()).thenReturn(true);
        outputService.buildAndExportVoltageDoc(DichotomyDirection.FR_ES, sweData, Optional.empty(), sweTaskParameters);
        Mockito.verify(fileExporter, Mockito.times(1)).saveVoltageMonitoringResultInJsonZip(Mockito.isNull(), Mockito.anyString(), any(OffsetDateTime.class), any(ProcessType.class), Mockito.anyString(), any());
    }

    @Test
    void noBuildAndExportFrEsVoltageDoc() {
        SweTaskParameters sweTaskParameters = Mockito.mock(SweTaskParameters.class);
        Mockito.when(sweTaskParameters.isRunVoltageCheck()).thenReturn(false);
        outputService.buildAndExportVoltageDoc(DichotomyDirection.FR_ES, sweData, Optional.empty(), sweTaskParameters);
        Mockito.verify(fileExporter, Mockito.times(0)).saveVoltageMonitoringResultInJsonZip(Mockito.isNull(), Mockito.anyString(), any(OffsetDateTime.class), any(ProcessType.class), Mockito.anyString(), any());
    }

    @Test
    void noBuildAndExportPtEsVoltageDoc() {
        SweTaskParameters sweTaskParameters = Mockito.mock(SweTaskParameters.class);
        Mockito.when(sweTaskParameters.isRunVoltageCheck()).thenReturn(true);
        outputService.buildAndExportVoltageDoc(DichotomyDirection.PT_ES, sweData, Optional.empty(), sweTaskParameters);
        Mockito.verify(fileExporter, Mockito.times(0)).saveVoltageMonitoringResultInJsonZip(Mockito.isNull(), Mockito.anyString(), any(OffsetDateTime.class), any(ProcessType.class), Mockito.anyString(), any());
    }

    @NotNull
    private static SweDichotomyResult getSweDichotomyResult(DichotomyDirection direction, Optional<RaoResultWithVoltageMonitoring> voltageMonitoringResult) {
        DichotomyResult<SweDichotomyValidationData> dichotomyResult1 = Mockito.mock(DichotomyResult.class);
        Mockito.when(dichotomyResult1.hasValidStep()).thenReturn(true);
        Mockito.when(dichotomyResult1.getHighestValidStepValue()).thenReturn(12345.90);
        SweDichotomyResult sweDichotomyResult1 = new SweDichotomyResult(direction, dichotomyResult1, voltageMonitoringResult, "exportedCgmesUrl", "", "");
        return sweDichotomyResult1;
    }
}

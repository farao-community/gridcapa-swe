/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.gridcapa_swe_commons.configuration.ProcessConfiguration;
import com.farao_community.farao.monitoring.voltage_monitoring.VoltageMonitoringResult;
import com.farao_community.farao.swe.runner.api.resource.ProcessType;
import com.farao_community.farao.swe.runner.app.dichotomy.DichotomyDirection;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyResult;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyValidationData;
import com.farao_community.farao.swe.runner.app.parallelization.ExecutionResult;
import com.farao_community.farao.swe.runner.app.voltage.VoltageMonitoringResultTestUtils;
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

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */
class OutputServiceTest {

    public static final String TTC_DOCUMENT_URL_STRING = "ttcDocumentUrl";
    public static final String VOLTAGE_DOCUMENT_URL_STRING = "voltageDocumentUrl";

    private static OutputService outputService;
    private static FileExporter fileExporter;
    private static ProcessConfiguration processConfiguration;
    private SweData sweData;
    private static ExecutionResult<SweDichotomyResult> executionResult;

    private final OffsetDateTime dateTime = OffsetDateTime.parse("2021-04-01T21:30Z");

    @BeforeAll
    static void init() {
        fileExporter = Mockito.mock(FileExporter.class);
        processConfiguration = Mockito.mock(ProcessConfiguration.class);
        Mockito.when(fileExporter.saveVoltageMonitoringResultInJsonZip(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyString()))
                .thenReturn(VOLTAGE_DOCUMENT_URL_STRING);
        Mockito.when(processConfiguration.getZoneId()).thenReturn("Europe/Paris");
        outputService = new OutputService(fileExporter, processConfiguration);
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
        sweData = Mockito.mock(SweData.class);
        Mockito.when(sweData.getTimestamp()).thenReturn(dateTime);
        Mockito.when(sweData.getProcessType()).thenReturn(ProcessType.D2CC);
    }

    @Test
    void buildAndExportTtcDocument() {
        Mockito.when(fileExporter.exportTtcDocument(Mockito.any(SweData.class), Mockito.any(InputStream.class), Mockito.anyString())).thenReturn(TTC_DOCUMENT_URL_STRING);
        Assertions.assertEquals(TTC_DOCUMENT_URL_STRING, outputService.buildAndExportTtcDocument(sweData, executionResult));
    }

    @Test
    void buildAndExportEsFrVoltageDoc() {
        outputService.buildAndExportVoltageDoc(DichotomyDirection.ES_FR, sweData, Optional.of(VoltageMonitoringResultTestUtils.getMonitoringResult()));
        Mockito.verify(fileExporter, Mockito.times(1)).saveVoltageMonitoringResultInJsonZip(Mockito.any(VoltageMonitoringResult.class), Mockito.anyString(), Mockito.any(OffsetDateTime.class), Mockito.any(ProcessType.class), Mockito.anyString());
    }

    @Test
    void buildAndExportFrEsFailureVoltageDoc() {
        outputService.buildAndExportVoltageDoc(DichotomyDirection.FR_ES, sweData, Optional.empty());
        Mockito.verify(fileExporter, Mockito.times(1)).saveVoltageMonitoringResultInJsonZip(Mockito.isNull(), Mockito.anyString(), Mockito.any(OffsetDateTime.class), Mockito.any(ProcessType.class), Mockito.anyString());
    }

    @Test
    void noBuildAndExportPtEsVoltageDoc() {
        outputService.buildAndExportVoltageDoc(DichotomyDirection.PT_ES, sweData, Optional.empty());
        Mockito.verify(fileExporter, Mockito.times(0)).saveVoltageMonitoringResultInJsonZip(Mockito.isNull(), Mockito.anyString(), Mockito.any(OffsetDateTime.class), Mockito.any(ProcessType.class), Mockito.anyString());
    }

    @NotNull
    private static SweDichotomyResult getSweDichotomyResult(DichotomyDirection direction, Optional<VoltageMonitoringResult> voltageMonitoringResult) {
        DichotomyResult<SweDichotomyValidationData> dichotomyResult1 = Mockito.mock(DichotomyResult.class);
        Mockito.when(dichotomyResult1.hasValidStep()).thenReturn(true);
        Mockito.when(dichotomyResult1.getHighestValidStepValue()).thenReturn(12345.90);
        SweDichotomyResult sweDichotomyResult1 = new SweDichotomyResult(direction, dichotomyResult1, voltageMonitoringResult, "exportedCgmesUrl", "", "");
        return sweDichotomyResult1;
    }
}

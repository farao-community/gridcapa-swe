/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.parallelization;

import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import com.farao_community.farao.swe.runner.app.dichotomy.DichotomyLogging;
import com.farao_community.farao.swe.runner.app.dichotomy.DichotomyRunner;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyResult;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyValidationData;
import com.farao_community.farao.swe.runner.app.domain.SweTaskParameters;
import com.farao_community.farao.swe.runner.app.services.CgmesExportService;
import com.farao_community.farao.swe.runner.app.services.CneFileExportService;
import com.farao_community.farao.swe.runner.app.services.OutputService;
import com.farao_community.farao.swe.runner.app.services.VoltageCheckService;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */
@SpringBootTest
class DichotomyParallelizationWorkerTest {

    @Autowired
    private DichotomyParallelizationWorker dichotomyParallelizationWorker;

    @MockBean
    private DichotomyLogging dichotomyLogging;
    @MockBean
    private DichotomyRunner dichotomyRunner;
    @MockBean
    private VoltageCheckService voltageCheckService;
    @MockBean
    private CneFileExportService cneFileExportService;
    @MockBean
    private CgmesExportService cgmesExportService;
    @MockBean
    private OutputService outputService;

    @Mock
    private SweData sweData;
    @Mock
    private DichotomyResult<SweDichotomyValidationData> result;

    private final OffsetDateTime startingTime = OffsetDateTime.now();

    @Test
    void testRunDichotomyForOneDirection() {
        DichotomyDirection direction = DichotomyDirection.ES_PT;
        when(dichotomyRunner.run(any(SweData.class), any(SweTaskParameters.class), any(DichotomyDirection.class))).thenReturn(result);
        when(cgmesExportService.buildAndExportCgmesFiles(any(DichotomyDirection.class), any(SweData.class), any(DichotomyResult.class), any(SweTaskParameters.class))).thenReturn("cgmesZipFileUrl");
        when(cneFileExportService.exportCneUrl(any(SweData.class), any(DichotomyResult.class), anyBoolean(), any(DichotomyDirection.class))).thenReturn("CneUrl");
        when(voltageCheckService.runVoltageCheck(any(SweData.class), any(DichotomyResult.class), any(SweTaskParameters.class), any(DichotomyDirection.class))).thenReturn(Optional.empty());
        SweTaskParameters sweTaskParameters = Mockito.mock(SweTaskParameters.class);
        Mockito.when(sweTaskParameters.getMinTtcEsPt()).thenReturn(0);
        Mockito.when(sweTaskParameters.getMaxTtcEsPt()).thenReturn(6400);
        Mockito.when(sweTaskParameters.getDichotomyPrecisionEsPt()).thenReturn(50);
        Future<SweDichotomyResult> futurResult = dichotomyParallelizationWorker.runDichotomyForOneDirection(sweData, sweTaskParameters, direction, startingTime);
        try {
            SweDichotomyResult sweDichotomyResult = futurResult.get();
            assertTrue(futurResult.isDone());
            assertEquals(direction, sweDichotomyResult.getDichotomyDirection());
            assertEquals("CneUrl", sweDichotomyResult.getHighestValidStepUrl());
            assertEquals("CneUrl", sweDichotomyResult.getLowestInvalidStepUrl());
            assertEquals("cgmesZipFileUrl", sweDichotomyResult.getExportedCgmesUrl());
            assertEquals(Optional.empty(), sweDichotomyResult.getVoltageMonitoringResult());
        } catch (InterruptedException | ExecutionException e) {
            fail(e);
        }
        verify(outputService, times(1)).buildAndExportVoltageDoc(any(DichotomyDirection.class), any(SweData.class), any(Optional.class), any(SweTaskParameters.class));
        verify(dichotomyLogging, times(1)).generateSummaryEvents(any(DichotomyDirection.class), any(DichotomyResult.class), any(SweData.class), any(Optional.class), any(SweTaskParameters.class), any(OffsetDateTime.class));
    }

    @Test
    void testRunDichotomyRaoFailure() {
        DichotomyDirection direction = DichotomyDirection.ES_PT;
        DichotomyResult<SweDichotomyValidationData> customResult = DichotomyResult.buildFromRaoFailure("failure");
        when(dichotomyRunner.run(any(SweData.class), any(SweTaskParameters.class), any(DichotomyDirection.class))).thenReturn(customResult);
        SweTaskParameters sweTaskParameters = Mockito.mock(SweTaskParameters.class);

        Future<SweDichotomyResult> futurResult = dichotomyParallelizationWorker.runDichotomyForOneDirection(sweData, sweTaskParameters, direction, startingTime);

        try {
            SweDichotomyResult sweDichotomyResult = futurResult.get();
            assertTrue(futurResult.isDone());
            assertEquals(direction, sweDichotomyResult.getDichotomyDirection());
            assertTrue(sweDichotomyResult.isRaoFailed());
        } catch (InterruptedException | ExecutionException e) {
            fail(e);
        }
    }
}

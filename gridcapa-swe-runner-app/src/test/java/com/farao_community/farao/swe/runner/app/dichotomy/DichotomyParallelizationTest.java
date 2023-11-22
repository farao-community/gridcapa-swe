/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.CracFactory;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.dichotomy.api.results.DichotomyStepResult;
import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import com.farao_community.farao.swe.runner.api.exception.SweInternalException;
import com.farao_community.farao.swe.runner.api.resource.ProcessType;
import com.farao_community.farao.swe.runner.api.resource.SweResponse;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyResult;
import com.farao_community.farao.swe.runner.app.parallelization.DichotomyParallelizationWorker;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyValidationData;
import com.farao_community.farao.swe.runner.app.parallelization.ExecutionResult;
import com.farao_community.farao.swe.runner.app.services.CgmesExportService;
import com.farao_community.farao.swe.runner.app.services.CneFileExportService;
import com.farao_community.farao.swe.runner.app.services.OutputService;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.scheduling.annotation.AsyncResult;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@SpringBootTest
class DichotomyParallelizationTest {

    @Autowired
    private DichotomyParallelization dichotomyParallelization;

    @MockBean
    private DichotomyRunner dichotomyRunner;

    @MockBean
    private OutputService outputService;

    @MockBean
    private CneFileExportService cneFileExportService;

    @MockBean
    private CgmesExportService cgmesExportService;

    @MockBean
    private DichotomyParallelizationWorker worker;

    @Mock
    private SweData sweData;

    @Mock
    private DichotomyResult<SweDichotomyValidationData> sweDichotomyResult;

    @Mock
    private DichotomyStepResult<SweDichotomyValidationData> highestValidStep;

    @Mock
    private DichotomyStepResult<SweDichotomyValidationData> lowestInvalidStep;

    @Mock
    private RaoResult raoResult;

    @Mock
    private ExecutionResult<SweDichotomyResult> executionResult;

    @Mock
    private CimCracCreationContext cracCreationContext;

    @Mock
    private Future future;

    private Network network;
    private Crac crac;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        network = Network.read("network.xiidm", getClass().getResourceAsStream("/network/network.xiidm"));
        crac = CracFactory.findDefault().create("test-crac");
    }

    @Test
    void testParallelization() {
        when(dichotomyRunner.run(any(SweData.class), any(DichotomyDirection.class))).thenReturn(sweDichotomyResult);
        when(outputService.buildAndExportTtcDocument(any(SweData.class), any(ExecutionResult.class))).thenReturn("ttcDocUrl");
        when(cgmesExportService.buildAndExportCgmesFiles(any(), any(), any())).thenReturn("ok");
        when(sweDichotomyResult.getHighestValidStep()).thenReturn(highestValidStep);
        when(highestValidStep.getRaoResult()).thenReturn(raoResult);
        when(sweDichotomyResult.hasValidStep()).thenReturn(true);
        when(sweDichotomyResult.getLowestInvalidStep()).thenReturn(lowestInvalidStep);
        when(lowestInvalidStep.getRaoResult()).thenReturn(raoResult);
        when(sweData.getCracFrEs()).thenReturn(cracCreationContext);
        when(sweData.getCracEsPt()).thenReturn(cracCreationContext);
        when(sweData.getProcessType()).thenReturn(ProcessType.D2CC);
        when(cracCreationContext.getCrac()).thenReturn(crac);
        when(sweData.getNetworkEsFr()).thenReturn(network);
        when(sweData.getNetworkEsPt()).thenReturn(network);
        when(sweData.getNetworkFrEs()).thenReturn(network);
        when(sweData.getNetworkPtEs()).thenReturn(network);
        SweDichotomyResult result = new SweDichotomyResult(DichotomyDirection.ES_FR, sweDichotomyResult, Optional.empty(), null, "esFrHighestValidStepUrl.zip", "esFrLowestInvalidStepUrl.zip");
        when(worker.runDichotomyForOneDirection(sweData, DichotomyDirection.ES_FR)).thenReturn(new AsyncResult<>(result));
        when(worker.runDichotomyForOneDirection(sweData, DichotomyDirection.FR_ES)).thenReturn(new AsyncResult<>(result));
        when(worker.runDichotomyForOneDirection(sweData, DichotomyDirection.ES_PT)).thenReturn(new AsyncResult<>(result));
        when(worker.runDichotomyForOneDirection(sweData, DichotomyDirection.PT_ES)).thenReturn(new AsyncResult<>(result));
        SweResponse sweResponse = dichotomyParallelization.launchDichotomy(sweData);
        assertEquals("ttcDocUrl", sweResponse.getTtcDocUrl());
    }

    @Test
    void testParallelizationWithInvalidResult() {
        when(dichotomyRunner.run(any(SweData.class), any(DichotomyDirection.class))).thenReturn(sweDichotomyResult);
        when(outputService.buildAndExportTtcDocument(any(SweData.class), any(ExecutionResult.class))).thenReturn("ttcDocUrl");
        when(sweDichotomyResult.hasValidStep()).thenReturn(false);
        when(sweDichotomyResult.getLowestInvalidStep()).thenReturn(lowestInvalidStep);
        when(lowestInvalidStep.getRaoResult()).thenReturn(raoResult);
        when(sweData.getCracFrEs()).thenReturn(cracCreationContext);
        when(cracCreationContext.getCrac()).thenReturn(crac);
        when(sweData.getNetworkEsFr()).thenReturn(network);
        when(sweData.getProcessType()).thenReturn(ProcessType.D2CC);
        SweDichotomyResult result = new SweDichotomyResult(DichotomyDirection.ES_FR, sweDichotomyResult, Optional.empty(), null, null, "esFrLowestInvalidStepUrl.zip");
        when(worker.runDichotomyForOneDirection(sweData, DichotomyDirection.ES_FR)).thenReturn(new AsyncResult<>(result));
        when(worker.runDichotomyForOneDirection(sweData, DichotomyDirection.FR_ES)).thenReturn(new AsyncResult<>(result));
        when(worker.runDichotomyForOneDirection(sweData, DichotomyDirection.ES_PT)).thenReturn(new AsyncResult<>(result));
        when(worker.runDichotomyForOneDirection(sweData, DichotomyDirection.PT_ES)).thenReturn(new AsyncResult<>(result));
        SweResponse sweResponse = dichotomyParallelization.launchDichotomy(sweData);
        assertEquals("ttcDocUrl", sweResponse.getTtcDocUrl());
    }

    @Test
    void testParallelizationWithNoResult1() {
        when(dichotomyRunner.run(any(SweData.class), any(DichotomyDirection.class))).thenReturn(sweDichotomyResult);
        when(outputService.buildAndExportTtcDocument(any(SweData.class), any(ExecutionResult.class))).thenReturn("ttcDocUrl");
        when(sweDichotomyResult.hasValidStep()).thenReturn(false);
        when(sweDichotomyResult.getLowestInvalidStep()).thenReturn(lowestInvalidStep);
        when(lowestInvalidStep.getRaoResult()).thenReturn(raoResult);
        when(sweData.getCracFrEs()).thenReturn(cracCreationContext);
        when(cracCreationContext.getCrac()).thenReturn(crac);
        when(sweData.getNetworkEsFr()).thenReturn(network);
        when(sweData.getProcessType()).thenReturn(ProcessType.D2CC);
        SweDichotomyResult result = new SweDichotomyResult(DichotomyDirection.ES_FR, null, null, null, null, null);
        when(worker.runDichotomyForOneDirection(sweData, DichotomyDirection.ES_FR)).thenReturn(new AsyncResult<>(result));
        when(worker.runDichotomyForOneDirection(sweData, DichotomyDirection.FR_ES)).thenReturn(new AsyncResult<>(result));
        when(worker.runDichotomyForOneDirection(sweData, DichotomyDirection.ES_PT)).thenReturn(new AsyncResult<>(result));
        when(worker.runDichotomyForOneDirection(sweData, DichotomyDirection.PT_ES)).thenReturn(new AsyncResult<>(result));
        SweResponse sweResponse = dichotomyParallelization.launchDichotomy(sweData);
        assertEquals("ttcDocUrl", sweResponse.getTtcDocUrl());
    }

    @Test
    void testParallelizationWithNoResult2() {
        when(dichotomyRunner.run(any(SweData.class), any(DichotomyDirection.class))).thenReturn(sweDichotomyResult);
        when(outputService.buildAndExportTtcDocument(any(SweData.class), any(ExecutionResult.class))).thenReturn("ttcDocUrl");
        when(sweDichotomyResult.hasValidStep()).thenReturn(false);
        when(sweDichotomyResult.getLowestInvalidStep()).thenReturn(lowestInvalidStep);
        when(lowestInvalidStep.getRaoResult()).thenReturn(raoResult);
        when(sweData.getCracFrEs()).thenReturn(cracCreationContext);
        when(cracCreationContext.getCrac()).thenReturn(crac);
        when(sweData.getNetworkEsFr()).thenReturn(network);
        when(sweData.getProcessType()).thenReturn(ProcessType.D2CC);
        SweDichotomyResult result = new SweDichotomyResult(DichotomyDirection.ES_FR, null, null, null, null, null);
        when(worker.runDichotomyForOneDirection(sweData, DichotomyDirection.ES_FR)).thenReturn(CompletableFuture.failedFuture(new InterruptedException()));
        when(worker.runDichotomyForOneDirection(sweData, DichotomyDirection.FR_ES)).thenReturn(CompletableFuture.failedFuture(new InterruptedException()));
        when(worker.runDichotomyForOneDirection(sweData, DichotomyDirection.ES_PT)).thenReturn(CompletableFuture.failedFuture(new InterruptedException()));
        when(worker.runDichotomyForOneDirection(sweData, DichotomyDirection.PT_ES)).thenReturn(CompletableFuture.failedFuture(new InterruptedException()));
        assertThrows(SweInternalException.class, () -> dichotomyParallelization.launchDichotomy(sweData));
    }

    @Test
    void testParallelizationWithException() throws ExecutionException, InterruptedException {
        when(dichotomyRunner.run(any(SweData.class), any(DichotomyDirection.class))).thenReturn(sweDichotomyResult);
        when(outputService.buildAndExportTtcDocument(any(SweData.class), any(ExecutionResult.class))).thenReturn("ttcDocUrl");
        when(sweDichotomyResult.hasValidStep()).thenReturn(false);
        when(sweDichotomyResult.getLowestInvalidStep()).thenReturn(lowestInvalidStep);
        when(lowestInvalidStep.getRaoResult()).thenReturn(raoResult);
        when(sweData.getCracFrEs()).thenReturn(cracCreationContext);
        when(cracCreationContext.getCrac()).thenReturn(crac);
        when(sweData.getNetworkEsFr()).thenReturn(network);
        when(sweData.getProcessType()).thenReturn(ProcessType.D2CC);
        SweDichotomyResult result = new SweDichotomyResult(DichotomyDirection.ES_FR, null, null, null, null, null);
        when(worker.runDichotomyForOneDirection(sweData, DichotomyDirection.ES_FR)).thenReturn(future);
        when(worker.runDichotomyForOneDirection(sweData, DichotomyDirection.FR_ES)).thenReturn(future);
        when(worker.runDichotomyForOneDirection(sweData, DichotomyDirection.ES_PT)).thenReturn(future);
        when(worker.runDichotomyForOneDirection(sweData, DichotomyDirection.PT_ES)).thenReturn(future);
        when(future.get()).thenThrow(InterruptedException.class);
        verify(future, Mockito.times(0)).cancel(true);
    }
}

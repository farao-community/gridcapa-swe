/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy;

import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.dichotomy.api.results.DichotomyStepResult;
import com.farao_community.farao.gridcapa.task_manager.api.TaskParameterDto;
import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import com.farao_community.farao.gridcapa_swe_commons.exception.SweInternalException;
import com.farao_community.farao.gridcapa_swe_commons.resource.ProcessType;
import com.farao_community.farao.swe.runner.api.resource.SweResponse;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyResult;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyValidationData;
import com.farao_community.farao.swe.runner.app.domain.SweTaskParameters;
import com.farao_community.farao.swe.runner.app.parallelization.DichotomyParallelizationWorker;
import com.farao_community.farao.swe.runner.app.parallelization.ExecutionResult;
import com.farao_community.farao.swe.runner.app.services.CgmesExportService;
import com.farao_community.farao.swe.runner.app.services.CneFileExportService;
import com.farao_community.farao.swe.runner.app.services.OutputService;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracFactory;
import com.powsybl.openrao.data.crac.io.cim.craccreator.CimCracCreationContext;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@SpringBootTest
class DichotomyParallelizationTest {

    @Autowired
    private DichotomyParallelization dichotomyParallelization;

    @MockitoBean
    private DichotomyRunner dichotomyRunner;

    @MockitoBean
    private OutputService outputService;

    @MockitoBean
    private CneFileExportService cneFileExportService;

    @MockitoBean
    private CgmesExportService cgmesExportService;

    @MockitoBean
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
    private SweTaskParameters defaultParameters;
    private final OffsetDateTime startingTime = OffsetDateTime.now();

    @BeforeEach
    void setup() {
        network = Network.read("network.xiidm", getClass().getResourceAsStream("/network/network.xiidm"));
        crac = CracFactory.findDefault().create("test-crac");
        defaultParameters = new TestParametersBuilder()
                .withEsFr(true).withFrEs(true)
                .withEsPt(true).withPtEs(true)
                .build();
    }

    @Test
    void testParallelization() {
        when(dichotomyRunner.run(any(SweData.class), any(SweTaskParameters.class), any(DichotomyDirection.class))).thenReturn(sweDichotomyResult);
        when(outputService.buildAndExportTtcDocument(any(SweData.class), any(ExecutionResult.class))).thenReturn("ttcDocUrl");
        when(cgmesExportService.buildAndExportLastSecureCgmesFiles(any(), any(), any(), any())).thenReturn("ok");
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
        SweDichotomyResult result = new SweDichotomyResult(DichotomyDirection.ES_FR, sweDichotomyResult, Optional.empty(), null, null, "esFrHighestValidStepUrl.zip", "esFrLowestInvalidStepUrl.zip");
        when(worker.runDichotomyForOneDirection(sweData, defaultParameters, DichotomyDirection.ES_FR, startingTime)).thenReturn(CompletableFuture.completedFuture(result));
        when(worker.runDichotomyForOneDirection(sweData, defaultParameters, DichotomyDirection.FR_ES, startingTime)).thenReturn(CompletableFuture.completedFuture(result));
        when(worker.runDichotomyForOneDirection(sweData, defaultParameters, DichotomyDirection.ES_PT, startingTime)).thenReturn(CompletableFuture.completedFuture(result));
        when(worker.runDichotomyForOneDirection(sweData, defaultParameters, DichotomyDirection.PT_ES, startingTime)).thenReturn(CompletableFuture.completedFuture(result));
        SweResponse sweResponse = dichotomyParallelization.launchDichotomy(sweData, defaultParameters, startingTime);
        assertEquals("ttcDocUrl", sweResponse.getTtcDocUrl());
        assertFalse(sweResponse.isInterrupted());
    }

    @Test
    void testParallelizationWithInvalidResult() {
        when(dichotomyRunner.run(any(SweData.class), any(SweTaskParameters.class), any(DichotomyDirection.class))).thenReturn(sweDichotomyResult);
        when(outputService.buildAndExportTtcDocument(any(SweData.class), any(ExecutionResult.class))).thenReturn("ttcDocUrl");
        when(sweDichotomyResult.hasValidStep()).thenReturn(false);
        when(sweDichotomyResult.getLowestInvalidStep()).thenReturn(lowestInvalidStep);
        when(lowestInvalidStep.getRaoResult()).thenReturn(raoResult);
        when(sweData.getCracFrEs()).thenReturn(cracCreationContext);
        when(cracCreationContext.getCrac()).thenReturn(crac);
        when(sweData.getNetworkEsFr()).thenReturn(network);
        when(sweData.getProcessType()).thenReturn(ProcessType.D2CC);
        SweDichotomyResult result = new SweDichotomyResult(DichotomyDirection.ES_FR, sweDichotomyResult, Optional.empty(), null, null, null, "esFrLowestInvalidStepUrl.zip");
        when(worker.runDichotomyForOneDirection(sweData, defaultParameters, DichotomyDirection.ES_FR, startingTime)).thenReturn(CompletableFuture.completedFuture(result));
        when(worker.runDichotomyForOneDirection(sweData, defaultParameters, DichotomyDirection.FR_ES, startingTime)).thenReturn(CompletableFuture.completedFuture(result));
        when(worker.runDichotomyForOneDirection(sweData, defaultParameters, DichotomyDirection.ES_PT, startingTime)).thenReturn(CompletableFuture.completedFuture(result));
        when(worker.runDichotomyForOneDirection(sweData, defaultParameters, DichotomyDirection.PT_ES, startingTime)).thenReturn(CompletableFuture.completedFuture(result));
        SweResponse sweResponse = dichotomyParallelization.launchDichotomy(sweData, defaultParameters, startingTime);
        assertEquals("ttcDocUrl", sweResponse.getTtcDocUrl());
    }

    @Test
    void testParallelizationWithNoResult1() {
        when(dichotomyRunner.run(any(SweData.class), any(SweTaskParameters.class), any(DichotomyDirection.class))).thenReturn(sweDichotomyResult);
        when(outputService.buildAndExportTtcDocument(any(SweData.class), any(ExecutionResult.class))).thenReturn("ttcDocUrl");
        when(sweDichotomyResult.hasValidStep()).thenReturn(false);
        when(sweDichotomyResult.getLowestInvalidStep()).thenReturn(lowestInvalidStep);
        when(lowestInvalidStep.getRaoResult()).thenReturn(raoResult);
        when(sweData.getCracFrEs()).thenReturn(cracCreationContext);
        when(cracCreationContext.getCrac()).thenReturn(crac);
        when(sweData.getNetworkEsFr()).thenReturn(network);
        when(sweData.getProcessType()).thenReturn(ProcessType.D2CC);
        SweDichotomyResult result = new SweDichotomyResult(DichotomyDirection.ES_FR, mock(DichotomyResult.class), null, null, null, null, null);
        when(worker.runDichotomyForOneDirection(sweData, defaultParameters, DichotomyDirection.ES_FR, startingTime)).thenReturn(CompletableFuture.completedFuture(result));
        when(worker.runDichotomyForOneDirection(sweData, defaultParameters, DichotomyDirection.FR_ES, startingTime)).thenReturn(CompletableFuture.completedFuture(result));
        when(worker.runDichotomyForOneDirection(sweData, defaultParameters, DichotomyDirection.ES_PT, startingTime)).thenReturn(CompletableFuture.completedFuture(result));
        when(worker.runDichotomyForOneDirection(sweData, defaultParameters, DichotomyDirection.PT_ES, startingTime)).thenReturn(CompletableFuture.completedFuture(result));
        SweResponse sweResponse = dichotomyParallelization.launchDichotomy(sweData, defaultParameters, startingTime);
        assertEquals("ttcDocUrl", sweResponse.getTtcDocUrl());
    }

    @Test
    void testParallelizationWithNoResult2() {
        when(dichotomyRunner.run(any(SweData.class), any(SweTaskParameters.class), any(DichotomyDirection.class))).thenReturn(sweDichotomyResult);
        when(outputService.buildAndExportTtcDocument(any(SweData.class), any(ExecutionResult.class))).thenReturn("ttcDocUrl");
        when(sweDichotomyResult.hasValidStep()).thenReturn(false);
        when(sweDichotomyResult.getLowestInvalidStep()).thenReturn(lowestInvalidStep);
        when(lowestInvalidStep.getRaoResult()).thenReturn(raoResult);
        when(sweData.getCracFrEs()).thenReturn(cracCreationContext);
        when(cracCreationContext.getCrac()).thenReturn(crac);
        when(sweData.getNetworkEsFr()).thenReturn(network);
        when(sweData.getProcessType()).thenReturn(ProcessType.D2CC);
        new SweDichotomyResult(DichotomyDirection.ES_FR, mock(DichotomyResult.class), null, null, null, null, null);
        when(worker.runDichotomyForOneDirection(sweData, defaultParameters, DichotomyDirection.ES_FR, startingTime)).thenReturn(CompletableFuture.failedFuture(new InterruptedException()));
        when(worker.runDichotomyForOneDirection(sweData, defaultParameters, DichotomyDirection.FR_ES, startingTime)).thenReturn(CompletableFuture.failedFuture(new InterruptedException()));
        when(worker.runDichotomyForOneDirection(sweData, defaultParameters, DichotomyDirection.ES_PT, startingTime)).thenReturn(CompletableFuture.failedFuture(new InterruptedException()));
        when(worker.runDichotomyForOneDirection(sweData, defaultParameters, DichotomyDirection.PT_ES, startingTime)).thenReturn(CompletableFuture.failedFuture(new InterruptedException()));
        assertThrows(SweInternalException.class, () -> dichotomyParallelization.launchDichotomy(sweData, defaultParameters, startingTime));
    }

    @Test
    void testParallelizationWithException() throws ExecutionException, InterruptedException {
        when(dichotomyRunner.run(any(SweData.class), any(SweTaskParameters.class), any(DichotomyDirection.class))).thenReturn(sweDichotomyResult);
        when(outputService.buildAndExportTtcDocument(any(SweData.class), any(ExecutionResult.class))).thenReturn("ttcDocUrl");
        when(sweDichotomyResult.hasValidStep()).thenReturn(false);
        when(sweDichotomyResult.getLowestInvalidStep()).thenReturn(lowestInvalidStep);
        when(lowestInvalidStep.getRaoResult()).thenReturn(raoResult);
        when(sweData.getCracFrEs()).thenReturn(cracCreationContext);
        when(cracCreationContext.getCrac()).thenReturn(crac);
        when(sweData.getNetworkEsFr()).thenReturn(network);
        when(sweData.getProcessType()).thenReturn(ProcessType.D2CC);
        new SweDichotomyResult(DichotomyDirection.ES_FR, mock(DichotomyResult.class), null, null, null, null, null);
        when(worker.runDichotomyForOneDirection(sweData, defaultParameters, DichotomyDirection.ES_FR, startingTime)).thenReturn(future);
        when(worker.runDichotomyForOneDirection(sweData, defaultParameters, DichotomyDirection.FR_ES, startingTime)).thenReturn(future);
        when(worker.runDichotomyForOneDirection(sweData, defaultParameters, DichotomyDirection.ES_PT, startingTime)).thenReturn(future);
        when(worker.runDichotomyForOneDirection(sweData, defaultParameters, DichotomyDirection.PT_ES, startingTime)).thenReturn(future);
        when(future.get()).thenThrow(InterruptedException.class);
        verify(future, Mockito.times(0)).cancel(true);
    }

    @Test
    void testParallelizationWithFrAndEsOnly() {
        SweTaskParameters onlyFrParameters = new TestParametersBuilder()
                .withEsFr(true).withFrEs(true)
                .withEsPt(false).withPtEs(false)
                .build();

        when(dichotomyRunner.run(any(SweData.class), any(SweTaskParameters.class), any(DichotomyDirection.class))).thenReturn(sweDichotomyResult);
        when(outputService.buildAndExportTtcDocument(any(SweData.class), any(ExecutionResult.class))).thenReturn("ttcDocUrl");
        when(cgmesExportService.buildAndExportLastSecureCgmesFiles(any(), any(), any(), any())).thenReturn("ok");
        when(sweDichotomyResult.getHighestValidStep()).thenReturn(highestValidStep);
        when(highestValidStep.getRaoResult()).thenReturn(raoResult);
        when(sweDichotomyResult.hasValidStep()).thenReturn(true);
        when(sweDichotomyResult.getLowestInvalidStep()).thenReturn(lowestInvalidStep);
        when(lowestInvalidStep.getRaoResult()).thenReturn(raoResult);
        when(sweData.getCracFrEs()).thenReturn(cracCreationContext);
        when(sweData.getProcessType()).thenReturn(ProcessType.D2CC);
        when(cracCreationContext.getCrac()).thenReturn(crac);
        when(sweData.getNetworkEsFr()).thenReturn(network);
        when(sweData.getNetworkFrEs()).thenReturn(network);
        SweDichotomyResult result = new SweDichotomyResult(DichotomyDirection.ES_FR, sweDichotomyResult, Optional.empty(), null, null, "esFrHighestValidStepUrl.zip", "esFrLowestInvalidStepUrl.zip");
        when(worker.runDichotomyForOneDirection(eq(sweData), eq(onlyFrParameters), any(DichotomyDirection.class), any(OffsetDateTime.class))).thenReturn(CompletableFuture.completedFuture(result));

        dichotomyParallelization.launchDichotomy(sweData, onlyFrParameters, startingTime);

        verify(worker, Mockito.times(1)).runDichotomyForOneDirection(sweData, onlyFrParameters, DichotomyDirection.FR_ES, startingTime);
        verify(worker, Mockito.times(1)).runDichotomyForOneDirection(sweData, onlyFrParameters, DichotomyDirection.ES_FR, startingTime);
        verify(worker, Mockito.times(0)).runDichotomyForOneDirection(sweData, onlyFrParameters, DichotomyDirection.PT_ES, startingTime);
        verify(worker, Mockito.times(0)).runDichotomyForOneDirection(sweData, onlyFrParameters, DichotomyDirection.ES_PT, startingTime);
    }

    @Test
    void testParallelizationWithPtAndEsOnly() {
        SweTaskParameters onlyFrParameters = new TestParametersBuilder()
                .withEsFr(false).withFrEs(false)
                .withEsPt(true).withPtEs(true)
                .build();

        when(dichotomyRunner.run(any(SweData.class), any(SweTaskParameters.class), any(DichotomyDirection.class))).thenReturn(sweDichotomyResult);
        when(outputService.buildAndExportTtcDocument(any(SweData.class), any(ExecutionResult.class))).thenReturn("ttcDocUrl");
        when(cgmesExportService.buildAndExportLastSecureCgmesFiles(any(), any(), any(), any())).thenReturn("ok");
        when(sweDichotomyResult.getHighestValidStep()).thenReturn(highestValidStep);
        when(highestValidStep.getRaoResult()).thenReturn(raoResult);
        when(sweDichotomyResult.hasValidStep()).thenReturn(true);
        when(sweDichotomyResult.getLowestInvalidStep()).thenReturn(lowestInvalidStep);
        when(lowestInvalidStep.getRaoResult()).thenReturn(raoResult);
        when(sweData.getCracFrEs()).thenReturn(cracCreationContext);
        when(sweData.getProcessType()).thenReturn(ProcessType.D2CC);
        when(cracCreationContext.getCrac()).thenReturn(crac);
        when(sweData.getNetworkEsFr()).thenReturn(network);
        when(sweData.getNetworkFrEs()).thenReturn(network);
        SweDichotomyResult result = new SweDichotomyResult(DichotomyDirection.ES_FR, sweDichotomyResult, Optional.empty(), null, null, "esFrHighestValidStepUrl.zip", "esFrLowestInvalidStepUrl.zip");
        when(worker.runDichotomyForOneDirection(eq(sweData), eq(onlyFrParameters), any(DichotomyDirection.class), any(OffsetDateTime.class))).thenReturn(CompletableFuture.completedFuture(result));

        dichotomyParallelization.launchDichotomy(sweData, onlyFrParameters, startingTime);

        verify(worker, Mockito.times(0)).runDichotomyForOneDirection(sweData, onlyFrParameters, DichotomyDirection.FR_ES, startingTime);
        verify(worker, Mockito.times(0)).runDichotomyForOneDirection(sweData, onlyFrParameters, DichotomyDirection.ES_FR, startingTime);
        verify(worker, Mockito.times(1)).runDichotomyForOneDirection(sweData, onlyFrParameters, DichotomyDirection.PT_ES, startingTime);
        verify(worker, Mockito.times(1)).runDichotomyForOneDirection(sweData, onlyFrParameters, DichotomyDirection.ES_PT, startingTime);
    }

    private static class TestParametersBuilder {
        private final List<TaskParameterDto> parameters = new ArrayList<>();

        public TestParametersBuilder withEsFr(boolean value) {
            parameters.add(new TaskParameterDto("RUN_ES-FR", "BOOLEAN", Boolean.toString(value), "true"));
            return this;
        }

        public TestParametersBuilder withFrEs(boolean value) {
            parameters.add(new TaskParameterDto("RUN_FR-ES", "BOOLEAN", Boolean.toString(value), "true"));
            return this;
        }

        public TestParametersBuilder withEsPt(boolean value) {
            parameters.add(new TaskParameterDto("RUN_ES-PT", "BOOLEAN", Boolean.toString(value), "true"));
            return this;
        }

        public TestParametersBuilder withPtEs(boolean value) {
            parameters.add(new TaskParameterDto("RUN_PT-ES", "BOOLEAN", Boolean.toString(value), "true"));
            return this;
        }

        public SweTaskParameters build() {
            return new SweTaskParameters(this.parameters);
        }
    }

    @Test
    void testParallelizationWithErrorInDichotomy() throws ExecutionException, InterruptedException {
        Future futureMock = mock(Future.class);
        when(worker.runDichotomyForOneDirection(any(), any(), any(), any())).thenReturn(futureMock);
        when(futureMock.get()).thenThrow(ExecutionException.class);
        assertThrows(SweInternalException.class, () -> dichotomyParallelization.launchDichotomy(sweData, defaultParameters, startingTime));
    }

    @Test
    void testParallelizationWithInterruption() {
        DichotomyResult<SweDichotomyValidationData> interruptedSweDichotomyResult = mock(DichotomyResult.class);
        when(interruptedSweDichotomyResult.isInterrupted()).thenReturn(true);

        when(dichotomyRunner.run(any(SweData.class), any(SweTaskParameters.class), any(DichotomyDirection.class))).thenReturn(sweDichotomyResult);
        when(cgmesExportService.buildAndExportLastSecureCgmesFiles(any(), any(), any(), any())).thenReturn("ok");
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
        SweDichotomyResult result = new SweDichotomyResult(DichotomyDirection.ES_FR, sweDichotomyResult, Optional.empty(), null, null, "esFrHighestValidStepUrl.zip", "esFrLowestInvalidStepUrl.zip");
        SweDichotomyResult interruptedResult = new SweDichotomyResult(DichotomyDirection.ES_FR, interruptedSweDichotomyResult, Optional.empty(), null, null, "esFrHighestValidStepUrl.zip", "esFrLowestInvalidStepUrl.zip");
        when(worker.runDichotomyForOneDirection(sweData, defaultParameters, DichotomyDirection.ES_FR, startingTime)).thenReturn(CompletableFuture.completedFuture(result));
        when(worker.runDichotomyForOneDirection(sweData, defaultParameters, DichotomyDirection.FR_ES, startingTime)).thenReturn(CompletableFuture.completedFuture(result));
        when(worker.runDichotomyForOneDirection(sweData, defaultParameters, DichotomyDirection.ES_PT, startingTime)).thenReturn(CompletableFuture.completedFuture(interruptedResult));
        when(worker.runDichotomyForOneDirection(sweData, defaultParameters, DichotomyDirection.PT_ES, startingTime)).thenReturn(CompletableFuture.completedFuture(interruptedResult));
        SweResponse sweResponse = dichotomyParallelization.launchDichotomy(sweData, defaultParameters, startingTime);
        assertEquals("", sweResponse.getTtcDocUrl());
        assertTrue(sweResponse.isInterrupted());
    }

    @Test
    void testParallelizationWithOneRaoFailure() {
        DichotomyResult<SweDichotomyValidationData> failedSweDichotomyResult = DichotomyResult.buildFromRaoFailure("My test failure");
        SweDichotomyResult failedResult = new SweDichotomyResult(DichotomyDirection.ES_PT, failedSweDichotomyResult, "cneFirstUnsecureUrl");

        when(dichotomyRunner.run(any(SweData.class), any(SweTaskParameters.class), any(DichotomyDirection.class))).thenReturn(sweDichotomyResult);
        when(outputService.buildAndExportTtcDocument(any(SweData.class), any(ExecutionResult.class))).thenReturn("ttcDocUrl");
        when(cgmesExportService.buildAndExportLastSecureCgmesFiles(any(), any(), any(), any())).thenReturn("ok");
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
        SweDichotomyResult result = new SweDichotomyResult(DichotomyDirection.ES_FR, sweDichotomyResult, Optional.empty(), null, null, "esFrHighestValidStepUrl.zip", "esFrLowestInvalidStepUrl.zip");
        when(worker.runDichotomyForOneDirection(sweData, defaultParameters, DichotomyDirection.ES_FR, startingTime)).thenReturn(CompletableFuture.completedFuture(result));
        when(worker.runDichotomyForOneDirection(sweData, defaultParameters, DichotomyDirection.FR_ES, startingTime)).thenReturn(CompletableFuture.completedFuture(result));
        when(worker.runDichotomyForOneDirection(sweData, defaultParameters, DichotomyDirection.ES_PT, startingTime)).thenReturn(CompletableFuture.completedFuture(failedResult));
        when(worker.runDichotomyForOneDirection(sweData, defaultParameters, DichotomyDirection.PT_ES, startingTime)).thenReturn(CompletableFuture.completedFuture(failedResult));
        SweResponse sweResponse = dichotomyParallelization.launchDichotomy(sweData, defaultParameters, startingTime);
        assertEquals("ttcDocUrl", sweResponse.getTtcDocUrl());
        assertFalse(sweResponse.isAllRaoFailed());
    }

    @Test
    void testParallelizationWithTwoDirectionsAllRaoFailure() {
        DichotomyResult<SweDichotomyValidationData> failedSweDichotomyResult = DichotomyResult.buildFromRaoFailure("My test failure");
        SweDichotomyResult failedResult = new SweDichotomyResult(DichotomyDirection.ES_PT, failedSweDichotomyResult, "cneFirstUnsecureUrl");

        when(dichotomyRunner.run(any(SweData.class), any(SweTaskParameters.class), any(DichotomyDirection.class))).thenReturn(sweDichotomyResult);
        when(outputService.buildAndExportTtcDocument(any(SweData.class), any(ExecutionResult.class))).thenReturn("ttcDocUrl");
        when(cgmesExportService.buildAndExportLastSecureCgmesFiles(any(), any(), any(), any())).thenReturn("ok");
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
        final SweTaskParameters parameters = new TestParametersBuilder()
                .withEsFr(true)
                .withEsPt(true)
                .withFrEs(false)
                .withPtEs(false)
                .build();
        when(worker.runDichotomyForOneDirection(sweData, parameters, DichotomyDirection.ES_FR, startingTime)).thenReturn(CompletableFuture.completedFuture(failedResult));
        when(worker.runDichotomyForOneDirection(sweData, parameters, DichotomyDirection.ES_PT, startingTime)).thenReturn(CompletableFuture.completedFuture(failedResult));
        SweResponse sweResponse = dichotomyParallelization.launchDichotomy(sweData, parameters, startingTime);
        assertTrue(sweResponse.isAllRaoFailed());
    }

    @Test
    void testParallelizationWithFourDirectionsAllRaoFailure() {
        DichotomyResult<SweDichotomyValidationData> failedSweDichotomyResult = DichotomyResult.buildFromRaoFailure("My test failure");
        SweDichotomyResult failedResult = new SweDichotomyResult(DichotomyDirection.ES_PT, failedSweDichotomyResult, "cneFirstUnsecureUrl");

        when(dichotomyRunner.run(any(SweData.class), any(SweTaskParameters.class), any(DichotomyDirection.class))).thenReturn(sweDichotomyResult);
        when(outputService.buildAndExportTtcDocument(any(SweData.class), any(ExecutionResult.class))).thenReturn("ttcDocUrl");
        when(cgmesExportService.buildAndExportLastSecureCgmesFiles(any(), any(), any(), any())).thenReturn("ok");
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
        when(worker.runDichotomyForOneDirection(sweData, defaultParameters, DichotomyDirection.ES_FR, startingTime)).thenReturn(CompletableFuture.completedFuture(failedResult));
        when(worker.runDichotomyForOneDirection(sweData, defaultParameters, DichotomyDirection.FR_ES, startingTime)).thenReturn(CompletableFuture.completedFuture(failedResult));
        when(worker.runDichotomyForOneDirection(sweData, defaultParameters, DichotomyDirection.ES_PT, startingTime)).thenReturn(CompletableFuture.completedFuture(failedResult));
        when(worker.runDichotomyForOneDirection(sweData, defaultParameters, DichotomyDirection.PT_ES, startingTime)).thenReturn(CompletableFuture.completedFuture(failedResult));
        SweResponse sweResponse = dichotomyParallelization.launchDichotomy(sweData, defaultParameters, startingTime);
        assertTrue(sweResponse.isAllRaoFailed());
    }
}

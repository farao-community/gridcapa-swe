/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
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
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.swe.runner.api.resource.ProcessType;
import com.farao_community.farao.swe.runner.api.resource.SweResponse;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyResult;
import com.farao_community.farao.swe.runner.app.parallelization.DichotomyParallelizationWorker;
import com.farao_community.farao.swe.runner.app.parallelization.ExecutionResult;
import com.farao_community.farao.swe.runner.app.services.CgmesExportService;
import com.farao_community.farao.swe.runner.app.services.CneFileExportService;
import com.farao_community.farao.swe.runner.app.services.OutputService;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
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
    private DichotomyResult<RaoResponse> sweDichotomyResult;

    @Mock
    private DichotomyStepResult<RaoResponse> highestValidStep;

    @Mock
    private DichotomyStepResult<RaoResponse> lowestInvalidStep;

    @Mock
    private RaoResult raoResult;

    @Mock
    private ExecutionResult<SweDichotomyResult> executionResult;

    @Mock
    private CimCracCreationContext cracCreationContext;

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
        when(sweData.getProcessType()).thenReturn(ProcessType.D2CC);
        when(cracCreationContext.getCrac()).thenReturn(crac);
        when(sweData.getNetworkEsFr()).thenReturn(network);
        when(sweData.getNetworkEsPt()).thenReturn(network);
        when(sweData.getNetworkFrEs()).thenReturn(network);
        when(sweData.getNetworkPtEs()).thenReturn(network);
        SweDichotomyResult result = new SweDichotomyResult(DichotomyDirection.ES_FR, sweDichotomyResult, Optional.empty(), null, "esFrHighestValidStepUrl.zip", "esFrLowestInvalidStepUrl.zip");
        when(worker.runDichotomyForOneDirection(sweData, DichotomyDirection.ES_FR)).thenReturn(CompletableFuture.completedFuture(result));
        when(worker.runDichotomyForOneDirection(sweData, DichotomyDirection.FR_ES)).thenReturn(CompletableFuture.completedFuture(result));
        when(worker.runDichotomyForOneDirection(sweData, DichotomyDirection.ES_PT)).thenReturn(CompletableFuture.completedFuture(result));
        when(worker.runDichotomyForOneDirection(sweData, DichotomyDirection.PT_ES)).thenReturn(CompletableFuture.completedFuture(result));
        SweResponse sweResponse = dichotomyParallelization.launchDichotomy(sweData);
        assertEquals("ttcDocUrl", sweResponse.getTtcDocUrl());
        assertEquals("esFrHighestValidStepUrl.zip", sweResponse.getEsFrHighestValidStepUrl());
        assertEquals("esFrLowestInvalidStepUrl.zip", sweResponse.getEsFrLowestInvalidStepUrl());
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
        when(worker.runDichotomyForOneDirection(sweData, DichotomyDirection.ES_FR)).thenReturn(CompletableFuture.completedFuture(result));
        when(worker.runDichotomyForOneDirection(sweData, DichotomyDirection.FR_ES)).thenReturn(CompletableFuture.completedFuture(result));
        when(worker.runDichotomyForOneDirection(sweData, DichotomyDirection.ES_PT)).thenReturn(CompletableFuture.completedFuture(result));
        when(worker.runDichotomyForOneDirection(sweData, DichotomyDirection.PT_ES)).thenReturn(CompletableFuture.completedFuture(result));
        SweResponse sweResponse = dichotomyParallelization.launchDichotomy(sweData);
        assertEquals("ttcDocUrl", sweResponse.getTtcDocUrl());
        assertNull(sweResponse.getEsFrHighestValidStepUrl());
        assertEquals("esFrLowestInvalidStepUrl.zip", sweResponse.getEsFrLowestInvalidStepUrl());
    }
}

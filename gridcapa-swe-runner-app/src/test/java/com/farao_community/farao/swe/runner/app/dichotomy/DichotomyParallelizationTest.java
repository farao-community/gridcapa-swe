/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy;

import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.swe.runner.api.resource.SweResponse;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.parallelization.ExecutionResult;
import com.farao_community.farao.swe.runner.app.parallelization.ParallelExecution;
import com.farao_community.farao.swe.runner.app.services.OutputService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Mock
    private ParallelExecution parallelExecution;

    @Mock
    private SweData sweData;

    @Mock
    private DichotomyResult<RaoResponse> sweDichotomyResult;

    @Mock
    private ExecutionResult executionResult;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testParallelization() {
        when(dichotomyRunner.run(any(SweData.class), any(DichotomyDirection.class))).thenReturn(sweDichotomyResult);
        when(outputService.buildAndExportTtcDocument(any(SweData.class), any(ExecutionResult.class))).thenReturn("ttcDocUrl");
        when(parallelExecution.close()).thenReturn(executionResult);
        SweResponse sweResponse = dichotomyParallelization.launchDichotomy(sweData);
        assertEquals("ttcDocUrl", sweResponse.getTtcDocUrl());
    }
}

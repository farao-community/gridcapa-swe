/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.gridcapa.task_manager.api.TaskParameterDto;
import com.farao_community.farao.swe.runner.api.resource.SweRequest;
import com.farao_community.farao.swe.runner.api.resource.SweResponse;
import com.farao_community.farao.swe.runner.app.dichotomy.DichotomyParallelization;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweTaskParameters;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@SpringBootTest
class SweRunnerTest {

    @Autowired
    private SweRunner sweRunner;

    @MockBean
    private DichotomyParallelization dichotomyParallelization;

    @MockBean
    private FilesService filesService;

    @MockBean
    private Logger businessLogger;

    @Test
    void run() {
        when(filesService.importFiles(any(SweRequest.class), any(SweTaskParameters.class))).thenReturn(mock(SweData.class));
        when(dichotomyParallelization.launchDichotomy(any(SweData.class), any(SweTaskParameters.class))).thenReturn(new SweResponse("id", "ttcUrl", false));
        SweResponse sweResponse = sweRunner.run(mock(SweRequest.class));
        assertNotNull(sweResponse);
        assertEquals("ttcUrl", sweResponse.getTtcDocUrl());
    }

    @Test
    void logNotModifiedParameters() {
        when(filesService.importFiles(any(SweRequest.class), any(SweTaskParameters.class))).thenReturn(mock(SweData.class));
        when(dichotomyParallelization.launchDichotomy(any(SweData.class), any(SweTaskParameters.class))).thenReturn(new SweResponse("id", "ttcUrl", false));
        SweRequest sweRequest = mock(SweRequest.class);
        Mockito.when(sweRequest.getTaskParameterList()).thenReturn(List.of(new TaskParameterDto("MAX_CRA", "INT", "35", "35")));

        sweRunner.run(sweRequest);

        Mockito.verify(businessLogger, times(1)).info(anyString(), anyString());
        Mockito.verify(businessLogger, times(0)).warn(anyString(), anyString());
    }

    @Test
    void logModifiedParameters() {
        when(filesService.importFiles(any(SweRequest.class), any(SweTaskParameters.class))).thenReturn(mock(SweData.class));
        when(dichotomyParallelization.launchDichotomy(any(SweData.class), any(SweTaskParameters.class))).thenReturn(new SweResponse("id", "ttcUrl", false));
        SweRequest sweRequest = mock(SweRequest.class);
        Mockito.when(sweRequest.getTaskParameterList()).thenReturn(List.of(new TaskParameterDto("MAX_CRA", "INT", "17", "35")));

        sweRunner.run(sweRequest);

        Mockito.verify(businessLogger, times(0)).info(anyString(), anyString());
        Mockito.verify(businessLogger, times(1)).warn(anyString(), anyString());
    }
}

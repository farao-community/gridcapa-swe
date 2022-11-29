/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.swe.runner.api.resource.SweRequest;
import com.farao_community.farao.swe.runner.api.resource.SweResponse;
import com.farao_community.farao.swe.runner.app.dichotomy.DichotomyParallelization;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
class SweRunnerTest {

    @Autowired
    private SweRunner sweRunner;

    @MockBean
    private DichotomyParallelization dichotomyParallelization;

    @MockBean
    private FilesService filesService;

    @Test
    void run() {
        when(filesService.importFiles(any(SweRequest.class))).thenReturn(mock(SweData.class));
        when(dichotomyParallelization.launchDichotomy(any(SweData.class))).thenReturn(new SweResponse("id", "ttcDocUrl",
                "esFrVoltageZipUrl", "frEsVoltageZipUrl",
                "esFrCgmesZipUrl", "frEsCgmesZipUrl",
                "esPtCgmesZipUrl", "ptEsCgmesZipUrl",
                "esFrHighestValidStepUrl", "esFrLowestInvalidStepUrl",
                "frEsHighestValidStepUrl", "frEsLowestInvalidStepUrl",
                "esPtHighestValidStepUrl", "esPtLowestInvalidStepUrl",
                "ptEsHighestValidStepUrl", "ptEsLowestInvalidStepUrl"));

        SweResponse sweResponse = sweRunner.run(mock(SweRequest.class));
        assertNotNull(sweResponse);
        assertEquals("ttcDocUrl", sweResponse.getTtcDocUrl());
        assertEquals("esFrVoltageZipUrl", sweResponse.getEsFrVoltageZipUrl());
        assertEquals("frEsVoltageZipUrl", sweResponse.getFrEsVoltageZipUrl());
        assertEquals("esFrCgmesZipUrl", sweResponse.getEsFrCgmesZipUrl());
        assertEquals("frEsCgmesZipUrl", sweResponse.getFrEsCgmesZipUrl());
        assertEquals("esFrHighestValidStepUrl", sweResponse.getEsFrHighestValidStepUrl());
        assertEquals("esFrLowestInvalidStepUrl", sweResponse.getEsFrLowestInvalidStepUrl());
        assertEquals("frEsHighestValidStepUrl", sweResponse.getFrEsHighestValidStepUrl());
        assertEquals("frEsLowestInvalidStepUrl", sweResponse.getFrEsLowestInvalidStepUrl());
    }
}

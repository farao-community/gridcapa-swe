/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy;

import com.farao_community.farao.dichotomy.api.DichotomyEngine;
import com.farao_community.farao.dichotomy.api.NetworkShifter;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.swe.runner.app.configurations.DichotomyConfiguration.Parameters;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.services.FileExporter;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@SpringBootTest
class DichotomyRunnerTest {

    @Autowired
    private DichotomyRunner dichotomyRunner;

    @MockBean
    private NetworkShifterProvider networkShifterProvider;

    @MockBean
    private FileExporter fileExporter;

    @Mock
    private NetworkShifter networkShifter;

    @Mock
    private SweData sweData;

    @Mock
    private Parameters parameters;

    @Test
    void testBuildDichotomyEngine() throws IOException {
        when(networkShifterProvider.get(any(SweData.class), any(DichotomyDirection.class))).thenReturn(networkShifter);
        when(fileExporter.saveRaoParameters(sweData)).thenReturn("raoParameters.json");
        DichotomyEngine<RaoResponse> engine = dichotomyRunner.buildDichotomyEngine(sweData, DichotomyDirection.ES_FR, parameters);
        assertNotNull(engine);
    }
}

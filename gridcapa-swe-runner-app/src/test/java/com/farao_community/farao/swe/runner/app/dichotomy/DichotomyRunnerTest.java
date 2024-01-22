/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy;

import com.farao_community.farao.dichotomy.api.DichotomyEngine;
import com.farao_community.farao.dichotomy.api.NetworkShifter;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import com.farao_community.farao.gridcapa_swe_commons.resource.ProcessType;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.swe.runner.app.SweTaskParametersTestUtil;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyValidationData;
import com.farao_community.farao.swe.runner.app.domain.SweTaskParameters;
import com.farao_community.farao.swe.runner.app.services.FileExporter;
import com.farao_community.farao.swe.runner.app.services.NetworkService;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private DichotomyParameters parameters;

    @Test
    void testBuildDichotomyEngine() {
        when(networkShifterProvider.get(any(SweData.class), any(DichotomyDirection.class))).thenReturn(networkShifter);
        when(fileExporter.saveRaoParameters(eq(OffsetDateTime.now()), eq(ProcessType.D2CC), any(SweTaskParameters.class), eq(DichotomyDirection.ES_FR)))
            .thenReturn("raoParameters.json");
        DichotomyEngine<SweDichotomyValidationData> engine = dichotomyRunner.buildDichotomyEngine(sweData, DichotomyDirection.ES_FR, parameters);
        assertNotNull(engine);
    }

    @Test
    void runDichotomyEsFrTest() {
        Network network = Mockito.mock(Network.class);
        DichotomyResult<RaoResponse> mockDichotomyResult = Mockito.mock(DichotomyResult.class);
        Mockito.when(NetworkService.getNetworkByDirection(sweData, DichotomyDirection.ES_FR)).thenReturn(network);
        DichotomyEngine<RaoResponse> mockEngine = Mockito.mock(DichotomyEngine.class);
        DichotomyRunner spyDichotomyRunner = Mockito.spy(dichotomyRunner);
        ArgumentCaptor<DichotomyParameters> argumentCaptor = ArgumentCaptor.forClass(DichotomyParameters.class);
        Mockito.doReturn(mockEngine).when(spyDichotomyRunner).buildDichotomyEngine(Mockito.any(SweData.class), Mockito.any(DichotomyDirection.class), argumentCaptor.capture());
        Mockito.when(mockEngine.run(Mockito.any(Network.class))).thenReturn(mockDichotomyResult);
        SweTaskParameters sweTaskParameters = SweTaskParametersTestUtil.getSweTaskParameters();
        DichotomyResult<SweDichotomyValidationData> dichotomyResult = spyDichotomyRunner.run(sweData, sweTaskParameters, DichotomyDirection.ES_FR);
        assertEquals(mockDichotomyResult, dichotomyResult);
        DichotomyParameters captorValue = argumentCaptor.getValue();
        assertNotNull(captorValue);
        assertEquals(42, captorValue.getMaxValue());
        assertEquals(82, captorValue.getMinValue());
        assertEquals(12, captorValue.getPrecision());
        assertTrue(captorValue.isRunAngleCheck());
    }

    @Test
    void runDichotomyFrEsTest() {
        Network network = Mockito.mock(Network.class);
        DichotomyResult<RaoResponse> mockDichotomyResult = Mockito.mock(DichotomyResult.class);
        Mockito.when(NetworkService.getNetworkByDirection(sweData, DichotomyDirection.FR_ES)).thenReturn(network);
        DichotomyEngine<RaoResponse> mockEngine = Mockito.mock(DichotomyEngine.class);
        DichotomyRunner spyDichotomyRunner = Mockito.spy(dichotomyRunner);
        ArgumentCaptor<DichotomyParameters> argumentCaptor = ArgumentCaptor.forClass(DichotomyParameters.class);
        Mockito.doReturn(mockEngine).when(spyDichotomyRunner).buildDichotomyEngine(Mockito.any(SweData.class), Mockito.any(DichotomyDirection.class), argumentCaptor.capture());
        Mockito.when(mockEngine.run(Mockito.any(Network.class))).thenReturn(mockDichotomyResult);
        SweTaskParameters sweTaskParameters = SweTaskParametersTestUtil.getSweTaskParameters();
        DichotomyResult<SweDichotomyValidationData> dichotomyResult = spyDichotomyRunner.run(sweData, sweTaskParameters, DichotomyDirection.FR_ES);
        assertEquals(mockDichotomyResult, dichotomyResult);
        DichotomyParameters captorValue = argumentCaptor.getValue();
        assertNotNull(captorValue);
        assertEquals(43, captorValue.getMaxValue());
        assertEquals(83, captorValue.getMinValue());
        assertEquals(13, captorValue.getPrecision());
        assertTrue(captorValue.isRunAngleCheck());
    }

    @Test
    void runDichotomyEsPtTest() {
        Network network = Mockito.mock(Network.class);
        DichotomyResult<RaoResponse> mockDichotomyResult = Mockito.mock(DichotomyResult.class);
        Mockito.when(NetworkService.getNetworkByDirection(sweData, DichotomyDirection.ES_PT)).thenReturn(network);
        DichotomyEngine<RaoResponse> mockEngine = Mockito.mock(DichotomyEngine.class);
        DichotomyRunner spyDichotomyRunner = Mockito.spy(dichotomyRunner);
        ArgumentCaptor<DichotomyParameters> argumentCaptor = ArgumentCaptor.forClass(DichotomyParameters.class);
        Mockito.doReturn(mockEngine).when(spyDichotomyRunner).buildDichotomyEngine(Mockito.any(SweData.class), Mockito.any(DichotomyDirection.class), argumentCaptor.capture());
        Mockito.when(mockEngine.run(Mockito.any(Network.class))).thenReturn(mockDichotomyResult);
        SweTaskParameters sweTaskParameters = SweTaskParametersTestUtil.getSweTaskParameters();
        DichotomyResult<SweDichotomyValidationData> dichotomyResult = spyDichotomyRunner.run(sweData, sweTaskParameters, DichotomyDirection.ES_PT);
        assertEquals(mockDichotomyResult, dichotomyResult);
        DichotomyParameters captorValue = argumentCaptor.getValue();
        assertNotNull(captorValue);
        assertEquals(44, captorValue.getMaxValue());
        assertEquals(84, captorValue.getMinValue());
        assertEquals(14, captorValue.getPrecision());
        assertTrue(captorValue.isRunAngleCheck());
    }

    @Test
    void runDichotomyPtEsTest() {
        Network network = Mockito.mock(Network.class);
        DichotomyResult<RaoResponse> mockDichotomyResult = Mockito.mock(DichotomyResult.class);
        Mockito.when(NetworkService.getNetworkByDirection(sweData, DichotomyDirection.PT_ES)).thenReturn(network);
        DichotomyEngine<RaoResponse> mockEngine = Mockito.mock(DichotomyEngine.class);
        DichotomyRunner spyDichotomyRunner = Mockito.spy(dichotomyRunner);
        ArgumentCaptor<DichotomyParameters> argumentCaptor = ArgumentCaptor.forClass(DichotomyParameters.class);
        Mockito.doReturn(mockEngine).when(spyDichotomyRunner).buildDichotomyEngine(Mockito.any(SweData.class), Mockito.any(DichotomyDirection.class), argumentCaptor.capture());
        Mockito.when(mockEngine.run(Mockito.any(Network.class))).thenReturn(mockDichotomyResult);
        SweTaskParameters sweTaskParameters = SweTaskParametersTestUtil.getSweTaskParameters();
        DichotomyResult<SweDichotomyValidationData> dichotomyResult = spyDichotomyRunner.run(sweData, sweTaskParameters, DichotomyDirection.PT_ES);
        assertEquals(mockDichotomyResult, dichotomyResult);
        DichotomyParameters captorValue = argumentCaptor.getValue();
        assertNotNull(captorValue);
        assertEquals(45, captorValue.getMaxValue());
        assertEquals(85, captorValue.getMinValue());
        assertEquals(15, captorValue.getPrecision());
        assertTrue(captorValue.isRunAngleCheck());
    }
}

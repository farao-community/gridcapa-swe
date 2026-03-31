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
import com.farao_community.farao.rao_runner.api.resource.AbstractRaoResponse;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweTaskParameters;
import com.farao_community.farao.swe.runner.app.services.FileExporter;
import com.farao_community.farao.swe.runner.app.services.NetworkService;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openloadflow.OpenLoadFlowParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.OffsetDateTime;

import static com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection.ES_FR;
import static com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection.ES_PT;
import static com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection.FR_ES;
import static com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection.PT_ES;
import static com.farao_community.farao.gridcapa_swe_commons.resource.ProcessType.D2CC;
import static com.farao_community.farao.swe.runner.app.SweTaskParametersTestUtil.getSweTaskParameters;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@SpringBootTest
class DichotomyRunnerTest {

    @Autowired
    private DichotomyRunner dichotomyRunner;

    @MockitoBean
    private NetworkShifterProvider networkShifterProvider;

    @MockitoBean
    private FileExporter fileExporter;

    @Mock
    private NetworkShifter networkShifter;

    @Mock
    private SweData sweData;

    @Mock
    private DichotomyParameters dichotomyParameters;
    @Mock
    private LoadFlowParameters loadFlowParameters;

    Network network = mock(Network.class);
    DichotomyResult<AbstractRaoResponse> mockDichotomyResult = mock(DichotomyResult.class);
    DichotomyEngine<AbstractRaoResponse> mockEngine = mock(DichotomyEngine.class);
    DichotomyRunner spyDichotomyRunner;

    @BeforeEach
    void setUp() {
        when(mockEngine.run(any(Network.class)))
            .thenReturn(mockDichotomyResult);
        spyDichotomyRunner = spy(dichotomyRunner);
    }

    @Test
    void testBuildDichotomyEngine() {
        when(networkShifterProvider.get(anySweData(), anyDirection(), any(LoadFlowParameters.class), anyBoolean()))
            .thenReturn(networkShifter);

        when(fileExporter.saveRaoParameters(eq(OffsetDateTime.now()), eq(D2CC), any(SweTaskParameters.class), eq(ES_FR)))
            .thenReturn("raoParameters.json");

        assertNotNull(dichotomyRunner.buildDichotomyEngine(sweData, ES_FR, dichotomyParameters, loadFlowParameters));
    }

    @Test
    void runDichotomyEsFrTest() {
        when(NetworkService.getNetworkByDirection(sweData, ES_FR))
            .thenReturn(network);

        final ArgumentCaptor<DichotomyParameters> dichotomyParametersCaptor = ArgumentCaptor.forClass(DichotomyParameters.class);
        final ArgumentCaptor<LoadFlowParameters> loadFlowParametersCaptor = ArgumentCaptor.forClass(LoadFlowParameters.class);

        doReturn(mockEngine)
            .when(spyDichotomyRunner)
            .buildDichotomyEngine(anySweData(), anyDirection(), dichotomyParametersCaptor.capture(), loadFlowParametersCaptor.capture());

        assertEquals(mockDichotomyResult, spyDichotomyRunner.run(sweData, getSweTaskParameters(), ES_FR));

        final DichotomyParameters dichotomyParametersCaptorValue = dichotomyParametersCaptor.getValue();
        assertNotNull(dichotomyParametersCaptorValue);
        assertEquals(82, dichotomyParametersCaptorValue.maxValue());
        assertEquals(42, dichotomyParametersCaptorValue.minValue());
        assertEquals(12, dichotomyParametersCaptorValue.precision());
        assertTrue(dichotomyParametersCaptorValue.runAngleCheck());

        final LoadFlowParameters loadFlowParametersCaptorValue = loadFlowParametersCaptor.getValue();
        assertNotNull(loadFlowParametersCaptorValue);
        assert5MaxNewtonRaphsonIterations(loadFlowParametersCaptorValue);
    }

    @Test
    void runDichotomyFrEsTest() {
        when(NetworkService.getNetworkByDirection(sweData, FR_ES))
            .thenReturn(network);

        final ArgumentCaptor<DichotomyParameters> dichotomyParametersCaptor = ArgumentCaptor.forClass(DichotomyParameters.class);
        final ArgumentCaptor<LoadFlowParameters> loadFlowParametersCaptor = ArgumentCaptor.forClass(LoadFlowParameters.class);

        doReturn(mockEngine)
            .when(spyDichotomyRunner)
            .buildDichotomyEngine(anySweData(), anyDirection(), dichotomyParametersCaptor.capture(), loadFlowParametersCaptor.capture());

        assertEquals(mockDichotomyResult, spyDichotomyRunner.run(sweData, getSweTaskParameters(), FR_ES));

        final DichotomyParameters dichotomyParametersCaptorValue = dichotomyParametersCaptor.getValue();
        assertNotNull(dichotomyParametersCaptorValue);
        assertEquals(83, dichotomyParametersCaptorValue.maxValue());
        assertEquals(43, dichotomyParametersCaptorValue.minValue());
        assertEquals(13, dichotomyParametersCaptorValue.precision());
        assertTrue(dichotomyParametersCaptorValue.runAngleCheck());

        final LoadFlowParameters loadFlowParametersCaptorValue = loadFlowParametersCaptor.getValue();
        assertNotNull(loadFlowParametersCaptorValue);
        assert5MaxNewtonRaphsonIterations(loadFlowParametersCaptorValue);
    }

    @Test
    void runDichotomyEsPtTest() {
        when(NetworkService.getNetworkByDirection(sweData, ES_PT))
            .thenReturn(network);

        final ArgumentCaptor<DichotomyParameters> dichotomyParametersCaptor = ArgumentCaptor.forClass(DichotomyParameters.class);
        final ArgumentCaptor<LoadFlowParameters> loadFlowParametersCaptor = ArgumentCaptor.forClass(LoadFlowParameters.class);

        doReturn(mockEngine)
            .when(spyDichotomyRunner)
            .buildDichotomyEngine(anySweData(), anyDirection(), dichotomyParametersCaptor.capture(), loadFlowParametersCaptor.capture());

        assertEquals(mockDichotomyResult, spyDichotomyRunner.run(sweData, getSweTaskParameters(), ES_PT));

        final DichotomyParameters dichotomyParametersCaptorValue = dichotomyParametersCaptor.getValue();
        assertNotNull(dichotomyParametersCaptorValue);
        assertEquals(84, dichotomyParametersCaptorValue.maxValue());
        assertEquals(44, dichotomyParametersCaptorValue.minValue());
        assertEquals(14, dichotomyParametersCaptorValue.precision());
        assertTrue(dichotomyParametersCaptorValue.runAngleCheck());

        final LoadFlowParameters loadFlowParametersCaptorValue = loadFlowParametersCaptor.getValue();
        assertNotNull(loadFlowParametersCaptorValue);
        assert5MaxNewtonRaphsonIterations(loadFlowParametersCaptorValue);
    }

    @Test
    void runDichotomyPtEsTest() {
        when(NetworkService.getNetworkByDirection(sweData, PT_ES))
            .thenReturn(network);
        final ArgumentCaptor<DichotomyParameters> dichotomyParametersCaptor = ArgumentCaptor.forClass(DichotomyParameters.class);
        final ArgumentCaptor<LoadFlowParameters> loadFlowParametersCaptor = ArgumentCaptor.forClass(LoadFlowParameters.class);

        doReturn(mockEngine)
            .when(spyDichotomyRunner).buildDichotomyEngine(anySweData(), anyDirection(),
                                                           dichotomyParametersCaptor.capture(), loadFlowParametersCaptor.capture());

        assertEquals(mockDichotomyResult, spyDichotomyRunner.run(sweData, getSweTaskParameters(), PT_ES));

        final DichotomyParameters dichotomyParametersCaptorValue = dichotomyParametersCaptor.getValue();
        assertNotNull(dichotomyParametersCaptorValue);
        assertEquals(85, dichotomyParametersCaptorValue.maxValue());
        assertEquals(45, dichotomyParametersCaptorValue.minValue());
        assertEquals(15, dichotomyParametersCaptorValue.precision());
        assertTrue(dichotomyParametersCaptorValue.runAngleCheck());

        final LoadFlowParameters loadFlowParametersCaptorValue = loadFlowParametersCaptor.getValue();
        assertNotNull(loadFlowParametersCaptorValue);
        assert5MaxNewtonRaphsonIterations(loadFlowParametersCaptorValue);
    }

    private static SweData anySweData() {
        return any(SweData.class);
    }

    private static DichotomyDirection anyDirection() {
        return any(DichotomyDirection.class);
    }

    private static void assert5MaxNewtonRaphsonIterations(final LoadFlowParameters loadFlowParameters) {
        assertEquals(5, loadFlowParameters.getExtension(OpenLoadFlowParameters.class).getMaxNewtonRaphsonIterations());
    }
}

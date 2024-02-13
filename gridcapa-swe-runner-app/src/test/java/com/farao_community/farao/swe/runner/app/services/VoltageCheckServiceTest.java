/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.craccreation.creator.cim.craccreator.CimCracCreationContext;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.dichotomy.api.results.DichotomyStepResult;
import com.farao_community.farao.gridcapa.task_manager.api.TaskParameterDto;
import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import com.powsybl.openrao.monitoring.voltagemonitoring.VoltageMonitoringResult;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.swe.runner.app.SweTaskParametersTestUtil;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyValidationData;
import com.farao_community.farao.swe.runner.app.domain.SweTaskParameters;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */
@SpringBootTest
class VoltageCheckServiceTest {

    public static final String TEST_URL = "/network/network.xiidm";

    @Autowired
    private VoltageCheckService service;

    private SweTaskParameters sweTaskParameters = SweTaskParametersTestUtil.getSweTaskParameters();

    @Test
    void checkDoesntReturnVoltageCheckIfNotFrESBorder() {
        Optional<VoltageMonitoringResult> result = service.runVoltageCheck(null, null, sweTaskParameters, DichotomyDirection.ES_PT);
        assertTrue(result.isEmpty());
        result = service.runVoltageCheck(null, null, sweTaskParameters, DichotomyDirection.PT_ES);
        assertTrue(result.isEmpty());
    }

    @Test
    void checkDoesntReturnVoltageCheckIfParameterDisabled() {
        SweTaskParameters sweTaskParameters = new SweTaskParameters(List.of(new TaskParameterDto("RUN_VOLTAGE_CHECK", "BOOLEAN", "false", "true")));
        Optional<VoltageMonitoringResult> result = service.runVoltageCheck(null, null, sweTaskParameters, DichotomyDirection.ES_FR);
        assertTrue(result.isEmpty());
    }

    @Test
    void checkReturnsEmptyVoltageCheckIfException() {
        DichotomyResult<SweDichotomyValidationData> dicho = Mockito.mock(DichotomyResult.class);
        Mockito.when(dicho.hasValidStep()).thenReturn(true);
        Optional<VoltageMonitoringResult> result = service.runVoltageCheck(null, dicho, sweTaskParameters, DichotomyDirection.ES_FR);
        assertTrue(result.isEmpty());
        result = service.runVoltageCheck(null, dicho, sweTaskParameters, DichotomyDirection.FR_ES);
        assertTrue(result.isEmpty());
    }

    @Test
    void checkReturnsEmptyVoltageCheckIfNoValidStep() {
        DichotomyResult<SweDichotomyValidationData> dicho = Mockito.mock(DichotomyResult.class);
        Mockito.when(dicho.hasValidStep()).thenReturn(false);
        Optional<VoltageMonitoringResult> result = service.runVoltageCheck(null, dicho, sweTaskParameters, DichotomyDirection.ES_FR);
        assertTrue(result.isEmpty());
        result = service.runVoltageCheck(null, dicho, sweTaskParameters, DichotomyDirection.FR_ES);
        assertTrue(result.isEmpty());
    }

    @Test
    void checkReturnsVoltageCheck() throws URISyntaxException {
        DichotomyResult<SweDichotomyValidationData> dicho = Mockito.mock(DichotomyResult.class);
        DichotomyStepResult<SweDichotomyValidationData> step = Mockito.mock(DichotomyStepResult.class);
        SweDichotomyValidationData data = Mockito.mock(SweDichotomyValidationData.class);
        RaoResponse raoResponse = Mockito.mock(RaoResponse.class);
        InputStream inputStream = Mockito.mock(InputStream.class);
        SweData sweData = Mockito.mock(SweData.class);
        CimCracCreationContext cimCracCreationContext = Mockito.mock(CimCracCreationContext.class);
        Crac crac = Mockito.mock(Crac.class);
        RaoResult raoResult = Mockito.mock(RaoResult.class);
        Mockito.when(sweData.getCracFrEs()).thenReturn(cimCracCreationContext);
        Mockito.when(cimCracCreationContext.getCrac()).thenReturn(crac);
        Mockito.when(dicho.hasValidStep()).thenReturn(true);
        Mockito.when(dicho.getHighestValidStep()).thenReturn(step);
        Mockito.when(step.getValidationData()).thenReturn(data);
        Mockito.when(step.getRaoResult()).thenReturn(raoResult);
        Mockito.when(data.getRaoResponse()).thenReturn(raoResponse);
        Mockito.when(raoResponse.getNetworkWithPraFileUrl()).thenReturn(getClass().getResource(TEST_URL).toURI().toString());
        Optional<VoltageMonitoringResult> result = service.runVoltageCheck(sweData, dicho, sweTaskParameters, DichotomyDirection.ES_FR);
        assertTrue(result.isPresent());
    }

    @Test
    void checkReturnsEmptyVoltageCheckOnExceptionInGetFile() throws URISyntaxException {
        DichotomyResult<SweDichotomyValidationData> dicho = Mockito.mock(DichotomyResult.class);
        DichotomyStepResult<SweDichotomyValidationData> step = Mockito.mock(DichotomyStepResult.class);
        SweDichotomyValidationData data = Mockito.mock(SweDichotomyValidationData.class);
        RaoResponse raoResponse = Mockito.mock(RaoResponse.class);
        InputStream inputStream = Mockito.mock(InputStream.class);
        SweData sweData = Mockito.mock(SweData.class);
        CimCracCreationContext cimCracCreationContext = Mockito.mock(CimCracCreationContext.class);
        Crac crac = Mockito.mock(Crac.class);
        RaoResult raoResult = Mockito.mock(RaoResult.class);
        Mockito.when(sweData.getCracFrEs()).thenReturn(cimCracCreationContext);
        Mockito.when(cimCracCreationContext.getCrac()).thenReturn(crac);
        Mockito.when(dicho.hasValidStep()).thenReturn(true);
        Mockito.when(dicho.getHighestValidStep()).thenReturn(step);
        Mockito.when(step.getValidationData()).thenReturn(data);
        Mockito.when(step.getRaoResult()).thenReturn(raoResult);
        Mockito.when(data.getRaoResponse()).thenReturn(raoResponse);
        Mockito.when(raoResponse.getNetworkWithPraFileUrl()).thenReturn("file:/returnEmpty");
        Optional<VoltageMonitoringResult> result = service.runVoltageCheck(sweData, dicho, sweTaskParameters, DichotomyDirection.ES_FR);
        assertTrue(result.isEmpty());
    }
}

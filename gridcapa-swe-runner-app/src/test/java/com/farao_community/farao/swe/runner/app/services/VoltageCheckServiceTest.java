/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.dichotomy.api.results.DichotomyStepResult;
import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyValidationData;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.NetworkElement;
import com.powsybl.openrao.data.cracapi.cnec.VoltageCnec;
import com.powsybl.openrao.data.craccreation.creator.cim.craccreator.CimCracCreationContext;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.monitoring.voltagemonitoring.VoltageMonitoringResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */
@SpringBootTest
class VoltageCheckServiceTest {

    public static final String TEST_URL = "/network/network.xiidm";

    @Autowired
    private VoltageCheckService service;

    @Test
    void checkDoesntReturnVoltageCheckIfNotFrESBorder() {
        Optional<VoltageMonitoringResult> result = service.runVoltageCheck(null, null, DichotomyDirection.ES_PT);
        assertTrue(result.isEmpty());
        result = service.runVoltageCheck(null, null, DichotomyDirection.PT_ES);
        assertTrue(result.isEmpty());
    }

    @Test
    void checkReturnsEmptyVoltageCheckIfException() {
        DichotomyResult<SweDichotomyValidationData> dicho = Mockito.mock(DichotomyResult.class);
        Mockito.when(dicho.hasValidStep()).thenReturn(true);
        Optional<VoltageMonitoringResult> result = service.runVoltageCheck(null, dicho, DichotomyDirection.ES_FR);
        assertTrue(result.isEmpty());
        result = service.runVoltageCheck(null, dicho, DichotomyDirection.FR_ES);
        assertTrue(result.isEmpty());
    }

    @Test
    void checkReturnsEmptyVoltageCheckIfNoValidStep() {
        DichotomyResult<SweDichotomyValidationData> dicho = Mockito.mock(DichotomyResult.class);
        Mockito.when(dicho.hasValidStep()).thenReturn(false);
        Optional<VoltageMonitoringResult> result = service.runVoltageCheck(null, dicho, DichotomyDirection.ES_FR);
        assertTrue(result.isEmpty());
        result = service.runVoltageCheck(null, dicho, DichotomyDirection.FR_ES);
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
        Optional<VoltageMonitoringResult> result = service.runVoltageCheck(sweData, dicho, DichotomyDirection.ES_FR);
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
        Optional<VoltageMonitoringResult> result = service.runVoltageCheck(sweData, dicho, DichotomyDirection.ES_FR);
        assertTrue(result.isEmpty());
    }

    @Test
    void checkNoLogWhenNoConstraint() {
        //Given
        VoltageMonitoringResult result = Mockito.mock(VoltageMonitoringResult.class);
        Mockito.when(result.getConstrainedElements()).thenReturn(Collections.emptySet());

        //Expect
        assertTrue(service.printHighAndLowVoltageConstraints(result).isEmpty());
    }

    @Test
    void checkLogHighAndLowVoltageConstraintsIfOutsideBounds() {
        //Given
        VoltageMonitoringResult result = Mockito.mock(VoltageMonitoringResult.class);
        VoltageCnec vc = Mockito.mock(VoltageCnec.class);
        Set<VoltageCnec> constrainedElements = Set.of(vc);
        Mockito.when(result.getConstrainedElements()).thenReturn(constrainedElements);
        Mockito.when(result.getMinVoltage(vc)).thenReturn(0d);
        Mockito.when(vc.getLowerBound(Unit.KILOVOLT)).thenReturn(Optional.of(100d));
        Mockito.when(result.getMaxVoltage(vc)).thenReturn(600d);
        Mockito.when(vc.getUpperBound(Unit.KILOVOLT)).thenReturn(Optional.of(500d));
        NetworkElement ne = Mockito.mock(NetworkElement.class);
        Mockito.when(vc.getNetworkElement()).thenReturn(ne);
        Mockito.when(ne.getId()).thenReturn("VL1");
        //When
        final List<String> constraints = service.printHighAndLowVoltageConstraints(result);
        //Then
        assertEquals(2, constraints.size());
        assertEquals("Low Voltage constraint reached due to VL1 0/100 kV", constraints.get(0));
        assertEquals("High Voltage constraint reached due to VL1 600/500 kV", constraints.get(1));
    }

    @Test
    void checkNoLogWhenHighAndLowVoltageConstraintsInBounds() {
        //Given
        VoltageMonitoringResult result = Mockito.mock(VoltageMonitoringResult.class);
        VoltageCnec vc = Mockito.mock(VoltageCnec.class);
        Set<VoltageCnec> constrainedElements = Set.of(vc);
        Mockito.when(result.getConstrainedElements()).thenReturn(constrainedElements);
        Mockito.when(result.getMaxVoltage(vc)).thenReturn(600d);
        Mockito.when(vc.getUpperBound(Unit.KILOVOLT)).thenReturn(Optional.of(700d));
        Mockito.when(result.getMinVoltage(vc)).thenReturn(200d);
        Mockito.when(vc.getLowerBound(Unit.KILOVOLT)).thenReturn(Optional.of(100d));
        //Expect
        assertTrue(service.printHighAndLowVoltageConstraints(result).isEmpty());
    }
}

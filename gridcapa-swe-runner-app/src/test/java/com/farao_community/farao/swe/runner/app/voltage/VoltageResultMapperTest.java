/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.voltage;

import com.farao_community.farao.swe.runner.app.voltage.json.VoltageCheckResult;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.monitoring.results.RaoResultWithVoltageMonitoring;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static com.farao_community.farao.swe.runner.app.voltage.VoltageMonitoringResultTestUtils.CONTINGENCY_ID;
import static com.farao_community.farao.swe.runner.app.voltage.VoltageMonitoringResultTestUtils.EXPECTED_LOWER_0;
import static com.farao_community.farao.swe.runner.app.voltage.VoltageMonitoringResultTestUtils.EXPECTED_LOWER_1;
import static com.farao_community.farao.swe.runner.app.voltage.VoltageMonitoringResultTestUtils.EXPECTED_MAX_0;
import static com.farao_community.farao.swe.runner.app.voltage.VoltageMonitoringResultTestUtils.EXPECTED_MAX_1;
import static com.farao_community.farao.swe.runner.app.voltage.VoltageMonitoringResultTestUtils.EXPECTED_MIN_0;
import static com.farao_community.farao.swe.runner.app.voltage.VoltageMonitoringResultTestUtils.EXPECTED_MIN_1;
import static com.farao_community.farao.swe.runner.app.voltage.VoltageMonitoringResultTestUtils.EXPECTED_UPPER_0;
import static com.farao_community.farao.swe.runner.app.voltage.VoltageMonitoringResultTestUtils.EXPECTED_UPPER_1;
import static com.farao_community.farao.swe.runner.app.voltage.VoltageMonitoringResultTestUtils.NETWORK_1_ID;
import static com.farao_community.farao.swe.runner.app.voltage.VoltageMonitoringResultTestUtils.NETWORK_2_ID;

import com.powsybl.openrao.commons.Unit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */
class VoltageResultMapperTest {
    static VoltageResultMapper mapper = new VoltageResultMapper();
    private static VoltageCheckResult result;
    private static final Instant PREVENTIVE_INSTANT = Mockito.mock(Instant.class);
    private static final Instant CURATIVE_INSTANT = Mockito.mock(Instant.class);
    private static final Set<VoltageCnec> VOLTAGE_CNEC_SET = new HashSet<>();

    @BeforeAll
    static void getVoltageMonitoringResult() {
        final RaoResultWithVoltageMonitoring voltageMonitoringResult = getMonitoringResult();
        final Crac crac = Mockito.mock(Crac.class);
        Mockito.when(crac.getVoltageCnecs()).thenReturn(VOLTAGE_CNEC_SET);
        result = mapper.mapVoltageResult(voltageMonitoringResult, crac);
    }

    private static RaoResultWithVoltageMonitoring getMonitoringResult() {
        when(PREVENTIVE_INSTANT.isPreventive()).thenReturn(true);
        when(PREVENTIVE_INSTANT.getKind()).thenReturn(InstantKind.PREVENTIVE);
        when(CURATIVE_INSTANT.isPreventive()).thenReturn(false);
        when(CURATIVE_INSTANT.isCurative()).thenReturn(true);
        when(CURATIVE_INSTANT.getKind()).thenReturn(InstantKind.CURATIVE);
        RaoResultWithVoltageMonitoring voltageMonitoringResult = Mockito.mock(RaoResultWithVoltageMonitoring.class);
        when(voltageMonitoringResult.getComputationStatus()).thenReturn(ComputationStatus.DEFAULT);
        // Preventive constraints are not yet supported by Open Rao
        VoltageCnec voltageCnec1 = Mockito.mock(VoltageCnec.class);
        State state1 = Mockito.mock(State.class);
        when(state1.getInstant()).thenReturn(PREVENTIVE_INSTANT);
        when(voltageCnec1.getState()).thenReturn(state1);
        NetworkElement networkElement1 = Mockito.mock(NetworkElement.class);
        when(networkElement1.getId()).thenReturn(NETWORK_1_ID);
        when(voltageCnec1.getNetworkElement()).thenReturn(networkElement1);
        when(voltageCnec1.getUpperBound(Unit.KILOVOLT)).thenReturn(Optional.of(EXPECTED_UPPER_1));
        when(voltageCnec1.getLowerBound(Unit.KILOVOLT)).thenReturn(Optional.of(EXPECTED_LOWER_1));
        //
        VoltageCnec voltageCnec2 = Mockito.mock(VoltageCnec.class);
        State state2 = Mockito.mock(State.class);
        when(state2.getInstant()).thenReturn(CURATIVE_INSTANT);
        Contingency contingency = Mockito.mock(Contingency.class);
        when(contingency.getId()).thenReturn(CONTINGENCY_ID);
        when(state2.getContingency()).thenReturn(Optional.of(contingency));
        when(voltageCnec2.getState()).thenReturn(state2);
        NetworkElement networkElement2 = Mockito.mock(NetworkElement.class);
        when(networkElement2.getId()).thenReturn(NETWORK_2_ID);
        when(voltageCnec2.getNetworkElement()).thenReturn(networkElement2);
        when(voltageCnec2.getUpperBound(Unit.KILOVOLT)).thenReturn(Optional.of(EXPECTED_UPPER_0));
        when(voltageCnec2.getLowerBound(Unit.KILOVOLT)).thenReturn(Optional.of(EXPECTED_LOWER_0));
        when(voltageMonitoringResult.getMargin(CURATIVE_INSTANT, voltageCnec1, Unit.KILOVOLT)).thenReturn(-1d);
        when(voltageMonitoringResult.getMargin(CURATIVE_INSTANT, voltageCnec2, Unit.KILOVOLT)).thenReturn(-1d);
        when(voltageMonitoringResult.getMinVoltage(CURATIVE_INSTANT, voltageCnec1, Unit.KILOVOLT)).thenReturn(EXPECTED_MIN_1);
        when(voltageMonitoringResult.getMinVoltage(CURATIVE_INSTANT, voltageCnec1, Unit.KILOVOLT)).thenReturn(EXPECTED_MIN_1);
        when(voltageMonitoringResult.getMaxVoltage(CURATIVE_INSTANT, voltageCnec1, Unit.KILOVOLT)).thenReturn(EXPECTED_MAX_1);
        when(voltageMonitoringResult.getMinVoltage(CURATIVE_INSTANT, voltageCnec2, Unit.KILOVOLT)).thenReturn(EXPECTED_MIN_0);
        when(voltageMonitoringResult.getMaxVoltage(CURATIVE_INSTANT, voltageCnec2, Unit.KILOVOLT)).thenReturn(EXPECTED_MAX_0);
        when(voltageMonitoringResult.getSecurityStatus()).thenReturn(Cnec.SecurityStatus.HIGH_CONSTRAINT);
        // gather voltage cnecs as we will need to test their values
        VOLTAGE_CNEC_SET.add(voltageCnec1);
        VOLTAGE_CNEC_SET.add(voltageCnec2);
        return voltageMonitoringResult;
    }

    @Test
    void mapVoltageResultTestCheckListAndState() {
        assertNotNull(result);
        assertEquals(1, result.getConstraintElements().size());
        assertEquals(Cnec.SecurityStatus.HIGH_CONSTRAINT, result.getStatus());
    }

    @Test
    void mapVoltageResultTestCheckListElememnts() {
        result.getConstraintElements().forEach(voltResult -> {
            assertEquals(EXPECTED_LOWER_0, voltResult.getLowerBound(), VoltageMonitoringResultTestUtils.DELTA_BIG);
            assertEquals(EXPECTED_UPPER_0, voltResult.getUpperBound(), VoltageMonitoringResultTestUtils.DELTA_SMALL);
            assertEquals(EXPECTED_MIN_0, voltResult.getMinVoltage(), VoltageMonitoringResultTestUtils.DELTA_SMALL);
            assertEquals(EXPECTED_MAX_0, voltResult.getMaxVoltage(), VoltageMonitoringResultTestUtils.DELTA_SMALL);
            assertEquals(NETWORK_2_ID, voltResult.getNetworkElementId());
            assertEquals(CONTINGENCY_ID, voltResult.getContingencyId());
        });
    }
}

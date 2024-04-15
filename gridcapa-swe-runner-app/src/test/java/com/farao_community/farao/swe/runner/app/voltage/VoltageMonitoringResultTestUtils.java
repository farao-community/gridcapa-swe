/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.voltage;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.NetworkElement;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.VoltageCnec;
import com.powsybl.openrao.monitoring.voltagemonitoring.VoltageMonitoringResult;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */
public final class VoltageMonitoringResultTestUtils {
    public static final String CONTINGENCY_ID = "contingency-id";
    public static final String NETWORK_1_ID = "netId1";
    public static final String NETWORK_2_ID = "netId2";
    public static final double EXPECTED_LOWER_0 = 91.23245678;
    public static final double EXPECTED_UPPER_0 = 1232.45678;
    public static final double EXPECTED_MIN_0 = 99.223;
    public static final double EXPECTED_MAX_0 = 223.99;
    public static final double EXPECTED_LOWER_1 = 912.3245678;
    public static final double EXPECTED_UPPER_1 = 123245.678;
    public static final double EXPECTED_MIN_1 = 66.155;
    public static final double EXPECTED_MAX_1 = 155.666;
    public static final double DELTA_SMALL = 0.001;
    public static final double DELTA_BIG = 0.0000001;
    private static final Instant PREVENTIVE_INSTANT = Mockito.mock(Instant.class);
    private static final Instant CURATIVE_INSTANT = Mockito.mock(Instant.class);

    public static VoltageMonitoringResult getMonitoringResult() {
        Mockito.when(PREVENTIVE_INSTANT.isPreventive()).thenReturn(true);
        Mockito.when(PREVENTIVE_INSTANT.getKind()).thenReturn(InstantKind.PREVENTIVE);
        Mockito.when(CURATIVE_INSTANT.isPreventive()).thenReturn(false);
        Mockito.when(CURATIVE_INSTANT.isCurative()).thenReturn(true);
        Mockito.when(CURATIVE_INSTANT.getKind()).thenReturn(InstantKind.CURATIVE);
        VoltageMonitoringResult voltageMonitoringResult = Mockito.mock(VoltageMonitoringResult.class);
        Mockito.when(voltageMonitoringResult.getStatus()).thenReturn(VoltageMonitoringResult.Status.SECURE);
        Set<VoltageCnec> constraintElements = new HashSet<>();
        VoltageCnec voltageCnec1 = Mockito.mock(VoltageCnec.class);
        State state1 = Mockito.mock(State.class);
        Mockito.when(state1.getInstant()).thenReturn(PREVENTIVE_INSTANT);
        Mockito.when(voltageCnec1.getState()).thenReturn(state1);
        NetworkElement networkElement1 = Mockito.mock(NetworkElement.class);
        Mockito.when(networkElement1.getId()).thenReturn(NETWORK_1_ID);
        Mockito.when(voltageCnec1.getNetworkElement()).thenReturn(networkElement1);
        Mockito.when(voltageCnec1.getUpperBound(Unit.KILOVOLT)).thenReturn(Optional.of(EXPECTED_UPPER_1));
        Mockito.when(voltageCnec1.getLowerBound(Unit.KILOVOLT)).thenReturn(Optional.of(EXPECTED_LOWER_1));
        constraintElements.add(voltageCnec1);
        VoltageCnec voltageCnec2 = Mockito.mock(VoltageCnec.class);
        State state2 = Mockito.mock(State.class);
        Mockito.when(state2.getInstant()).thenReturn(CURATIVE_INSTANT);
        Contingency contingency = Mockito.mock(Contingency.class);
        Mockito.when(contingency.getId()).thenReturn(CONTINGENCY_ID);
        Mockito.when(state2.getContingency()).thenReturn(Optional.of(contingency));
        Mockito.when(voltageCnec2.getState()).thenReturn(state2);
        NetworkElement networkElement2 = Mockito.mock(NetworkElement.class);
        Mockito.when(networkElement2.getId()).thenReturn(NETWORK_2_ID);
        Mockito.when(voltageCnec2.getNetworkElement()).thenReturn(networkElement2);
        Mockito.when(voltageCnec2.getUpperBound(Unit.KILOVOLT)).thenReturn(Optional.of(EXPECTED_UPPER_0));
        Mockito.when(voltageCnec2.getLowerBound(Unit.KILOVOLT)).thenReturn(Optional.of(EXPECTED_LOWER_0));
        constraintElements.add(voltageCnec2);
        Mockito.when(voltageMonitoringResult.getMinVoltage(voltageCnec1)).thenReturn(EXPECTED_MIN_1);
        Mockito.when(voltageMonitoringResult.getMaxVoltage(voltageCnec1)).thenReturn(EXPECTED_MAX_1);
        Mockito.when(voltageMonitoringResult.getMinVoltage(voltageCnec2)).thenReturn(EXPECTED_MIN_0);
        Mockito.when(voltageMonitoringResult.getMaxVoltage(voltageCnec2)).thenReturn(EXPECTED_MAX_0);
        Mockito.when(voltageMonitoringResult.getConstrainedElements()).thenReturn(constraintElements);
        return voltageMonitoringResult;
    }

    private VoltageMonitoringResultTestUtils() { }
}

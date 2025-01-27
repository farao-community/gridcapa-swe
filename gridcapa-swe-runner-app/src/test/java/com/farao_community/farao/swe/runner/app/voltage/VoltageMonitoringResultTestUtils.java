/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.swe.runner.app.voltage;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.monitoring.results.RaoResultWithVoltageMonitoring;
import org.mockito.Mockito;

import java.util.Optional;

import static com.powsybl.openrao.commons.MinOrMax.MAX;
import static com.powsybl.openrao.commons.MinOrMax.MIN;
import static com.powsybl.openrao.commons.Unit.KILOVOLT;
import static org.mockito.Mockito.when;

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

    public static RaoResultWithVoltageMonitoring getMonitoringResult() {
        when(PREVENTIVE_INSTANT.isPreventive()).thenReturn(true);
        when(PREVENTIVE_INSTANT.getKind()).thenReturn(InstantKind.PREVENTIVE);
        when(CURATIVE_INSTANT.isPreventive()).thenReturn(false);
        when(CURATIVE_INSTANT.isCurative()).thenReturn(true);
        when(CURATIVE_INSTANT.getKind()).thenReturn(InstantKind.CURATIVE);
        RaoResultWithVoltageMonitoring voltageMonitoringResult = Mockito.mock(RaoResultWithVoltageMonitoring.class);
        when(voltageMonitoringResult.getComputationStatus()).thenReturn(ComputationStatus.DEFAULT);
        //
        VoltageCnec voltageCnec1 = Mockito.mock(VoltageCnec.class);
        State state1 = Mockito.mock(State.class);
        when(state1.getInstant()).thenReturn(PREVENTIVE_INSTANT);
        when(voltageCnec1.getState()).thenReturn(state1);
        NetworkElement networkElement1 = Mockito.mock(NetworkElement.class);
        when(networkElement1.getId()).thenReturn(NETWORK_1_ID);
        when(voltageCnec1.getNetworkElement()).thenReturn(networkElement1);
        when(voltageCnec1.getUpperBound(KILOVOLT)).thenReturn(Optional.of(EXPECTED_UPPER_1));
        when(voltageCnec1.getLowerBound(KILOVOLT)).thenReturn(Optional.of(EXPECTED_LOWER_1));
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
        when(voltageCnec2.getUpperBound(KILOVOLT)).thenReturn(Optional.of(EXPECTED_UPPER_0));
        when(voltageCnec2.getLowerBound(KILOVOLT)).thenReturn(Optional.of(EXPECTED_LOWER_0));
        when(voltageMonitoringResult.getMinVoltage(CURATIVE_INSTANT, voltageCnec1, MIN, KILOVOLT)).thenReturn(EXPECTED_MIN_1);
        when(voltageMonitoringResult.getMaxVoltage(CURATIVE_INSTANT, voltageCnec1, MAX, KILOVOLT)).thenReturn(EXPECTED_MAX_1);
        when(voltageMonitoringResult.getMinVoltage(CURATIVE_INSTANT, voltageCnec2, MIN, KILOVOLT)).thenReturn(EXPECTED_MIN_0);
        when(voltageMonitoringResult.getMaxVoltage(CURATIVE_INSTANT, voltageCnec2, MAX, KILOVOLT)).thenReturn(EXPECTED_MAX_0);
        when(voltageMonitoringResult.getSecurityStatus()).thenReturn(Cnec.SecurityStatus.SECURE);

        return voltageMonitoringResult;
    }

    private VoltageMonitoringResultTestUtils() {
    }
}


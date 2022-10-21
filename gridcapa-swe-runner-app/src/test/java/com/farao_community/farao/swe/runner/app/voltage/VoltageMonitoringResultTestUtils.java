package com.farao_community.farao.swe.runner.app.voltage;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.monitoring.voltage_monitoring.VoltageMonitoringResult;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

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

    public static VoltageMonitoringResult getMonitoringResult() {
        VoltageMonitoringResult voltageMonitoringResult = Mockito.mock(VoltageMonitoringResult.class);
        Mockito.when(voltageMonitoringResult.getStatus()).thenReturn(VoltageMonitoringResult.Status.SECURE);
        Set<VoltageCnec> constraintElements = new HashSet<>();
        VoltageCnec voltageCnec1 = Mockito.mock(VoltageCnec.class);
        State state1 = Mockito.mock(State.class);
        Mockito.when(state1.getInstant()).thenReturn(Instant.PREVENTIVE);
        Mockito.when(voltageCnec1.getState()).thenReturn(state1);
        NetworkElement networkElement1 = Mockito.mock(NetworkElement.class);
        Mockito.when(networkElement1.getId()).thenReturn(NETWORK_1_ID);
        Mockito.when(voltageCnec1.getNetworkElement()).thenReturn(networkElement1);
        Mockito.when(voltageCnec1.getUpperBound(Unit.KILOVOLT)).thenReturn(Optional.of(EXPECTED_UPPER_1));
        Mockito.when(voltageCnec1.getLowerBound(Unit.KILOVOLT)).thenReturn(Optional.of(EXPECTED_LOWER_1));
        constraintElements.add(voltageCnec1);
        VoltageCnec voltageCnec2 = Mockito.mock(VoltageCnec.class);
        State state2 = Mockito.mock(State.class);
        Mockito.when(state2.getInstant()).thenReturn(Instant.CURATIVE);
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

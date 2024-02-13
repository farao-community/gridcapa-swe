/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.voltage;

import com.powsybl.openrao.monitoring.voltagemonitoring.VoltageMonitoringResult;
import com.farao_community.farao.swe.runner.app.voltage.json.VoltageCheckResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */
class VoltageResultMapperTest {
    static VoltageResultMapper mapper = new VoltageResultMapper();
    private static VoltageCheckResult result;

    @Test
    void mapVoltageResultTestCheckListAndState() {
        assertNotNull(result);
        assertEquals(2, result.getConstraintElements().size());
        assertEquals(VoltageMonitoringResult.Status.SECURE, result.getIsSecure());
    }

    @Test
    void mapVoltageResultTestCheckListElememnts() {
        result.getConstraintElements().forEach(voltResult -> {
            if ("CURATIVE".equals(voltResult.getInstant())) {
                assertEquals(VoltageMonitoringResultTestUtils.EXPECTED_LOWER_0, voltResult.getLowerBound(), VoltageMonitoringResultTestUtils.DELTA_BIG);
                assertEquals(VoltageMonitoringResultTestUtils.EXPECTED_UPPER_0, voltResult.getUpperBound(), VoltageMonitoringResultTestUtils.DELTA_SMALL);
                assertEquals(VoltageMonitoringResultTestUtils.EXPECTED_MIN_0, voltResult.getMinVoltage(), VoltageMonitoringResultTestUtils.DELTA_SMALL);
                assertEquals(VoltageMonitoringResultTestUtils.EXPECTED_MAX_0, voltResult.getMaxVoltage(), VoltageMonitoringResultTestUtils.DELTA_SMALL);
                assertEquals(VoltageMonitoringResultTestUtils.NETWORK_2_ID, voltResult.getNetworkElementId());
                assertEquals(VoltageMonitoringResultTestUtils.CONTINGENCY_ID, voltResult.getContingencyId());
            } else {
                assertEquals(VoltageMonitoringResultTestUtils.EXPECTED_LOWER_1, voltResult.getLowerBound(), VoltageMonitoringResultTestUtils.DELTA_BIG);
                assertEquals(VoltageMonitoringResultTestUtils.EXPECTED_UPPER_1, voltResult.getUpperBound(), VoltageMonitoringResultTestUtils.DELTA_SMALL);
                assertEquals(VoltageMonitoringResultTestUtils.EXPECTED_MIN_1, voltResult.getMinVoltage(), VoltageMonitoringResultTestUtils.DELTA_SMALL);
                assertEquals(VoltageMonitoringResultTestUtils.EXPECTED_MAX_1, voltResult.getMaxVoltage(), VoltageMonitoringResultTestUtils.DELTA_SMALL);
                assertEquals(VoltageMonitoringResultTestUtils.NETWORK_1_ID, voltResult.getNetworkElementId());
                assertEquals("PREVENTIVE", voltResult.getInstant());
                assertNull(voltResult.getContingencyId());
            }
        });
    }

    @BeforeAll
    private static void getVoltageMonitoringResult() {
        VoltageMonitoringResult voltageMonitoringResult = VoltageMonitoringResultTestUtils.getMonitoringResult();
        result = mapper.mapVoltageResult(voltageMonitoringResult);
    }
}

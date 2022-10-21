/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.voltage.json;

import com.farao_community.farao.monitoring.voltage_monitoring.VoltageMonitoringResult;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */
class VoltageCheckResultTest {

    @Test
    void testNullIsSecureParameter() {
        NullPointerException npe = assertThrows(NullPointerException.class, () -> {
            new VoltageCheckResult(null, null);
            fail("No is secure parameter");
        });
        assertEquals("The value of isSecure cannot be null in VoltageCheckResult", npe.getMessage());
    }

    @Test
    void testNullListParameter() {
        NullPointerException npe = assertThrows(NullPointerException.class, () -> {
            VoltageCheckResult result = new VoltageCheckResult(VoltageMonitoringResult.Status.SECURE, null);
            fail("No is secure parameter");
        });
        assertEquals("The value of constraintElements cannot be null in VoltageCheckResult", npe.getMessage());
    }

    @Test
    void testOk() {
        VoltageCheckResult result = new VoltageCheckResult(VoltageMonitoringResult.Status.HIGH_AND_LOW_VOLTAGE_CONSTRAINTS, Collections.emptyList());
        assertEquals(VoltageMonitoringResult.Status.HIGH_AND_LOW_VOLTAGE_CONSTRAINTS, result.getIsSecure());
        assertEquals(Collections.emptyList(), result.getConstraintElements());
    }
}

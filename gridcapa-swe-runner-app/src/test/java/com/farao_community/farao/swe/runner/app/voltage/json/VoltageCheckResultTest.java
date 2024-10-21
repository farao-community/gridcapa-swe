/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.voltage.json;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */
class VoltageCheckResultTest {

    @Test
    void testNullListParameter() {
        final NullPointerException npe = assertThrows(NullPointerException.class, () -> {
            new VoltageCheckResult(true, null);
            fail("No is secure parameter");
        });
        assertEquals("The value of constraintElements cannot be null in VoltageCheckResult", npe.getMessage());
    }

    @Test
    void testOk() {
        final VoltageCheckResult result = new VoltageCheckResult(true, Collections.emptyList());
        assertTrue(result.getIsSecure());
        assertTrue(result.getConstraintElements().isEmpty());
    }
}

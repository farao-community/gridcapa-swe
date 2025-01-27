/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.api.resource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
class SweResponseTest {
    @Test
    void simpleRequestTest() {
        SweResponse sweResponse = new SweResponse("id", "ttcDocUrl", true, false);
        assertNotNull(sweResponse);
        assertEquals("id", sweResponse.getId());
        assertEquals("ttcDocUrl", sweResponse.getTtcDocUrl());
        assertTrue(sweResponse.isInterrupted());
        assertFalse(sweResponse.isAllRaoFailed());
    }

    @Test
    void simpleRequestRaoFailedTest() {
        SweResponse sweResponse = new SweResponse("id", "ttcDocUrl", false, true);
        assertNotNull(sweResponse);
        assertEquals("id", sweResponse.getId());
        assertEquals("ttcDocUrl", sweResponse.getTtcDocUrl());
        assertFalse(sweResponse.isInterrupted());
        assertTrue(sweResponse.isAllRaoFailed());
    }
}

/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.api.resource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
class SweResponseTest {

    @Test
    void simpleRequestTest() {
        SweResponse sweResponse = new SweResponse("id", "ttcDocUrl",

                "esFrHighestValidStepUrl", "esFrLowestInvalidStepUrl", "esFrCgmesZipUrl");
        assertNotNull(sweResponse);
        assertEquals("id", sweResponse.getId());
        assertEquals("ttcDocUrl", sweResponse.getTtcDocUrl());
        assertEquals("esFrHighestValidStepUrl", sweResponse.getEsFrHighestValidStepUrl());
        assertEquals("esFrLowestInvalidStepUrl", sweResponse.getEsFrLowestInvalidStepUrl());
        assertEquals("esFrCgmesZipUrl", sweResponse.getEsFrCgmesZipUrl());
    }

}

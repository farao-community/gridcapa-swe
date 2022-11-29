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

        SweResponse sweResponse = new SweResponse("id", "ttcDocUrl", "esFrVoltageZipUrl", "frEsVoltageZipUrl",
                "esFrCgmesZipUrl", "frEsCgmesZipUrl",
                "esPtCgmesZipUrl", "ptEsCgmesZipUrl",
                "esFrHighestValidStepUrl", "esFrLowestInvalidStepUrl",
                "frEsHighestValidStepUrl", "frEsLowestInvalidStepUrl",
                "esPtHighestValidStepUrl", "esPtLowestInvalidStepUrl",
                "ptEsHighestValidStepUrl", "ptEsLowestInvalidStepUrl");

        assertNotNull(sweResponse);
        assertEquals("id", sweResponse.getId());
        assertEquals("ttcDocUrl", sweResponse.getTtcDocUrl());
        assertEquals("esFrVoltageZipUrl", sweResponse.getEsFrVoltageZipUrl());
        assertEquals("frEsVoltageZipUrl", sweResponse.getFrEsVoltageZipUrl());
        assertEquals("esFrHighestValidStepUrl", sweResponse.getEsFrHighestValidStepUrl());
        assertEquals("esFrLowestInvalidStepUrl", sweResponse.getEsFrLowestInvalidStepUrl());
        assertEquals("frEsHighestValidStepUrl", sweResponse.getFrEsHighestValidStepUrl());
        assertEquals("frEsLowestInvalidStepUrl", sweResponse.getFrEsLowestInvalidStepUrl());
        assertEquals("esFrCgmesZipUrl", sweResponse.getEsFrCgmesZipUrl());
        assertEquals("frEsCgmesZipUrl", sweResponse.getFrEsCgmesZipUrl());
        assertEquals("esPtCgmesZipUrl", sweResponse.getEsPtCgmesZipUrl());
        assertEquals("ptEsCgmesZipUrl", sweResponse.getPtEsCgmesZipUrl());
        assertEquals("ptEsHighestValidStepUrl", sweResponse.getPtEsHighestValidStepUrl());
        assertEquals("ptEsLowestInvalidStepUrl", sweResponse.getPtEsLowestInvalidStepUrl());
        assertEquals("esPtHighestValidStepUrl", sweResponse.getEsPtHighestValidStepUrl());
        assertEquals("esPtLowestInvalidStepUrl", sweResponse.getEsPtLowestInvalidStepUrl());
    }

}

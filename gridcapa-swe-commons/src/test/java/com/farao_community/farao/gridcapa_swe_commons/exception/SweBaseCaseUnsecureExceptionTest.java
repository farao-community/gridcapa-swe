/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_swe_commons.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */
class SweBaseCaseUnsecureExceptionTest {

    @Test
    void checkException() {
        AbstractSweException sweException = new SweBaseCaseUnsecureException("Base case exception message");
        assertEquals("Base case exception message", sweException.getMessage());
        assertEquals(500, sweException.getStatus());
        assertNotNull(sweException.getCode());

        Exception cause = new RuntimeException("Cause");
        AbstractSweException exception = new SweBaseCaseUnsecureException("Base case exception message", cause);
        assertEquals("Base case exception message", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

}

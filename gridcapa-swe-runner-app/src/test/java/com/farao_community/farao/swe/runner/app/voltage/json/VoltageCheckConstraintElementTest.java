/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.voltage.json;

import com.powsybl.openrao.data.cracapi.Instant;
import com.farao_community.farao.swe.runner.app.voltage.VoltageMonitoringResultTestUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */
class VoltageCheckConstraintElementTest {

    private static final Instant PREVENTIVE_INSTANT = Mockito.mock(Instant.class);
    private static final Instant CURATIVE_INSTANT = Mockito.mock(Instant.class);

    @Test
    void testEmptyNetworkElementIdParameter() {
        Mockito.when(PREVENTIVE_INSTANT.isPreventive()).thenReturn(true);
        NullPointerException npe = assertThrows(NullPointerException.class, () -> {
            new VoltageCheckConstraintElement(null, "PREVENTIVE", null, 1.2, 1.3, 1.4, 1.5);
            fail("Should have thrown npe exception because no network element id");
        });
        assertEquals("The value of networkElementId cannot be null in VoltageCheckConstraintElement", npe.getMessage());
    }

    @Test
    void testEmptyInstantParameter() {
        NullPointerException npe = assertThrows(NullPointerException.class, () -> {
            new VoltageCheckConstraintElement("testid", null, "", 1.2, 1.3, 1.4, 1.5);
            fail("Should have thrown npe exception because no instant");
        });
        assertEquals("The value of instant cannot be null in VoltageCheckConstraintElement", npe.getMessage());
    }

    @Test
    void testEmptyMinVoltageParameter() {
        Mockito.when(CURATIVE_INSTANT.isCurative()).thenReturn(true);
        NullPointerException npe = assertThrows(NullPointerException.class, () -> {
            new VoltageCheckConstraintElement("testid", "CURATIVE", "test", null, 1.3, 1.4, 1.5);
            fail("Should have thrown npe exception because no minVoltage");
        });
        assertEquals("The value of minVoltage cannot be null in VoltageCheckConstraintElement", npe.getMessage());
    }

    @Test
    void testEmptyMaxVoltageParameter() {
        Mockito.when(CURATIVE_INSTANT.isCurative()).thenReturn(true);
        NullPointerException npe = assertThrows(NullPointerException.class, () -> {
            new VoltageCheckConstraintElement("testid", "CURATIVE", "test", 1.2, null, 1.4, 1.5);
            fail("Should have thrown npe exception because no maxVoltage");
        });
        assertEquals("The value of maxVoltage cannot be null in VoltageCheckConstraintElement", npe.getMessage());
    }

    @Test
    void testEmptyLowerBoundParameter() {
        Mockito.when(CURATIVE_INSTANT.isCurative()).thenReturn(true);
        NullPointerException npe = assertThrows(NullPointerException.class, () -> {
            new VoltageCheckConstraintElement("testid", "CURATIVE", "test", 1.2, 1.3, null, 1.5);
            fail("Should have thrown npe exception because no lowerBound");
        });
        assertEquals("The value of lowerBound cannot be null in VoltageCheckConstraintElement", npe.getMessage());
    }

    @Test
    void testEmptyHigherBoundParameter() {
        Mockito.when(CURATIVE_INSTANT.isCurative()).thenReturn(true);
        NullPointerException npe = assertThrows(NullPointerException.class, () -> {
            new VoltageCheckConstraintElement("testid", "CURATIVE", "test", 1.2, 1.3, 1.4, null);
            fail("Should have thrown npe exception because no higherBound");
        });
        assertEquals("The value of upperBound cannot be null in VoltageCheckConstraintElement", npe.getMessage());
    }

    @Test
    void testOKAllParameter() {
        Mockito.when(CURATIVE_INSTANT.isCurative()).thenReturn(true);
        try {
            VoltageCheckConstraintElement result = new VoltageCheckConstraintElement("testid", "CURATIVE", "test", 1.2, 1.3, 1.4, 1.5);
            assertNotNull(result);
            assertEquals("testid", result.getNetworkElementId());
            assertEquals("CURATIVE", result.getInstant());
            assertEquals("test", result.getContingencyId());
            assertEquals(1.2, result.getMinVoltage(), VoltageMonitoringResultTestUtils.DELTA_SMALL);
            assertEquals(1.3, result.getMaxVoltage(), VoltageMonitoringResultTestUtils.DELTA_SMALL);
            assertEquals(1.4, result.getLowerBound(), VoltageMonitoringResultTestUtils.DELTA_SMALL);
            assertEquals(1.5, result.getUpperBound(), VoltageMonitoringResultTestUtils.DELTA_SMALL);

        } catch (NullPointerException npe) {
            fail("Should not have thrown npe exception");
        }
    }

    @Test
    void testOKAllParameterNullContigency() {
        Mockito.when(CURATIVE_INSTANT.isCurative()).thenReturn(true);
        try {
            VoltageCheckConstraintElement result = new VoltageCheckConstraintElement("testid", "CURATIVE", null, 1.2, 1.3, 1.4, 1.5);
            assertNotNull(result);
        } catch (NullPointerException npe) {
            fail("Should not have thrown npe exception");
        }
    }
}

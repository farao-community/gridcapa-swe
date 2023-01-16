/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.swe.runner.app.domain;

import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.monitoring.voltage_monitoring.VoltageMonitoringResult;
import com.farao_community.farao.swe.runner.app.dichotomy.DichotomyDirection;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */

class SweDichotomyResultTest {

    private static DichotomyResult<SweDichotomyValidationData> dichotomyResult;

    private static VoltageMonitoringResult voltageMonitoringResult;

    @BeforeAll
    static void setup() {
        dichotomyResult = Mockito.mock(DichotomyResult.class);
        voltageMonitoringResult = Mockito.mock(VoltageMonitoringResult.class);
    }

    @Test
    void simpleTestEmptyOptional() {
        SweDichotomyResult result = new SweDichotomyResult(DichotomyDirection.ES_FR, dichotomyResult, Optional.empty(), "exportedCgmesUrl", "highestValidStepUrl", "lowestValidStepUrl");
        assertEquals(DichotomyDirection.ES_FR, result.getDichotomyDirection());
        assertEquals(dichotomyResult, result.getDichotomyResult());
        assertTrue(result.getVoltageMonitoringResult().isEmpty());
        assertEquals("highestValidStepUrl", result.getHighestValidStepUrl());
        assertEquals("lowestValidStepUrl", result.getLowestInvalidStepUrl());
    }

    @Test
    void simpleTestWithOptional() {
        SweDichotomyResult result = new SweDichotomyResult(DichotomyDirection.ES_FR, dichotomyResult, Optional.of(voltageMonitoringResult), "exportedCgmesUrl", "highestValidStepUrl", "lowestValidStepUrl");
        assertEquals(DichotomyDirection.ES_FR, result.getDichotomyDirection());
        assertEquals(dichotomyResult, result.getDichotomyResult());
        assertTrue(result.getVoltageMonitoringResult().isPresent());
        assertEquals(voltageMonitoringResult, result.getVoltageMonitoringResult().get());
        assertEquals("highestValidStepUrl", result.getHighestValidStepUrl());
        assertEquals("lowestValidStepUrl", result.getLowestInvalidStepUrl());
    }
}

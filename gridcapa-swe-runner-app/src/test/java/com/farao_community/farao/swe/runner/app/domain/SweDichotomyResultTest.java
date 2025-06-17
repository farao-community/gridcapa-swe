/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.swe.runner.app.domain;

import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import com.powsybl.openrao.monitoring.results.RaoResultWithVoltageMonitoring;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */

class SweDichotomyResultTest {

    private DichotomyResult<SweDichotomyValidationData> dichotomyResult;

    private static RaoResultWithVoltageMonitoring voltageMonitoringResult;

    @BeforeEach
    void setup() {
        dichotomyResult = Mockito.mock(DichotomyResult.class);
        voltageMonitoringResult = Mockito.mock(RaoResultWithVoltageMonitoring.class);
    }

    @Test
    void simpleTestEmptyOptional() {
        SweDichotomyResult result = new SweDichotomyResult(DichotomyDirection.ES_FR, dichotomyResult, Optional.empty(), "exportedLastSecureCgmesUrl", "exportedFirstUnsecureCgmesUrl", "highestValidStepUrl", "lowestValidStepUrl");
        assertEquals(DichotomyDirection.ES_FR, result.getDichotomyDirection());
        assertEquals(dichotomyResult, result.getDichotomyResult());
        assertFalse(result.isInterrupted());
        assertTrue(result.getVoltageMonitoringResult().isEmpty());
        assertEquals("highestValidStepUrl", result.getHighestValidStepUrl());
        assertEquals("lowestValidStepUrl", result.getLowestInvalidStepUrl());
        assertEquals("exportedLastSecureCgmesUrl", result.getExportedLastSecureCgmesUrl());
        assertEquals("exportedFirstUnsecureCgmesUrl", result.getExportedFirstUnsecureCgmesUrl());
        assertFalse(result.isRaoFailed());
    }

    @Test
    void simpleTestWithOptional() {
        SweDichotomyResult result = new SweDichotomyResult(DichotomyDirection.ES_FR, dichotomyResult, Optional.of(voltageMonitoringResult), "exportedLastSecureCgmesUrl", "exportedFirstUnsecureCgmesUrl", "highestValidStepUrl", "lowestValidStepUrl");
        assertEquals(DichotomyDirection.ES_FR, result.getDichotomyDirection());
        assertEquals(dichotomyResult, result.getDichotomyResult());
        assertFalse(result.isInterrupted());
        assertTrue(result.getVoltageMonitoringResult().isPresent());
        assertEquals(voltageMonitoringResult, result.getVoltageMonitoringResult().get());
        assertEquals("highestValidStepUrl", result.getHighestValidStepUrl());
        assertEquals("lowestValidStepUrl", result.getLowestInvalidStepUrl());
        assertEquals("exportedLastSecureCgmesUrl", result.getExportedLastSecureCgmesUrl());
        assertEquals("exportedFirstUnsecureCgmesUrl", result.getExportedFirstUnsecureCgmesUrl());
        assertFalse(result.isRaoFailed());
    }

    @Test
    void simpleTestWithInterruption() {
        Mockito.when(dichotomyResult.isInterrupted()).thenReturn(true);
        SweDichotomyResult result = new SweDichotomyResult(DichotomyDirection.ES_FR, dichotomyResult, Optional.of(voltageMonitoringResult), "exportedLastSecureCgmesUrl", "exportedFirstUnsecureCgmesUrl", "highestValidStepUrl", "lowestValidStepUrl");
        assertEquals(DichotomyDirection.ES_FR, result.getDichotomyDirection());
        assertEquals(dichotomyResult, result.getDichotomyResult());
        assertTrue(result.isInterrupted());
        assertTrue(result.getVoltageMonitoringResult().isPresent());
        assertEquals(voltageMonitoringResult, result.getVoltageMonitoringResult().get());
        assertEquals("highestValidStepUrl", result.getHighestValidStepUrl());
        assertEquals("lowestValidStepUrl", result.getLowestInvalidStepUrl());
        assertEquals("exportedLastSecureCgmesUrl", result.getExportedLastSecureCgmesUrl());
        assertEquals("exportedFirstUnsecureCgmesUrl", result.getExportedFirstUnsecureCgmesUrl());
        assertFalse(result.isRaoFailed());
    }

    @Test
    void simpleTestWithRaoFailure() {
        DichotomyResult<SweDichotomyValidationData> customDichotomyResult = DichotomyResult.buildFromRaoFailure("failure");
        SweDichotomyResult result = new SweDichotomyResult(DichotomyDirection.ES_FR, customDichotomyResult, "cneFirstUnsecureUrl");
        assertEquals(DichotomyDirection.ES_FR, result.getDichotomyDirection());
        assertEquals(customDichotomyResult, result.getDichotomyResult());
        assertFalse(result.isInterrupted());
        assertFalse(result.getVoltageMonitoringResult().isPresent());
        assertNull(result.getHighestValidStepUrl());
        assertEquals("cneFirstUnsecureUrl", result.getLowestInvalidStepUrl());
        assertNull(result.getExportedLastSecureCgmesUrl());
        assertEquals(customDichotomyResult, result.getDichotomyResult());
        assertTrue(result.isRaoFailed());
    }
}


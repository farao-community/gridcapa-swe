/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy;

import com.farao_community.farao.monitoring.voltage_monitoring.VoltageMonitoringResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DichotomyLoggingTest {

    @Test
    void getVoltageCheckResult() {
        Logger logger = Mockito.mock(Logger.class);
        DichotomyLogging businessLogger = new DichotomyLogging(logger);
        assertEquals("FAILURE", ReflectionTestUtils.invokeMethod(businessLogger, "getVoltageCheckResult", DichotomyDirection.ES_FR, Optional.empty()));
        assertEquals("NONE", ReflectionTestUtils.invokeMethod(businessLogger, "getVoltageCheckResult", DichotomyDirection.PT_ES, Optional.empty()));
        assertEquals("SECURE", ReflectionTestUtils.invokeMethod(businessLogger, "getVoltageCheckResult", DichotomyDirection.FR_ES, Optional.of(new VoltageMonitoringResult(Collections.emptyMap(), Collections.emptyMap(), VoltageMonitoringResult.Status.SECURE))));
    }
}

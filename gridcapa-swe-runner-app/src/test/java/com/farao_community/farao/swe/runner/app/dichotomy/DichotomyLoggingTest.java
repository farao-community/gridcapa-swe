/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy;

import com.farao_community.farao.gridcapa_swe_commons.configuration.ProcessConfiguration;
import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import com.powsybl.openrao.monitoring.voltagemonitoring.VoltageMonitoringResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DichotomyLoggingTest {

    @Test
    void getVoltageCheckResult() {
        Logger logger = Mockito.mock(Logger.class);
        ProcessConfiguration processConfiguration = Mockito.mock(ProcessConfiguration.class);
        Mockito.when(processConfiguration.getZoneId()).thenReturn("Europe/Brussels");
        DichotomyLogging businessLogger = new DichotomyLogging(logger, processConfiguration);
        assertEquals("FAILURE", ReflectionTestUtils.invokeMethod(businessLogger, "getVoltageCheckResult", DichotomyDirection.ES_FR, Optional.empty()));
        assertEquals("NONE", ReflectionTestUtils.invokeMethod(businessLogger, "getVoltageCheckResult", DichotomyDirection.PT_ES, Optional.empty()));
        assertEquals("SECURE", ReflectionTestUtils.invokeMethod(businessLogger, "getVoltageCheckResult", DichotomyDirection.FR_ES, Optional.of(new VoltageMonitoringResult(Collections.emptyMap(), Collections.emptyMap(), VoltageMonitoringResult.Status.SECURE))));
    }

    @Test
    void getTimestampLocalized() {
        Logger logger = Mockito.mock(Logger.class);
        ProcessConfiguration processConfiguration = Mockito.mock(ProcessConfiguration.class);
        Mockito.when(processConfiguration.getZoneId()).thenReturn("Europe/Brussels");
        DichotomyLogging businessLogger = new DichotomyLogging(logger, processConfiguration);
        OffsetDateTime timestampSummer = OffsetDateTime.of(2022, 6, 18, 23, 29, 1, 0, ZoneOffset.UTC);
        assertEquals("2022-06-19 01:29", ReflectionTestUtils.invokeMethod(businessLogger, "getTimestampLocalized", timestampSummer));
        OffsetDateTime timestampWinter = OffsetDateTime.of(2022, 11, 18, 22, 27, 2, 0, ZoneOffset.UTC);
        assertEquals("2022-11-18 23:27", ReflectionTestUtils.invokeMethod(businessLogger, "getTimestampLocalized", timestampWinter));
    }
}

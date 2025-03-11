/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy;

import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.dichotomy.api.results.DichotomyStepResult;
import com.farao_community.farao.gridcapa_swe_commons.configuration.ProcessConfiguration;
import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyValidationData;
import com.farao_community.farao.swe.runner.app.domain.SweTaskParameters;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.io.cim.craccreator.CimCracCreationContext;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.monitoring.results.RaoResultWithVoltageMonitoring;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Optional;

import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

class DichotomyLoggingTest {

    @Test
    void getVoltageCheckResult() {
        Logger logger = Mockito.mock(Logger.class);
        ProcessConfiguration processConfiguration = Mockito.mock(ProcessConfiguration.class);
        Mockito.when(processConfiguration.getZoneId()).thenReturn("Europe/Brussels");
        DichotomyLogging businessLogger = new DichotomyLogging(logger, processConfiguration);
        SweTaskParameters sweTaskParameters = Mockito.mock(SweTaskParameters.class);
        Mockito.when(sweTaskParameters.isRunVoltageCheck()).thenReturn(true);
        assertEquals("FAILURE", ReflectionTestUtils.invokeMethod(businessLogger, "getVoltageCheckResult", DichotomyDirection.ES_FR, Optional.empty(), sweTaskParameters));
        assertEquals("NONE", ReflectionTestUtils.invokeMethod(businessLogger, "getVoltageCheckResult", DichotomyDirection.PT_ES, Optional.empty(), sweTaskParameters));
        final RaoResultWithVoltageMonitoring mockedVoltageMonitoringResults = Mockito.mock(RaoResultWithVoltageMonitoring.class);
        Mockito.when(mockedVoltageMonitoringResults.getSecurityStatus()).thenReturn(Cnec.SecurityStatus.SECURE);
        assertEquals("SECURE", ReflectionTestUtils.invokeMethod(businessLogger, "getVoltageCheckResult", DichotomyDirection.FR_ES, Optional.of(mockedVoltageMonitoringResults), sweTaskParameters));
    }

    @Test
    void getTimestampLocalized() {
        Logger logger = Mockito.mock(Logger.class);
        ProcessConfiguration processConfiguration = Mockito.mock(ProcessConfiguration.class);
        Mockito.when(processConfiguration.getZoneId()).thenReturn("Europe/Brussels");
        DichotomyLogging businessLogger = new DichotomyLogging(logger, processConfiguration);
        OffsetDateTime timestampSummer = OffsetDateTime.of(2022, 6, 18, 23, 29, 1, 0, UTC);
        assertEquals("2022-06-19 01:29", ReflectionTestUtils.invokeMethod(businessLogger, "getTimestampLocalized", timestampSummer));
        OffsetDateTime timestampWinter = OffsetDateTime.of(2022, 11, 18, 22, 27, 2, 0, UTC);
        assertEquals("2022-11-18 23:27", ReflectionTestUtils.invokeMethod(businessLogger, "getTimestampLocalized", timestampWinter));
    }

    @Test
    void testGenerateSummaryEvents() {
        //Given
        DichotomyDirection dichotomyDirection = DichotomyDirection.PT_ES;
        DichotomyResult<SweDichotomyValidationData> result = Mockito.mock(DichotomyResult.class);
        Mockito.when(result.getHighestValidStepValue()).thenReturn(2000d);
        Mockito.when(result.getLowestInvalidStepValue()).thenReturn(1000d);
        Mockito.when(result.hasValidStep()).thenReturn(true);
        DichotomyStepResult stepResult = Mockito.mock(DichotomyStepResult.class);
        Mockito.when(result.getHighestValidStep()).thenReturn(stepResult);
        RaoResult raoResult = Mockito.mock(RaoResult.class);
        DichotomyStepResult invalidStepResult = Mockito.mock(DichotomyStepResult.class);
        Mockito.when(result.getLowestInvalidStep()).thenReturn(invalidStepResult);
        RaoResult raoResult1 = Mockito.mock(RaoResult.class);
        Mockito.when(invalidStepResult.getRaoResult()).thenReturn(raoResult1);
        Mockito.when(stepResult.getRaoResult()).thenReturn(raoResult);
        SweDichotomyValidationData validationData = new SweDichotomyValidationData(null, SweDichotomyValidationData.AngleMonitoringStatus.SECURE);
        Mockito.when(stepResult.getValidationData()).thenReturn(validationData);
        SweData sweData = Mockito.mock(SweData.class);
        OffsetDateTime timestamp = OffsetDateTime.parse("2024-02-14T08:21:00Z");
        Mockito.when(sweData.getTimestamp()).thenReturn(timestamp);
        CimCracCreationContext context = Mockito.mock(CimCracCreationContext.class);
        Mockito.when(sweData.getCracEsPt()).thenReturn(context);
        Crac crac = Mockito.mock(Crac.class);
        Mockito.when(context.getCrac()).thenReturn(crac);
        Mockito.when(crac.getFlowCnecs()).thenReturn(Collections.emptySet());
        ProcessConfiguration processConfiguration = new ProcessConfiguration();
        processConfiguration.setZoneId("UTC");
        Logger logger = Mockito.mock(Logger.class);
        SweTaskParameters sweTaskParameters = Mockito.mock(SweTaskParameters.class);
        Mockito.when(sweTaskParameters.isRunVoltageCheck()).thenReturn(false);
        final OffsetDateTime startingTime = OffsetDateTime.of(2025, 3, 10, 15, 15, 0, 0, ZoneOffset.UTC);
        //When
        try (final MockedStatic<OffsetDateTime> mockedStatic = mockStatic(OffsetDateTime.class, CALLS_REAL_METHODS)) {
            final OffsetDateTime now = startingTime.plusHours(1).plusMinutes(2).plusSeconds(3);
            mockedStatic.when(OffsetDateTime::now).thenReturn(now);
            DichotomyLogging dichotomyLogging = new DichotomyLogging(logger, processConfiguration);
            dichotomyLogging.generateSummaryEvents(dichotomyDirection, result, sweData, Optional.empty(), sweTaskParameters, startingTime);
            //Then
            String summary = """
                    Summary :
                    Limiting event : {},
                    Limiting element : {},
                    PRAs : {},
                    CRAs : {}.""";
            String summaryBd = """
                    Summary BD :  {}
                    Last secure TTC : {},
                    First unsecure TTC : {},
                    Voltage Check : {},
                    Angle Check : {},
                    Computation time: {}h {}min {}s since the task switched to RUNNING.""";
            Mockito.verify(logger, Mockito.times(1)).info(summary, "NONE", "None", "", "");
            Mockito.verify(logger, Mockito.times(1)).info(summaryBd, "2024-02-14 08:21", "2000", "1000", "NONE", "SECURE", 1L, 2, 3);
        }
    }
}

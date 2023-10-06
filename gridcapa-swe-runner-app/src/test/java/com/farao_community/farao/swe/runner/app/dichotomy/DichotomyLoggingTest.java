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
        assertEquals("SECURE", ReflectionTestUtils.invokeMethod(businessLogger, "getVoltageCheckResult", DichotomyDirection.FR_ES, Optional.of(new VoltageMonitoringResult(Collections.emptyMap()))));
    }
}

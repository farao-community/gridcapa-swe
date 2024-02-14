package com.farao_community.farao.swe.runner.app.domain;

import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import org.junit.jupiter.api.Test;

import static com.farao_community.farao.swe.runner.app.domain.SweDichotomyValidationData.AngleMonitoringStatus.SECURE;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SweDichotomyValidationDataTest {

    @Test
    void testConstructor() {
        RaoResponse raoResponse = new RaoResponse.RaoResponseBuilder().build();
        SweDichotomyValidationData.AngleMonitoringStatus status = SECURE;
        SweDichotomyValidationData constructedData = new SweDichotomyValidationData(raoResponse, status);
        assertEquals(raoResponse, constructedData.getRaoResponse());
        assertEquals(SECURE, constructedData.getAngleMonitoringStatus());
    }
}

package com.farao_community.farao.swe.runner.app.domain;

import com.farao_community.farao.monitoring.angle_monitoring.AngleMonitoringResult;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;

public class SweDichotomyValidationData {

    private final RaoResponse raoResponse;
    private final AngleMonitoringResult angleMonitoringResult;

    public SweDichotomyValidationData(RaoResponse raoResponse, AngleMonitoringResult angleMonitoringResult) {
        this.raoResponse = raoResponse;
        this.angleMonitoringResult = angleMonitoringResult;
    }

    public RaoResponse getRaoResponse() {
        return raoResponse;
    }

    public AngleMonitoringResult getAngleMonitoringResult() {
        return angleMonitoringResult;
    }
}

/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.domain;

import com.farao_community.farao.monitoring.angle_monitoring.AngleMonitoringResult;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */

public class SweDichotomyValidationData {

    private final RaoResponse raoResponse;
    private final AngleMonitoringResult angleMonitoringResult;

    public SweDichotomyValidationData(RaoResponse raoResponse, AngleMonitoringResult angleMonitoringResult) {
        this.raoResponse = raoResponse;
        this.angleMonitoringResult = angleMonitoringResult;
    }

    public SweDichotomyValidationData(RaoResponse raoResponse) {
        this(raoResponse, null);
    }

    public RaoResponse getRaoResponse() {
        return raoResponse;
    }

    public AngleMonitoringResult getAngleMonitoringResult() {
        return angleMonitoringResult;
    }
}

/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.domain;

import com.farao_community.farao.rao_runner.api.resource.RaoResponse;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */

public class SweDichotomyValidationData {

    private final RaoResponse raoResponse;
    private final AngleMonitoringStatus angleMonitoringStatus;

    public enum AngleMonitoringStatus {
        SECURE, UNSECURE, FAILURE, NONE
    }

    public SweDichotomyValidationData(RaoResponse raoResponse,
                                      AngleMonitoringStatus status) {
        this.raoResponse = raoResponse;
        this.angleMonitoringStatus = status;
    }

    public RaoResponse getRaoResponse() {
        return raoResponse;
    }

    public AngleMonitoringStatus getAngleMonitoringStatus() {
        return angleMonitoringStatus;
    }
}

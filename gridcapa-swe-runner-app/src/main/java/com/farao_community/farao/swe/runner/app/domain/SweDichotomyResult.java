/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.domain;

import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.monitoring.voltage_monitoring.VoltageMonitoringResult;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.swe.runner.app.dichotomy.DichotomyDirection;

import java.util.Optional;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
public class SweDichotomyResult {

    private final DichotomyDirection dichotomyDirection;
    private final DichotomyResult<RaoResponse> dichotomyResult;
    private final Optional<VoltageMonitoringResult> voltageMonitoringResult;
    private final String exportedCgmesUrl;
    private final String highestValidStepUrl;
    private final String lowestInvalidStepUrl;

    public SweDichotomyResult(DichotomyDirection dichotomyDirection,
                              DichotomyResult<RaoResponse> dichotomyResult,
                              Optional<VoltageMonitoringResult> voltageMonitoringResult,
                              String exportedCgmesUrl,
                              String highestValidStepUrl,
                              String lowestInvalidStepUrl) {
        this.dichotomyDirection = dichotomyDirection;
        this.dichotomyResult = dichotomyResult;
        this.voltageMonitoringResult = voltageMonitoringResult;
        this.exportedCgmesUrl = exportedCgmesUrl;
        this.highestValidStepUrl = highestValidStepUrl;
        this.lowestInvalidStepUrl = lowestInvalidStepUrl;
    }

    public DichotomyDirection getDichotomyDirection() {
        return dichotomyDirection;
    }

    public DichotomyResult<RaoResponse> getDichotomyResult() {
        return dichotomyResult;
    }

    public Optional<VoltageMonitoringResult> getVoltageMonitoringResult() {
        return voltageMonitoringResult;
    }

    public String getExportedCgmesUrl() {
        return exportedCgmesUrl;
    }

    public String getHighestValidStepUrl() {
        return highestValidStepUrl;
    }

    public String getLowestInvalidStepUrl() {
        return lowestInvalidStepUrl;
    }
}

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

import java.util.Optional;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
public class SweDichotomyResult {

    private final DichotomyDirection dichotomyDirection;
    private final DichotomyResult<SweDichotomyValidationData> dichotomyResult;
    private final Optional<RaoResultWithVoltageMonitoring> voltageMonitoringResult;
    private final String exportedLastSecureCgmesUrl;
    private final String exportedFirstUnsecureCgmesUrl;
    private final String highestValidStepUrl;
    private final String lowestInvalidStepUrl;
    private final boolean interrupted;
    private final boolean raoFailed;
    private final boolean runFailed;

    public SweDichotomyResult(DichotomyDirection dichotomyDirection,
                              DichotomyResult<SweDichotomyValidationData> dichotomyResult,
                              Optional<RaoResultWithVoltageMonitoring> voltageMonitoringResult,
                              String exportedLastSecureCgmesUrl,
                              String exportedFirstUnsecureCgmesUrl,
                              String highestValidStepUrl,
                              String lowestInvalidStepUrl) {
        this.dichotomyDirection = dichotomyDirection;
        this.dichotomyResult = dichotomyResult;
        this.voltageMonitoringResult = voltageMonitoringResult;
        this.exportedLastSecureCgmesUrl = exportedLastSecureCgmesUrl;
        this.exportedFirstUnsecureCgmesUrl = exportedFirstUnsecureCgmesUrl;
        this.highestValidStepUrl = highestValidStepUrl;
        this.lowestInvalidStepUrl = lowestInvalidStepUrl;
        this.interrupted = dichotomyResult.isInterrupted();
        this.raoFailed = dichotomyResult.isRaoFailed();
        this.runFailed = false;
    }

    public SweDichotomyResult(DichotomyDirection dichotomyDirection,
                              DichotomyResult<SweDichotomyValidationData> dichotomyResult,
                              String lowestInvalidStepUrl) {
        this(dichotomyDirection, dichotomyResult, Optional.empty(), null, null, null, lowestInvalidStepUrl);
    }

    public SweDichotomyResult(final DichotomyDirection dichotomyDirection) {
        this.dichotomyDirection = dichotomyDirection;
        this.dichotomyResult = null;
        this.voltageMonitoringResult = Optional.empty();
        this.exportedLastSecureCgmesUrl = null;
        this.exportedFirstUnsecureCgmesUrl = null;
        this.highestValidStepUrl = null;
        this.lowestInvalidStepUrl = null;
        this.interrupted = false;
        this.raoFailed = false;
        this.runFailed = true;
    }

    public DichotomyDirection getDichotomyDirection() {
        return dichotomyDirection;
    }

    public DichotomyResult<SweDichotomyValidationData> getDichotomyResult() {
        return dichotomyResult;
    }

    public Optional<RaoResultWithVoltageMonitoring> getVoltageMonitoringResult() {
        return voltageMonitoringResult;
    }

    public String getExportedLastSecureCgmesUrl() {
        return exportedLastSecureCgmesUrl;
    }

    public String getExportedFirstUnsecureCgmesUrl() {
        return exportedFirstUnsecureCgmesUrl;
    }

    public String getHighestValidStepUrl() {
        return highestValidStepUrl;
    }

    public String getLowestInvalidStepUrl() {
        return lowestInvalidStepUrl;
    }

    public boolean isInterrupted() {
        return interrupted;
    }

    public boolean isRaoFailed() {
        return raoFailed;
    }

    public boolean isRunFailed() {
        return runFailed;
    }
}

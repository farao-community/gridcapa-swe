/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.monitoring.voltage_monitoring.VoltageMonitoringResult;
import com.farao_community.farao.swe.runner.app.configurations.DichotomyConfiguration.Parameters;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyValidationData;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Service
public class DichotomyLogging {

    private static final String NONE = "NONE";
    private final Logger businessLogger;
    private static final String SUMMARY = "Summary :  " +
            "Limiting event : {},  \n" +
            "Limiting element : {},  \n" +
            "PRAs : {},  \n" +
            "CRAs : {}.";
    private static final String SUMMARY_BD = "Summary BD :  " +
            "Current TTC : {},  \n" +
            "Previous TTC : {},  \n" +
            "Voltage Check : {},  \n" +
            "Angle Check : {}.";

    public DichotomyLogging(Logger businessLogger) {
        this.businessLogger = businessLogger;
    }

    public void logStartDichotomy(Parameters parameters) {
        businessLogger.info("Start dichotomy : minimum dichotomy index: {}, maximum dichotomy index: {}, dichotomy precision: {}", parameters.getMinValue(), parameters.getMaxValue(), parameters.getPrecision());
    }

    public void logEndOneDichotomy() {
        businessLogger.info("Dichotomy finished");
    }

    public void logEndAllDichotomies() {
        businessLogger.info("All - Dichotomies are done");
    }

    public  void generateSummaryEvents(DichotomyDirection direction, DichotomyResult<SweDichotomyValidationData> dichotomyResult, SweData sweData, Optional<VoltageMonitoringResult> voltageMonitoringResult) {
        String limitingElement = NONE;
        String printablePrasIds = NONE;
        String printableCrasIds = NONE;
        String currentTtc = String.valueOf(dichotomyResult.getHighestValidStepValue());
        String previousTtc = String.valueOf(dichotomyResult.getLowestInvalidStepValue());
        String voltageCheckStatus =  voltageMonitoringResult.isPresent() ? String.valueOf(voltageMonitoringResult.get().getStatus()) : NONE;
        String angleCheckStatus = NONE;
        String limitingCause = dichotomyResult.getLimitingCause() != null ? DichotomyResultHelper.limitingCauseToString(dichotomyResult.getLimitingCause()) : NONE;
        Crac crac = (direction == DichotomyDirection.ES_FR || direction == DichotomyDirection.FR_ES) ? sweData.getCracFrEs().getCrac() : sweData.getCracEsPt().getCrac();
        if (dichotomyResult.hasValidStep() && dichotomyResult.getHighestValidStep().getRaoResult() != null) {
            RaoResult raoResult = dichotomyResult.getHighestValidStep().getRaoResult();
            limitingElement = DichotomyResultHelper.getLimitingElement(crac, raoResult);
            printablePrasIds = toString(DichotomyResultHelper.getActivatedActionInPreventive(crac, raoResult));
            printableCrasIds = toString(DichotomyResultHelper.getActivatedActionInCurative(crac, raoResult));
            if (dichotomyResult.getHighestValidStep().getValidationData() != null && dichotomyResult.getHighestValidStep().getValidationData().getAngleMonitoringResult() != null) {
                angleCheckStatus = dichotomyResult.getHighestValidStep().getValidationData().getAngleMonitoringResult().getStatus().name();
            }
        }
        businessLogger.info(SUMMARY, limitingCause, limitingElement, printablePrasIds, printableCrasIds);
        businessLogger.info(SUMMARY_BD, currentTtc, previousTtc, voltageCheckStatus, angleCheckStatus);
    }

    private static String toString(Collection<String> c) {
        return c.stream().map(Object::toString).collect(Collectors.joining(", "));
    }
}

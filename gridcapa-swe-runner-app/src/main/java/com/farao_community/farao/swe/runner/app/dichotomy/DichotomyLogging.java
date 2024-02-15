/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy;

import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.gridcapa_swe_commons.configuration.ProcessConfiguration;
import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyValidationData;
import com.farao_community.farao.swe.runner.app.domain.SweTaskParameters;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.monitoring.voltagemonitoring.VoltageMonitoringResult;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Service
public class DichotomyLogging {

    private static final String NONE = "NONE";
    private static final String FAILURE = "FAILURE";
    private final Logger businessLogger;
    private final ZoneId zoneId;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy'-'MM'-'dd' 'HH':'mm");
    private static final String SUMMARY = """
            Summary :
            Limiting event : {},
            Limiting element : {},
            PRAs : {},
            CRAs : {}.""";
    private static final String SUMMARY_BD = """
            Summary BD :  {}
            Current TTC : {},
            Previous TTC : {},
            Voltage Check : {},
            Angle Check : {}.""";

    public DichotomyLogging(Logger businessLogger, ProcessConfiguration processConfiguration) {
        this.businessLogger = businessLogger;
        this.zoneId = ZoneId.of(processConfiguration.getZoneId());
    }

    public void logStartDichotomy(DichotomyParameters parameters) {
        businessLogger.info("Start dichotomy : minimum dichotomy index: {}, maximum dichotomy index: {}, dichotomy precision: {}", parameters.getMinValue(), parameters.getMaxValue(), parameters.getPrecision());
    }

    public void logEndOneDichotomy() {
        businessLogger.info("Dichotomy finished");
    }

    public void logEndAllDichotomies() {
        businessLogger.info("All - Dichotomies are done");
    }

    public void generateSummaryEvents(DichotomyDirection direction, DichotomyResult<SweDichotomyValidationData> dichotomyResult, SweData sweData, Optional<VoltageMonitoringResult> voltageMonitoringResult, SweTaskParameters sweTaskParameters) {
        String limitingElement = NONE;
        String printablePrasIds = NONE;
        String printableCrasIds = NONE;
        String timestamp = getTimestampLocalized(sweData.getTimestamp());
        String currentTtc = String.valueOf(dichotomyResult.getHighestValidStepValue());
        String previousTtc = String.valueOf(dichotomyResult.getLowestInvalidStepValue());
        String voltageCheckStatus =  getVoltageCheckResult(direction, voltageMonitoringResult, sweTaskParameters);
        String angleCheckStatus = NONE;
        String limitingCause = dichotomyResult.getLimitingCause() != null ? DichotomyResultHelper.limitingCauseToString(dichotomyResult.getLimitingCause()) : NONE;
        Crac crac = (direction == DichotomyDirection.ES_FR || direction == DichotomyDirection.FR_ES) ? sweData.getCracFrEs().getCrac() : sweData.getCracEsPt().getCrac();
        if (dichotomyResult.hasValidStep() && dichotomyResult.getHighestValidStep().getRaoResult() != null) {
            RaoResult raoResult = dichotomyResult.getHighestValidStep().getRaoResult();
            limitingElement = DichotomyResultHelper.getLimitingElement(crac, raoResult);
            printablePrasIds = toString(DichotomyResultHelper.getActivatedActionInPreventive(crac, raoResult));
            printableCrasIds = toString(DichotomyResultHelper.getActivatedActionInCurative(crac, raoResult));
            if (dichotomyResult.getHighestValidStep().getValidationData() != null && dichotomyResult.getHighestValidStep().getValidationData().getAngleMonitoringStatus() != null) {
                angleCheckStatus = dichotomyResult.getHighestValidStep().getValidationData().getAngleMonitoringStatus().name();
            }
        }
        businessLogger.info(SUMMARY, limitingCause, limitingElement, printablePrasIds, printableCrasIds);
        businessLogger.info(SUMMARY_BD, timestamp, currentTtc, previousTtc, voltageCheckStatus, angleCheckStatus);
    }

    private static String toString(Collection<String> c) {
        return c.stream().map(Object::toString).collect(Collectors.joining(", "));
    }

    private String getVoltageCheckResult(DichotomyDirection direction, Optional<VoltageMonitoringResult> voltageMonitoringResult, SweTaskParameters sweTaskParameters) {
        if (sweTaskParameters.isRunVoltageCheck() && (direction.equals(DichotomyDirection.FR_ES) || direction.equals(DichotomyDirection.ES_FR))) {
            if (voltageMonitoringResult.isPresent()) {
                return String.valueOf(voltageMonitoringResult.get().getStatus());
            } else {
                return FAILURE;
            }
        }
        return NONE;
    }

    private String getTimestampLocalized(OffsetDateTime timestamp) {
        OffsetDateTime localOffsetDateTime = OffsetDateTime.ofInstant(timestamp.toInstant(), zoneId);
        return DATE_TIME_FORMATTER.format(localOffsetDateTime);
    }
}

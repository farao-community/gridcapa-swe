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
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.monitoring.results.RaoResultWithVoltageMonitoring;
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

    public DichotomyLogging(final Logger businessLogger,
                            final ProcessConfiguration processConfiguration) {
        this.businessLogger = businessLogger;
        this.zoneId = ZoneId.of(processConfiguration.getZoneId());
    }

    public void logStartDichotomy(final DichotomyParameters parameters) {
        businessLogger.info("Start dichotomy : minimum dichotomy index: {}, maximum dichotomy index: {}, dichotomy precision: {}", parameters.getMinValue(), parameters.getMaxValue(), parameters.getPrecision());
    }

    public void logEndOneDichotomy() {
        businessLogger.info("Dichotomy finished");
    }

    public void logEndAllDichotomies() {
        businessLogger.info("All - Dichotomies are done");
    }

    public void generateSummaryEvents(final DichotomyDirection direction,
                                      final DichotomyResult<SweDichotomyValidationData> dichotomyResult,
                                      final SweData sweData,
                                      final Optional<RaoResultWithVoltageMonitoring> voltageMonitoringResult,
                                      final SweTaskParameters sweTaskParameters) {
        String limitingElement = NONE;
        String printablePrasIds = NONE;
        String printableCrasIds = NONE;
        final String timestamp = getTimestampLocalized(sweData.getTimestamp());
        final String currentTtc = String.valueOf((int) dichotomyResult.getHighestValidStepValue());
        final String previousTtc = String.valueOf((int) dichotomyResult.getLowestInvalidStepValue());
        final String voltageCheckStatus =  getVoltageCheckResult(direction, voltageMonitoringResult, sweTaskParameters);
        String angleCheckStatus = NONE;
        final String limitingCause = dichotomyResult.getLimitingCause() != null ? DichotomyResultHelper.limitingCauseToString(dichotomyResult.getLimitingCause()) : NONE;
        final Crac crac = (direction == DichotomyDirection.ES_FR || direction == DichotomyDirection.FR_ES) ? sweData.getCracFrEs().getCrac() : sweData.getCracEsPt().getCrac();
        if (dichotomyResult.getLowestInvalidStep() != null && dichotomyResult.getLowestInvalidStep().getRaoResult() != null) {
            final RaoResult raoResult = dichotomyResult.getLowestInvalidStep().getRaoResult();
            limitingElement = DichotomyResultHelper.getLimitingElement(crac, raoResult);
        }
        if (dichotomyResult.hasValidStep() && dichotomyResult.getHighestValidStep().getRaoResult() != null) {
            final RaoResult raoResult = dichotomyResult.getHighestValidStep().getRaoResult();
            printablePrasIds = toString(DichotomyResultHelper.getActivatedActionInPreventive(crac, raoResult));
            printableCrasIds = toString(DichotomyResultHelper.getActivatedActionInCurative(crac, raoResult));
            if (dichotomyResult.getHighestValidStep().getValidationData() != null && dichotomyResult.getHighestValidStep().getValidationData().getAngleMonitoringStatus() != null) {
                angleCheckStatus = dichotomyResult.getHighestValidStep().getValidationData().getAngleMonitoringStatus().name();
            }
        }
        businessLogger.info(SUMMARY, limitingCause, limitingElement, printablePrasIds, printableCrasIds);
        businessLogger.info(SUMMARY_BD, timestamp, currentTtc, previousTtc, voltageCheckStatus, angleCheckStatus);
    }

    private static String toString(final Collection<String> c) {
        return c.stream().map(Object::toString).collect(Collectors.joining(", "));
    }

    private String getVoltageCheckResult(final DichotomyDirection direction,
                                         final Optional<RaoResultWithVoltageMonitoring> voltageMonitoringResult,
                                         final SweTaskParameters sweTaskParameters) {
        if (sweTaskParameters.isRunVoltageCheck() && (direction.equals(DichotomyDirection.FR_ES) || direction.equals(DichotomyDirection.ES_FR))) {
            if (voltageMonitoringResult.isPresent()) {
                return String.valueOf(voltageMonitoringResult.get().getSecurityStatus());
            } else {
                return FAILURE;
            }
        }
        return NONE;
    }

    private String getTimestampLocalized(final OffsetDateTime timestamp) {
        final OffsetDateTime localOffsetDateTime = OffsetDateTime.ofInstant(timestamp.toInstant(), zoneId);
        return DATE_TIME_FORMATTER.format(localOffsetDateTime);
    }
}

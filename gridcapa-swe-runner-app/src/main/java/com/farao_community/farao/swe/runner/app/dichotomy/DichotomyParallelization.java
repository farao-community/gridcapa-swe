/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy;

import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.monitoring.voltage_monitoring.VoltageMonitoringResult;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.swe.runner.api.resource.SweResponse;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyResult;
import com.farao_community.farao.swe.runner.app.parallelization.ExecutionResult;
import com.farao_community.farao.swe.runner.app.parallelization.ParallelExecution;
import com.farao_community.farao.swe.runner.app.services.OutputService;
import com.farao_community.farao.swe.runner.app.services.VoltageCheckService;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Service
public class DichotomyParallelization {
    private final Logger businessLogger;
    private final DichotomyLogging dichotomyLogging;
    private final DichotomyRunner dichotomyRunner;
    private final OutputService outputService;
    private final VoltageCheckService voltageCheckService;
    public static final String SUMMARY = "Summary [{}] :  " +
            "Limiting event : {},  " +
            "Limiting element : {},  " +
            "PRAs : {},  " +
            "CRAs : {}.";
    public static final String SUMMARY_BD = "Summary BD [{}] :  " +
            "Current TTC : {},  " +
            "Previous TTC : {},  " +
            "Voltage Check : {},  " +
            "Angle Check : {}.";

    public DichotomyParallelization(DichotomyLogging dichotomyLogging, DichotomyRunner dichotomyRunner, OutputService outputService, VoltageCheckService voltageCheckService) {
    public DichotomyParallelization(Logger businessLogger, DichotomyLogging dichotomyLogging, DichotomyRunner dichotomyRunner, OutputService outputService) {
        this.businessLogger = businessLogger;
        this.dichotomyLogging = dichotomyLogging;
        this.dichotomyRunner = dichotomyRunner;
        this.outputService = outputService;
        this.voltageCheckService = voltageCheckService;
    }

    public SweResponse launchDichotomy(SweData sweData) {
        final ExecutionResult<SweDichotomyResult> executionResult = ParallelExecution
                .of(() -> runDichotomyForOneDirection(sweData, DichotomyDirection.ES_FR))
                // .and(() -> runDichotomyForOneDirection(sweData, Direction.FR_ES))
                // .and(() -> runDichotomyForOneDirection(sweData, Direction.ES_PT))
                // .and(() -> runDichotomyForOneDirection(sweData, Direction.PT_ES))
                .close();
        dichotomyLogging.logEndAllDichotomies();
        // build swe response from every response
        String ttcDocUrl = outputService.buildAndExportTtcDocument(sweData, executionResult);
        String voltageEsFrZipUrl = outputService.buildAndExportVoltageDoc(DichotomyDirection.ES_FR, sweData, executionResult);
        return  new SweResponse(sweData.getId(), ttcDocUrl, voltageEsFrZipUrl);
    }

    SweDichotomyResult runDichotomyForOneDirection(SweData sweData, DichotomyDirection direction) {
        // propagate in logs MDC the task requestId as an extra field to be able to send logs with calculation tasks.
        MDC.put("gridcapa-task-id", sweData.getId());
        String limitingCause = "NONE";
        String limitingElement = "NONE";
        String printablePrasIds = "NONE";
        String printableCrasIds = "NONE";
        String currentTtc = "NONE";
        String previousTtc = "NONE";
        String voltageCheckStatus = "NONE";
        String angleCheckStatus = "NONE";
        DichotomyResult<RaoResponse> dichotomyResult = dichotomyRunner.run(sweData, direction);
        dichotomyLogging.logEndOneDichotomy(direction);
        businessLogger.info(SUMMARY, direction, limitingCause, limitingElement, printablePrasIds, printableCrasIds); // todo add elements
        businessLogger.info(SUMMARY_BD, direction, currentTtc, previousTtc, voltageCheckStatus, angleCheckStatus);
        // Generate files specific for one direction (cne, cgm, voltage) and add them to the returned object (to create)
        Optional<VoltageMonitoringResult> voltageMonitoringResult = voltageCheckService.runVoltageCheck(sweData, dichotomyResult, direction);
        // fill response for one dichotomy
        return new SweDichotomyResult(direction, dichotomyResult, voltageMonitoringResult);
    }
}

/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.swe.runner.app.parallelization;

import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import com.farao_community.farao.swe.runner.app.dichotomy.DichotomyLogging;
import com.farao_community.farao.swe.runner.app.dichotomy.DichotomyRunner;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyResult;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyValidationData;
import com.farao_community.farao.swe.runner.app.domain.SweTaskParameters;
import com.farao_community.farao.swe.runner.app.services.CgmesExportService;
import com.farao_community.farao.swe.runner.app.services.CneFileExportService;
import com.farao_community.farao.swe.runner.app.services.OutputService;
import com.farao_community.farao.swe.runner.app.services.VoltageCheckService;
import com.powsybl.openrao.monitoring.results.RaoResultWithVoltageMonitoring;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */

@Service
public class DichotomyParallelizationWorker {

    private final DichotomyLogging dichotomyLogging;
    private final DichotomyRunner dichotomyRunner;
    private final VoltageCheckService voltageCheckService;
    private final CneFileExportService cneFileExportService;
    private final CgmesExportService cgmesExportService;
    private final OutputService outputService;

    public DichotomyParallelizationWorker(final DichotomyLogging dichotomyLogging,
                                          final DichotomyRunner dichotomyRunner,
                                          final VoltageCheckService voltageCheckService,
                                          final CneFileExportService cneFileExportService,
                                          final CgmesExportService cgmesExportService,
                                          final OutputService outputService) {
        this.dichotomyLogging = dichotomyLogging;
        this.dichotomyRunner = dichotomyRunner;
        this.voltageCheckService = voltageCheckService;
        this.cneFileExportService = cneFileExportService;
        this.cgmesExportService = cgmesExportService;
        this.outputService = outputService;
    }

    @Async("threadPoolTaskExecutor")
    public Future<SweDichotomyResult> runDichotomyForOneDirection(final SweData sweData,
                                                                  final SweTaskParameters sweTaskParameters,
                                                                  final DichotomyDirection direction,
                                                                  final OffsetDateTime startTime) {
        // propagate in logs MDC the task requestId as an extra field to be able to send logs with calculation tasks.
        MDC.put("gridcapa-task-id", sweData.getId());
        MDC.put("eventPrefix", direction.getDashName());
        final DichotomyResult<SweDichotomyValidationData> dichotomyResult = dichotomyRunner.run(sweData, sweTaskParameters, direction);
        dichotomyLogging.logEndOneDichotomy();

        if (dichotomyResult.isRaoFailed()) {
            final String lowestInvalidStepUrl = cneFileExportService.exportCneUrl(sweData, dichotomyResult, false, direction);
            return CompletableFuture.completedFuture(new SweDichotomyResult(direction, dichotomyResult, lowestInvalidStepUrl));
        }
        if (dichotomyResult.isInterrupted()) {
            return CompletableFuture.completedFuture(new SweDichotomyResult(direction, dichotomyResult, ""));
        }

        // Generate files specific for one direction (cne, cgm, voltage) and add them to the returned object (to create)
        final String zippedLastSecureCgmesUrl = cgmesExportService.buildAndExportLastSecureCgmesFiles(direction, sweData, dichotomyResult, sweTaskParameters);
        String zippedFirstUnsecureCgmesUrl = null;
        if (sweTaskParameters.isExportFirstUnsecureShiftedCGM()) {
            zippedFirstUnsecureCgmesUrl = cgmesExportService.buildAndExportFirstUnsecureCgmesFiles(direction, sweData, dichotomyResult, sweTaskParameters);
        }
        final String highestValidStepUrl = cneFileExportService.exportCneUrl(sweData, dichotomyResult, true, direction);
        final String lowestInvalidStepUrl = cneFileExportService.exportCneUrl(sweData, dichotomyResult, false, direction);
        final Optional<RaoResultWithVoltageMonitoring> voltageMonitoringResult = voltageCheckService.runVoltageCheck(sweData, dichotomyResult, sweTaskParameters, direction);
        outputService.buildAndExportVoltageDoc(direction, sweData, voltageMonitoringResult, sweTaskParameters);
        dichotomyLogging.generateSummaryEvents(direction, dichotomyResult, sweData, voltageMonitoringResult, sweTaskParameters, startTime);
        // fill response for one dichotomy
        return CompletableFuture.completedFuture(new SweDichotomyResult(direction, dichotomyResult, voltageMonitoringResult, zippedLastSecureCgmesUrl, zippedFirstUnsecureCgmesUrl, highestValidStepUrl, lowestInvalidStepUrl));
    }
}

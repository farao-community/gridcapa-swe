/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.swe.runner.app.parallelization;

import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.monitoring.voltage_monitoring.VoltageMonitoringResult;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.swe.runner.app.dichotomy.DichotomyDirection;
import com.farao_community.farao.swe.runner.app.dichotomy.DichotomyLogging;
import com.farao_community.farao.swe.runner.app.dichotomy.DichotomyRunner;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyResult;
import com.farao_community.farao.swe.runner.app.services.CgmesExportService;
import com.farao_community.farao.swe.runner.app.services.CneFileExportService;
import com.farao_community.farao.swe.runner.app.services.OutputService;
import com.farao_community.farao.swe.runner.app.services.VoltageCheckService;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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

    public DichotomyParallelizationWorker(DichotomyLogging dichotomyLogging, DichotomyRunner dichotomyRunner,
                                          VoltageCheckService voltageCheckService, CneFileExportService cneFileExportService,
                                          CgmesExportService cgmesExportService, OutputService outputService) {
        this.dichotomyLogging = dichotomyLogging;
        this.dichotomyRunner = dichotomyRunner;
        this.voltageCheckService = voltageCheckService;
        this.cneFileExportService = cneFileExportService;
        this.cgmesExportService = cgmesExportService;
        this.outputService = outputService;
    }

    @Async("threadPoolTaskExecutor")
    public CompletableFuture<SweDichotomyResult> runDichotomyForOneDirection(SweData sweData, DichotomyDirection direction) {
        // propagate in logs MDC the task requestId as an extra field to be able to send logs with calculation tasks.
        MDC.put("gridcapa-task-id", sweData.getId());
        DichotomyResult<RaoResponse> dichotomyResult = dichotomyRunner.run(sweData, direction);
        dichotomyLogging.logEndOneDichotomy(direction);
        // Generate files specific for one direction (cne, cgm, voltage) and add them to the returned object (to create)
        String zippedCgmesUrl = cgmesExportService.buildAndExportCgmesFiles(direction, sweData, dichotomyResult);
        String highestValidStepUrl = cneFileExportService.exportCneUrl(sweData, dichotomyResult, true, direction);
        String lowestInvalidStepUrl = cneFileExportService.exportCneUrl(sweData, dichotomyResult, false, direction);
        Optional<VoltageMonitoringResult> voltageMonitoringResult = voltageCheckService.runVoltageCheck(sweData, dichotomyResult, direction);
        outputService.buildAndExportVoltageDoc(direction, sweData, voltageMonitoringResult);
        dichotomyLogging.generateSummaryEvents(direction, dichotomyResult, sweData, voltageMonitoringResult);
        // fill response for one dichotomy
        return CompletableFuture.completedFuture(new SweDichotomyResult(direction, dichotomyResult, voltageMonitoringResult, zippedCgmesUrl, highestValidStepUrl, lowestInvalidStepUrl));
    }

}

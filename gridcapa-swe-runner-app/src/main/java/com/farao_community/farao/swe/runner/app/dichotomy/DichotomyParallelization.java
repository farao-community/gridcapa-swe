/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy;

import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import com.farao_community.farao.gridcapa_swe_commons.exception.SweInternalException;
import com.farao_community.farao.swe.runner.api.resource.SweResponse;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyResult;
import com.farao_community.farao.swe.runner.app.domain.SweTaskParameters;
import com.farao_community.farao.swe.runner.app.parallelization.DichotomyParallelizationWorker;
import com.farao_community.farao.swe.runner.app.parallelization.ExecutionResult;
import com.farao_community.farao.swe.runner.app.services.InterruptionService;
import com.farao_community.farao.swe.runner.app.services.OutputService;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@Service
public class DichotomyParallelization {
    private final DichotomyLogging dichotomyLogging;
    private final OutputService outputService;
    private final DichotomyParallelizationWorker worker;
    private final InterruptionService interruptionService;

    public DichotomyParallelization(DichotomyLogging dichotomyLogging, OutputService outputService, DichotomyParallelizationWorker worker, InterruptionService interruptionService) {

        this.dichotomyLogging = dichotomyLogging;
        this.outputService = outputService;
        this.worker = worker;
        this.interruptionService = interruptionService;
    }

    public SweResponse launchDichotomy(final SweData sweData,
                                       final SweTaskParameters sweTaskParameters,
                                       final OffsetDateTime startTime) {
        final ExecutionResult<SweDichotomyResult> executionResult = runAndGetSweDichotomyResults(sweData, sweTaskParameters, startTime);
        dichotomyLogging.logEndAllDichotomies();
        String ttcDocUrl = outputService.buildAndExportTtcDocument(sweData, executionResult);
        final boolean interrupted = executionResult.getResult().stream().anyMatch(SweDichotomyResult::isInterrupted);
        interruptionService.removeRunToBeInterrupted(sweData.getId());
        final boolean allRaoFailed = executionResult.getResult().stream().allMatch(SweDichotomyResult::isRaoFailed);
        return new SweResponse(sweData.getId(), ttcDocUrl, interrupted, allRaoFailed);
    }

    private ExecutionResult<SweDichotomyResult> runAndGetSweDichotomyResults(final SweData sweData,
                                                                             final SweTaskParameters sweTaskParameters,
                                                                             final OffsetDateTime startTime) {
        List<SweDichotomyResult> results = new ArrayList<>();
        List<Future<SweDichotomyResult>> futures = new ArrayList<>();
        try {
            if (sweTaskParameters.isRunDirectionEsFr()) {
                futures.add(worker.runDichotomyForOneDirection(sweData, sweTaskParameters, DichotomyDirection.ES_FR, startTime));
            }
            if (sweTaskParameters.isRunDirectionFrEs()) {
                futures.add(worker.runDichotomyForOneDirection(sweData, sweTaskParameters, DichotomyDirection.FR_ES, startTime));
            }
            if (sweTaskParameters.isRunDirectionEsPt()) {
                futures.add(worker.runDichotomyForOneDirection(sweData, sweTaskParameters, DichotomyDirection.ES_PT, startTime));
            }
            if (sweTaskParameters.isRunDirectionPtEs()) {
                futures.add(worker.runDichotomyForOneDirection(sweData, sweTaskParameters, DichotomyDirection.PT_ES, startTime));
            }

            for (Future<SweDichotomyResult> future : futures) {
                results.add(waitAndGet(future));
            }
        } catch (InterruptedException e) {
            futures.forEach(f -> f.cancel(true));
            Thread.currentThread().interrupt();
        }
        return new ExecutionResult<>(results);
    }

    private SweDichotomyResult waitAndGet(Future<SweDichotomyResult> dichotomy) throws InterruptedException {
        try {
            return dichotomy.get();
        } catch (ExecutionException e) {
            throw new SweInternalException("Error on dichotomy direction", e);
        }
    }
}

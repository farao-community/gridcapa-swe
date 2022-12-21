/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy;

import com.farao_community.farao.swe.runner.api.exception.SweInvalidDataException;
import com.farao_community.farao.swe.runner.api.resource.SweResponse;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyResult;
import com.farao_community.farao.swe.runner.app.parallelization.DichotomyParallelizationWorker;
import com.farao_community.farao.swe.runner.app.parallelization.ExecutionResult;
import com.farao_community.farao.swe.runner.app.services.OutputService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Service
public class DichotomyParallelization {
    private final DichotomyLogging dichotomyLogging;
    private final OutputService outputService;

    private final DichotomyParallelizationWorker worker;

    public DichotomyParallelization(DichotomyLogging dichotomyLogging, OutputService outputService, DichotomyParallelizationWorker worker) {

        this.dichotomyLogging = dichotomyLogging;
        this.outputService = outputService;
        this.worker = worker;
    }

    public SweResponse launchDichotomy(SweData sweData) {
        ExecutionResult<SweDichotomyResult> executionResult = runAndGetSweDichotomyResults(sweData);
        dichotomyLogging.logEndAllDichotomies();
        if (isAllEmptyResults(executionResult)) {
            return new SweResponse(sweData.getId(), null, null, null, null);
        } else {
            String ttcDocUrl = outputService.buildAndExportTtcDocument(sweData, executionResult);
            SweDichotomyResult esFrResult = getDichotomyResultByDirection(executionResult, DichotomyDirection.ES_FR);
            return new SweResponse(sweData.getId(), ttcDocUrl, esFrResult.getHighestValidStepUrl(), esFrResult.getLowestInvalidStepUrl(), esFrResult.getExportedCgmesUrl());
        }
    }

    private ExecutionResult<SweDichotomyResult> runAndGetSweDichotomyResults(SweData sweData) {
        List<SweDichotomyResult> results = new ArrayList<>();
        CompletableFuture<SweDichotomyResult> dichoEsFr = worker.runDichotomyForOneDirection(sweData, DichotomyDirection.ES_FR);
        CompletableFuture<SweDichotomyResult> dichoFrEs = worker.runDichotomyForOneDirection(sweData, DichotomyDirection.FR_ES);
        CompletableFuture<SweDichotomyResult> dichoEsPt = worker.runDichotomyForOneDirection(sweData, DichotomyDirection.ES_PT);
        CompletableFuture<SweDichotomyResult> dichoPtEs = worker.runDichotomyForOneDirection(sweData, DichotomyDirection.PT_ES);
        try {
            results.add(waitAndGet(dichoEsFr, DichotomyDirection.ES_FR));
            results.add(waitAndGet(dichoFrEs, DichotomyDirection.FR_ES));
            results.add(waitAndGet(dichoEsPt, DichotomyDirection.ES_PT));
            results.add(waitAndGet(dichoPtEs, DichotomyDirection.PT_ES));
        } catch (InterruptedException e) {
            dichoEsFr.cancel(true);
            dichoFrEs.cancel(true);
            dichoEsPt.cancel(true);
            dichoPtEs.cancel(true);
        }
        return new ExecutionResult<>(results);
    }

    private SweDichotomyResult waitAndGet(CompletableFuture<SweDichotomyResult> dichotomy, DichotomyDirection direction) throws InterruptedException {
        try {
            return dichotomy.get();
        } catch (InterruptedException e) {
            throw e;
        } catch (ExecutionException e) {
            dichotomyLogging.logErrorOnDirection(direction, e);
        }
        return new SweDichotomyResult(direction, null, null, null, null, null);
    }

    private SweDichotomyResult getDichotomyResultByDirection(ExecutionResult<SweDichotomyResult> executionResult, DichotomyDirection direction) {
        Optional<SweDichotomyResult> result = executionResult.getResult().stream().filter(dichotomyResult -> dichotomyResult.getDichotomyDirection() == direction).findFirst();
        if (result.isEmpty()) {
            throw new SweInvalidDataException(String.format("No dichotomy result found for direction: [ %s ]", direction.name()));
        }
        return result.get();
    }

    private boolean isAllEmptyResults(ExecutionResult<SweDichotomyResult> executionResult) {
        List<SweDichotomyResult> results = executionResult.getResult();
        return results.isEmpty() || results.stream().allMatch(sweDR -> sweDR.getDichotomyResult() == null && sweDR.getExportedCgmesUrl() == null);
    }
}

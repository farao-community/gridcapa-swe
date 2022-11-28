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
        ExecutionResult<SweDichotomyResult> executionResult = null;
        CompletableFuture<SweDichotomyResult> dichoEsFr = worker.runDichotomyForOneDirection(sweData, DichotomyDirection.ES_FR);
        CompletableFuture<SweDichotomyResult> dichoFrEs = worker.runDichotomyForOneDirection(sweData, DichotomyDirection.FR_ES);
        //CompletableFuture<SweDichotomyResult> dichoEsPt = worker.runDichotomyForOneDirection(sweData, DichotomyDirection.ES_PT);
        //CompletableFuture<SweDichotomyResult> dichoPtEs = worker.runDichotomyForOneDirection(sweData, DichotomyDirection.PT_ES);
        List<SweDichotomyResult> results = new ArrayList<>();
        waitAndAddToReults(dichoEsFr, results, DichotomyDirection.ES_FR);
        waitAndAddToReults(dichoFrEs, results, DichotomyDirection.FR_ES);
        //waitAndAddToReults(dichoEsPt, results, DichotomyDirection.ES_PT)
        //waitAndAddToReults(dichoPtEs, results, DichotomyDirection.PT_ES);
        executionResult = new ExecutionResult<>(results);

        dichotomyLogging.logEndAllDichotomies();
        String ttcDocUrl = outputService.buildAndExportTtcDocument(sweData, executionResult);
        String voltageEsFrZipUrl = outputService.buildAndExportVoltageDoc(DichotomyDirection.ES_FR, sweData, executionResult);
        String voltageFrEsZipUrl = outputService.buildAndExportVoltageDoc(DichotomyDirection.FR_ES, sweData, executionResult);
        SweDichotomyResult esFrResult = getDichotomyResultByDirection(executionResult, DichotomyDirection.ES_FR);
        SweDichotomyResult frEsResult = getDichotomyResultByDirection(executionResult, DichotomyDirection.FR_ES);
        return  new SweResponse(sweData.getId(), ttcDocUrl, voltageEsFrZipUrl, voltageFrEsZipUrl, esFrResult.getExportedCgmesUrl(), frEsResult.getExportedCgmesUrl(),
                esFrResult.getHighestValidStepUrl(), esFrResult.getLowestInvalidStepUrl(), frEsResult.getHighestValidStepUrl(), frEsResult.getLowestInvalidStepUrl());
    }

    private void waitAndAddToReults(CompletableFuture<SweDichotomyResult> dichotomy, List<SweDichotomyResult> results, DichotomyDirection direction) {
        try {
            results.add(dichotomy.get());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            dichotomyLogging.logErrorOnDirection(direction, e);
        }
    }

    private SweDichotomyResult getDichotomyResultByDirection(ExecutionResult<SweDichotomyResult> executionResult, DichotomyDirection direction) {
        Optional<SweDichotomyResult> result = executionResult.getResult().stream().filter(dichotomyResult -> dichotomyResult.getDichotomyDirection() == direction).findFirst();
        if (result.isEmpty()) {
            throw new SweInvalidDataException(String.format("No dichotomy result found for direction: [ %s ]", direction.name()));
        }
        return result.get();
    }
}

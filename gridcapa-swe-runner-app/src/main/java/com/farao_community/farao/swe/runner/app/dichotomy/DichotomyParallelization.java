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
import com.farao_community.farao.swe.runner.app.parallelization.DichotomyParallelizationWorker;
import com.farao_community.farao.swe.runner.app.parallelization.ExecutionResult;
import com.farao_community.farao.swe.runner.app.services.OutputService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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
        String ttcDocUrl = outputService.buildAndExportTtcDocument(sweData, executionResult);
        return new SweResponse(sweData.getId(), ttcDocUrl);
    }

    private ExecutionResult<SweDichotomyResult> runAndGetSweDichotomyResults(SweData sweData) {
        List<SweDichotomyResult> results = new ArrayList<>();
        List<Future<SweDichotomyResult>> futures = new ArrayList<>();
        try {
            futures.add(worker.runDichotomyForOneDirection(sweData, DichotomyDirection.ES_FR));
            futures.add(worker.runDichotomyForOneDirection(sweData, DichotomyDirection.FR_ES));
            futures.add(worker.runDichotomyForOneDirection(sweData, DichotomyDirection.ES_PT));
            futures.add(worker.runDichotomyForOneDirection(sweData, DichotomyDirection.PT_ES));
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

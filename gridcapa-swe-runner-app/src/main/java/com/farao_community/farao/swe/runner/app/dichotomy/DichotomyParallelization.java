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
import com.farao_community.farao.swe.runner.api.exception.SweInvalidDataException;
import com.farao_community.farao.swe.runner.api.resource.ProcessType;
import com.farao_community.farao.swe.runner.api.resource.SweResponse;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyResult;
import com.farao_community.farao.swe.runner.app.parallelization.ExecutionResult;
import com.farao_community.farao.swe.runner.app.parallelization.ParallelExecution;
import com.farao_community.farao.swe.runner.app.services.CneFileExportService;
import com.farao_community.farao.swe.runner.app.services.OutputService;
import com.farao_community.farao.swe.runner.app.services.VoltageCheckService;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Service
public class DichotomyParallelization {

    private final DichotomyLogging dichotomyLogging;
    private final DichotomyRunner dichotomyRunner;
    private final OutputService outputService;
    private final VoltageCheckService voltageCheckService;
    private final CneFileExportService cneFileExportService;

    public DichotomyParallelization(DichotomyLogging dichotomyLogging, DichotomyRunner dichotomyRunner,
                                    OutputService outputService, VoltageCheckService voltageCheckService,
                                    CneFileExportService cneFileExportService) {
        this.dichotomyLogging = dichotomyLogging;
        this.dichotomyRunner = dichotomyRunner;
        this.outputService = outputService;
        this.voltageCheckService = voltageCheckService;
        this.cneFileExportService = cneFileExportService;
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
        SweDichotomyResult esFrResult = getByDirection(executionResult, DichotomyDirection.ES_FR);
        return  new SweResponse(sweData.getId(), ttcDocUrl, voltageEsFrZipUrl, esFrResult.getHighestValidStepUrl(), esFrResult.getLowestInvalidStepUrl());
    }

    SweDichotomyResult runDichotomyForOneDirection(SweData sweData, DichotomyDirection direction) {
        DichotomyResult<RaoResponse> dichotomyResult = dichotomyRunner.run(sweData, direction);
        dichotomyLogging.logEndOneDichotomy(direction);
        // Generate files specific for one direction (cne, cgm, voltage) and add them to the returned object (to create)
        String highestValidStepUrl = cneFileExportService.exportCneUrl(sweData, dichotomyResult.getHighestValidStep().getRaoResult(), true, ProcessType.D2CC, direction);
        String lowestInvalidStepUrl = cneFileExportService.exportCneUrl(sweData, dichotomyResult.getLowestInvalidStep().getRaoResult(), false, ProcessType.D2CC, direction);
        Optional<VoltageMonitoringResult> voltageMonitoringResult = voltageCheckService.runVoltageCheck(sweData, dichotomyResult, direction);
        // fill response for one dichotomy
        return new SweDichotomyResult(direction, dichotomyResult, voltageMonitoringResult, highestValidStepUrl, lowestInvalidStepUrl);
    }

    private SweDichotomyResult getByDirection(ExecutionResult<SweDichotomyResult> executionResult, DichotomyDirection direction) {
        Optional<SweDichotomyResult> result = executionResult.getResult().stream().filter(dichotomyResult -> dichotomyResult.getDichotomyDirection() == direction).findFirst();
        if (result.isEmpty()) {
            throw new SweInvalidDataException(String.format("No dichotomy result found for direction: [ %s ]", direction.name()));
        }
        return result.get();
    }
}

/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.monitoring.voltage_monitoring.VoltageMonitoring;
import com.farao_community.farao.monitoring.voltage_monitoring.VoltageMonitoringResult;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.swe.runner.api.resource.SweResponse;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyResult;
import com.farao_community.farao.swe.runner.app.parallelization.ExecutionResult;
import com.farao_community.farao.swe.runner.app.parallelization.ParallelExecution;
import com.farao_community.farao.swe.runner.app.services.OutputService;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
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

    public DichotomyParallelization(DichotomyLogging dichotomyLogging, DichotomyRunner dichotomyRunner, OutputService outputService) {
        this.dichotomyLogging = dichotomyLogging;
        this.dichotomyRunner = dichotomyRunner;
        this.outputService = outputService;
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
        String voltageEsFrZipUrl = outputService.buildAndExportEsFrVoltageDoc(sweData, executionResult);
        return  new SweResponse(sweData.getId(), ttcDocUrl, voltageEsFrZipUrl);
    }

    SweDichotomyResult runDichotomyForOneDirection(SweData sweData, DichotomyDirection direction) {
        DichotomyResult<RaoResponse> dichotomyResult = dichotomyRunner.run(sweData, direction);
        dichotomyLogging.logEndOneDichotomy(direction);
        // Generate files specific for one direction (cne, cgm, voltage) and add them to the returned object (to create)
        Optional<VoltageMonitoringResult> voltageMonitoringResult = runVoltageCheck(sweData, dichotomyResult, direction);
        // fill response for one dichotomy
        return new SweDichotomyResult(direction, dichotomyResult, voltageMonitoringResult);
    }

    private Optional<VoltageMonitoringResult> runVoltageCheck(SweData sweData, DichotomyResult<RaoResponse> dichotomyResult, DichotomyDirection direction) {

        if (direction == DichotomyDirection.ES_FR || direction == DichotomyDirection.FR_ES) {
            Crac crac = sweData.getCracFrEs();
            VoltageMonitoring voltageMonitoring = new VoltageMonitoring(crac, sweData.getNetwork(), dichotomyResult.getHighestValidStep().getRaoResult());
            return Optional.of(voltageMonitoring.run(LoadFlow.find().getName(), LoadFlowParameters.load(), 4));
        }
        return Optional.empty();
    }
}

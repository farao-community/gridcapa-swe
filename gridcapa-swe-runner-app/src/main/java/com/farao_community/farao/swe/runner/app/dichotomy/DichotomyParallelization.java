/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy;

import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.parallelization.ExecutionResult;
import com.farao_community.farao.swe.runner.app.parallelization.ParallelExecution;
import com.farao_community.farao.swe.runner.app.utils.Direction;
import org.springframework.stereotype.Service;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Service
public class DichotomyParallelization {

    private final DichotomyRunner dichotomyRunner;

    public DichotomyParallelization(DichotomyRunner dichotomyRunner) {
        this.dichotomyRunner = dichotomyRunner;
    }

    public void launchDichotomy(SweData sweData) {
        final ExecutionResult executionResult = ParallelExecution
                .of(() -> dichotomyRunner.run(sweData, Direction.ES_FR))
                // .and(() -> dichotomyRunner.run(sweData, Direction.FR_ES))
                // .and(() -> dichotomyRunner.run(sweData, Direction.ES_PT))
                // .and(() -> dichotomyRunner.run(sweData, Direction.PT_ES))
                .close();
    }
}

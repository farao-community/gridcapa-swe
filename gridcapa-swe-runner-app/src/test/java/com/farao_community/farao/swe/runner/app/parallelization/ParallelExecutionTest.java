/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.parallelization;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@SpringBootTest
class ParallelExecutionTest {

    @Test
    void testParallelization() {
        final ExecutionResult executionResult = ParallelExecution.of(() -> slowService(1))
                .and(() -> slowService(2))
                .and(() -> slowService(3))
                .close();

        int val = executionResult.get(0);
        int val2 = executionResult.get(1);
        int val3 = executionResult.get(2);

        assertEquals(6, val + val2 + val3);
    }

    private static Integer slowService(final int i) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return i;
    }
}

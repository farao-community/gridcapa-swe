/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 * @author Daniel Thirion {@literal <daniel.thirion at rte-france.com>}
 */
@SpringBootTest
class InterruptionServiceTest {

    @Mock
    private Logger businessLogger;

    @InjectMocks
    private InterruptionService interruptionService;

    @BeforeEach
    void setUp() {
        interruptionService = new InterruptionService(businessLogger);
    }

    private static final String TASK_ID = "tastkId";

    @Test
    void shouldMarkTaskForSoftInterruption() {
        interruptionService.softInterrupt().accept(TASK_ID);

        assertTrue(interruptionService.shouldRunBeInterruptedSoftly(TASK_ID));
        verify(businessLogger).warn("Soft interruption requested");
    }

    @Test
    void shouldNotInterruptTaskThatWasNotMarked() {
        assertFalse(interruptionService.shouldRunBeInterruptedSoftly(TASK_ID));
    }

    @Test
    void shouldRemoveTaskFromInterruptionList() {
        interruptionService.softInterrupt().accept(TASK_ID);
        assertTrue(interruptionService.shouldRunBeInterruptedSoftly(TASK_ID));

        interruptionService.removeRunToBeInterrupted(TASK_ID);
        assertFalse(interruptionService.shouldRunBeInterruptedSoftly(TASK_ID));
    }
}

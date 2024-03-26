/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.function.StreamBridge;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@SpringBootTest
class InterruptionServiceTest {

    @Autowired
    private InterruptionService interruptionService;

    @MockBean
    private StreamBridge streamBridge;

    @Test
    void softInterruption() {
        final String taskId = "testTask";

        assertFalse(interruptionService.shouldTaskBeInterruptedSoftly(taskId));

        interruptionService.softInterrupt().accept(taskId);

        Mockito.verify(streamBridge, Mockito.times(1)).send("stop-rao", taskId);

        assertTrue(interruptionService.shouldTaskBeInterruptedSoftly(taskId));

    }
}

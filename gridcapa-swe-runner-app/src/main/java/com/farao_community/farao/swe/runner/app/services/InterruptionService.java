/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.dichotomy.api.InterruptionStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@Service
public class InterruptionService implements InterruptionStrategy {
    private static final Logger LOGGER = LoggerFactory.getLogger(InterruptionService.class);
    private final Logger businessLogger;
    private static final String STOP_RAO_BINDING = "stop-rao";

    private final StreamBridge streamBridge;
    private final List<String> tasksToInterruptSoftly;

    public InterruptionService(Logger businessLogger, StreamBridge streamBridge) {
        this.businessLogger = businessLogger;
        this.streamBridge = streamBridge;
        this.tasksToInterruptSoftly = new ArrayList<>();
    }

    @Bean
    public Consumer<String> softInterrupt() {
        return this::activateSoftInterruptionFlag;
    }

    private void activateSoftInterruptionFlag(String taskId) {
        LOGGER.info("Soft interruption requested for task {}", taskId);
        businessLogger.warn("Soft interruption requested");
        streamBridge.send(STOP_RAO_BINDING, taskId);
        tasksToInterruptSoftly.add(taskId);
    }

    @Override
    public boolean shouldTaskBeInterruptedSoftly(String taskId) {
        boolean taskShouldBeInterrupted = tasksToInterruptSoftly.contains(taskId);

        if (taskShouldBeInterrupted) {
            LOGGER.info("Task {} should be interrupted softly", taskId);
        } else {
            LOGGER.info("Task {} doesn't need to be interrupted softly", taskId);
        }
        return taskShouldBeInterrupted;
    }

    public void removeTaskToBeInterrupted(String taskId) {
        tasksToInterruptSoftly.remove(taskId);
    }
}

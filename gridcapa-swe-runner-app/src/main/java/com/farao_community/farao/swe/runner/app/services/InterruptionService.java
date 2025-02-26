/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.dichotomy.api.InterruptionStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final List<String> runsToInterruptSoftly;

    public InterruptionService(final Logger businessLogger) {
        this.businessLogger = businessLogger;
        this.runsToInterruptSoftly = new ArrayList<>();
    }

    @Bean
    public Consumer<String> softInterrupt() {
        return this::activateSoftInterruptionFlag;
    }

    private void activateSoftInterruptionFlag(final String runId) {
        LOGGER.info("Soft interruption requested for run {}", runId);
        runsToInterruptSoftly.add(runId);
    }

    @Override
    public boolean shouldRunBeInterruptedSoftly(final String runId) {
        final boolean runShouldBeInterrupted = runsToInterruptSoftly.contains(runId);
        if (runShouldBeInterrupted) {
            businessLogger.warn("Soft interruption requested for run");
            LOGGER.info("Run {} should be interrupted softly", runId);
        } else {
            LOGGER.info("Run {} doesn't need to be interrupted softly", runId);
        }
        return runShouldBeInterrupted;
    }

    public void removeRunToBeInterrupted(final String runId) {
        runsToInterruptSoftly.remove(runId);
    }
}

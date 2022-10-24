/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy;

import com.farao_community.farao.swe.runner.app.configurations.DichotomyConfiguration.Parameters;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Service
public class DichotomyLogging {

    private final Logger eventsLogger;

    public DichotomyLogging(Logger eventsLogger) {
        this.eventsLogger = eventsLogger;
    }

    public void logStartDichotomy(DichotomyDirection direction, Parameters parameters) {
        eventsLogger.info("{} - Start dichotomy : minimum dichotomy index: {}, maximum dichotomy index: {}, dichotomy precision: {}", direction, parameters.getMinValue(), parameters.getMaxValue(), parameters.getPrecision());
    }

    public void logEndOneDichotomy(DichotomyDirection direction) {
        eventsLogger.info("{} - Dichotomy finished", direction);
    }

    public void logEndAllDichotomies() {
        eventsLogger.info("All - Dichotomies are done");
    }
}

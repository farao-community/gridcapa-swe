/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.configurations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes the named business {@link Logger} bean used to publish SWE calculation events. This logger is injected
 * into services such as {@link com.farao_community.farao.swe.runner.app.dichotomy.DichotomyLogging} and
 * {@link com.farao_community.farao.swe.runner.app.services.SweRunner} so that business messages are separated from
 * technical logs.
 *
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Configuration
class EventsLogger {

    @Bean
    public Logger getLogger() {
        return LoggerFactory.getLogger("SWE_BUSINESS_LOGGER");
    }

}

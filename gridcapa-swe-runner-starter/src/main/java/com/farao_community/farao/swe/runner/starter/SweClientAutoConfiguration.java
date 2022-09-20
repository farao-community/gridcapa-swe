/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.starter;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Configuration
@EnableConfigurationProperties(SweClientProperties.class)
public class SweClientAutoConfiguration {
    private final SweClientProperties clientProperties;

    public SweClientAutoConfiguration(SweClientProperties clientProperties) {
        this.clientProperties = clientProperties;
    }

    @Bean
    public SweClient sweClient(AmqpTemplate amqpTemplate) {
        return new SweClient(amqpTemplate, clientProperties);
    }

}

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
 * Spring Boot auto-configuration that exposes a ready-to-use {@link SweClient} bean for sending SWE runner requests.
 * It binds external {@link SweClientProperties} and injects the application {@link AmqpTemplate} used by the client.
 *
 * @see SweClient client created by this configuration
 * @see SweClientProperties broker settings bound from {@code swe-runner.*}
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

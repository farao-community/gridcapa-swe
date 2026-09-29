/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app;

import com.farao_community.farao.swe.runner.app.configurations.UrlConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Spring Boot entry point for the SWE runner application. It starts the application context that wires the runner
 * services, and explicitly enables {@link UrlConfiguration} property binding used by the orchestration layer.
 *
 * <p>Main collaborators include {@link org.springframework.boot.SpringApplication}, which boots the context, and
 * {@link com.farao_community.farao.swe.runner.app.services.SweRunner}, the business service exposed once the
 * application has started.</p>
 *
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@SuppressWarnings("HideUtilityClassConstructor")
@SpringBootApplication
@EnableConfigurationProperties(UrlConfiguration.class)
public class SweApplication {
    public static void main(String[] args) {
        SpringApplication.run(SweApplication.class, args);
    }
}

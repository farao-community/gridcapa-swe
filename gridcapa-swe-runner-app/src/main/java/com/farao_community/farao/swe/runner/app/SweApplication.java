/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app;

import com.farao_community.farao.swe.runner.app.configurations.DataFixConfiguration;
import com.farao_community.farao.swe.runner.app.configurations.UrlConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@SuppressWarnings("HideUtilityClassConstructor")
@SpringBootApplication
@EnableConfigurationProperties({UrlConfiguration.class, DataFixConfiguration.class})
public class SweApplication {
    public static void main(String[] args) {
        SpringApplication.run(SweApplication.class, args);
    }
}

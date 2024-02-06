/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@SuppressWarnings("HideUtilityClassConstructor")
@SpringBootApplication
@ComponentScan(basePackages = {"com.farao_community.farao.swe.runner.app", "com.farao_community.farao.gridcapa_swe_commons"})
public class SweApplication {
    public static void main(String[] args) {
        SpringApplication.run(SweApplication.class, args);
    }
}

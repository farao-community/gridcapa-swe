/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@SuppressWarnings("HideUtilityClassConstructor")
@SpringBootApplication
public class SweApplication {
    public static void main(String[] args) {
        SpringApplication.run(SweApplication.class, args);
    }
}

/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.configurations;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties("swe-runner")
public class UrlConfiguration {
    private final List<String> whitelist = new ArrayList<>();
    private String interruptServerUrl;

    public List<String> getWhitelist() {
        return whitelist;
    }

    public String getInterruptServerUrl() {
        return interruptServerUrl;
    }

    public void setInterruptServerUrl(String interruptServerUrl) {
        this.interruptServerUrl = interruptServerUrl;
    }
}

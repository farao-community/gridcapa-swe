/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.configurations;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the external URL settings configured under {@code swe-runner}. It is injected into
 * {@link com.farao_community.farao.swe.runner.app.utils.UrlValidationService} to validate inbound file URLs and
 * into {@link com.farao_community.farao.swe.runner.app.services.SweRunner} to query the interruption endpoint.
 *
 * <p>The {@code whitelist} defines the allowed URL prefixes for remote resources, while
 * {@code interruptServerUrl} is the base URL used to check whether a run must stop.</p>
 *
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */
@ConfigurationProperties("swe-runner")
public class UrlConfiguration {
    private final List<String> whitelist;
    private final String interruptServerUrl;

    public UrlConfiguration(List<String> whitelist, String interruptServerUrl) {
        this.whitelist = whitelist == null ? new ArrayList<>() : whitelist;
        this.interruptServerUrl = interruptServerUrl;
    }

    public List<String> getWhitelist() {
        return whitelist;
    }

    public String getInterruptServerUrl() {
        return interruptServerUrl;
    }
}

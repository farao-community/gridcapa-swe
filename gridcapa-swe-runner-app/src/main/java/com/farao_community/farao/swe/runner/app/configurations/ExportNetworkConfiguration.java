/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.configurations;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Configuration
@ConfigurationProperties(prefix = "swe-runner")
public class ExportNetworkConfiguration {

    private boolean exportFailedNetwork = false;

    public boolean isExportFailedNetwork() {
        return exportFailedNetwork;
    }

    public void setExportFailedNetwork(boolean exportFailedNetwork) {
        this.exportFailedNetwork = exportFailedNetwork;
    }
}

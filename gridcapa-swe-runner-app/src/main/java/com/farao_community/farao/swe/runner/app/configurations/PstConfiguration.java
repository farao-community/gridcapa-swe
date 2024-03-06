/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
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
@ConfigurationProperties(prefix = "swe-runner.pst")
public class PstConfiguration {
    private String pst1Id;
    private String pst2Id;

    public String getPst1Id() {
        return pst1Id;
    }

    public String getPst2Id() {
        return pst2Id;
    }

    public void setPst1Id(String pst1Id) {
        this.pst1Id = pst1Id;
    }

    public void setPst2Id(String pst2Id) {
        this.pst2Id = pst2Id;
    }
}

/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.configurations;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Configuration
@ConfigurationProperties(prefix = "swe-runner.pst")
public class PstConfiguration {
    private List<String> pstIds;

    private List<PstToRegulate> pstsToRegulate;

    public List<String> getPstIds() {
        return pstIds;
    }

    public void setPstIds(List<String> pstIds) {
        this.pstIds = pstIds;
    }

    public List<PstToRegulate> getPstsToRegulate() {
        return pstsToRegulate;
    }

    public void setPstsToRegulate(final List<PstToRegulate>  pstsToRegulate) {
        this.pstsToRegulate = pstsToRegulate;
    }

    public record PstToRegulate(String pstId, String monitoredLineId) {
        @Override
        public boolean equals(final Object o) {
            return EqualsBuilder.reflectionEquals(this, o);
        }

        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }
    }
}

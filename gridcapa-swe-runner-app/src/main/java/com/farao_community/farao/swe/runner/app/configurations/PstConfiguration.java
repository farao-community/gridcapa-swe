/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.configurations;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Objects;

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

    public static class PstToRegulate {
        private String pstId;
        private String monitoredLineId;

        public String getPstId() {
            return pstId;
        }

        public void setPstId(final String pstId) {
            this.pstId = pstId;
        }

        public String getMonitoredLineId() {
            return monitoredLineId;
        }

        public void setMonitoredLineId(final String monitoredLineId) {
            this.monitoredLineId = monitoredLineId;
        }

        @Override
        public boolean equals(final Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final PstToRegulate that = (PstToRegulate) o;
            return Objects.equals(pstId, that.pstId) && Objects.equals(monitoredLineId, that.monitoredLineId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(pstId, monitoredLineId);
        }
    }
}

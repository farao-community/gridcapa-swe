/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_swe_commons.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */
@Configuration
@ConfigurationProperties(prefix = "swe-commons")
public class ProcessConfiguration {

    private String zoneId;
    private Integer shiftMaxIterationNumber;
    private Map<String, String> modelingAuthoritySet;

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public Integer getShiftMaxIterationNumber() {
        return shiftMaxIterationNumber;
    }

    public void setShiftMaxIterationNumber(Integer shiftMaxIterationNumber) {
        this.shiftMaxIterationNumber = shiftMaxIterationNumber;
    }

    public Map<String, String> getModelingAuthoritySet() {
        return modelingAuthoritySet;
    }

    public void setModelingAuthoritySet(Map<String, String> modelingAuthoritySet) {
        this.modelingAuthoritySet = modelingAuthoritySet;
    }

}

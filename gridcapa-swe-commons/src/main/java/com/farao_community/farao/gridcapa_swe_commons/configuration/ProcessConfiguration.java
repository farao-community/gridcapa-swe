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
 * Spring-bound configuration shared by SWE common components to parameterize balancing and export behavior across the Spain-Portugal-France capacity-calculation flow.
 * {@link com.farao_community.farao.gridcapa_swe_commons.shift.SweNetworkShifter} uses {@code shiftMaxIterationNumber} to cap balancing-adjustment retries, while downstream exporters consume {@code zoneId} and {@code modelingAuthorityMap} to tag outputs with the expected regional and modeling-authority identifiers.
 *
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 * @see com.farao_community.farao.gridcapa_swe_commons.shift.SweNetworkShifter uses the iterative shift settings
 */
@Configuration
@ConfigurationProperties(prefix = "swe-commons")
public class ProcessConfiguration {

    private String zoneId;
    private Integer shiftMaxIterationNumber;
    private Map<String, String> modelingAuthorityMap;

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

    public Map<String, String> getModelingAuthorityMap() {
        return modelingAuthorityMap;
    }

    public void setModelingAuthorityMap(Map<String, String> modelingAuthorityMap) {
        this.modelingAuthorityMap = modelingAuthorityMap;
    }

}

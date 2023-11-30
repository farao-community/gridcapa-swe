/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_swe_commons.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */
@Configuration
public class ProcessConfiguration {

    @Value("${swe-commons.zone-id}")
    private String zoneId;

    @Value("${swe-commons.shift-max-iteration-number}")
    private Integer shiftMaxIterationNumber;

    public String getZoneId() {
        return zoneId;
    }

    public Integer getShiftMaxIterationNumber() {
        return shiftMaxIterationNumber;
    }
}

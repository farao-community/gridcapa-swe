/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.voltage.json;

import com.farao_community.farao.data.crac_api.Instant;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */
public class VoltageCheckConstraintElement {

    private final String networkElementId;
    private final Instant instant;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String contingencyId;
    private final Double minVoltage;
    private final Double maxVoltage;
    private final Double lowerBound;
    private final Double upperBound;

    public VoltageCheckConstraintElement(String networkElementId,
                                         Instant instant,
                                         String contingencyId,
                                         Double minVoltage,
                                         Double maxVoltage,
                                         Double lowerBound,
                                         Double upperBound) {
        this.networkElementId = Objects.requireNonNull(networkElementId,
                "The value of networkElementId cannot be null in VoltageCheckConstraintElement");
        this.instant = Objects.requireNonNull(instant,
                "The value of instant cannot be null in VoltageCheckConstraintElement");
        this.contingencyId = contingencyId;
        this.minVoltage = Objects.requireNonNull(minVoltage,
                "The value of minVoltage cannot be null in VoltageCheckConstraintElement");
        this.maxVoltage = Objects.requireNonNull(maxVoltage,
                "The value of maxVoltage cannot be null in VoltageCheckConstraintElement");
        this.lowerBound = Objects.requireNonNull(lowerBound,
                "The value of lowerBound cannot be null in VoltageCheckConstraintElement");
        this.upperBound = Objects.requireNonNull(upperBound,
                "The value of upperBound cannot be null in VoltageCheckConstraintElement");
    }

    public String getNetworkElementId() {
        return networkElementId;
    }

    public Instant getInstant() {
        return instant;
    }

    public String getContingencyId() {
        return contingencyId;
    }

    public Double getMinVoltage() {
        return minVoltage;
    }

    public Double getMaxVoltage() {
        return maxVoltage;
    }

    public Double getLowerBound() {
        return lowerBound;
    }

    public Double getUpperBound() {
        return upperBound;
    }
}

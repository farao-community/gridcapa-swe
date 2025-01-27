/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.voltage.json;

import com.powsybl.openrao.data.crac.api.cnec.Cnec;

import java.util.List;
import java.util.Objects;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */
public class VoltageCheckResult {

    private final Cnec.SecurityStatus status;
    private final List<VoltageCheckConstraintElement> constraintElements;

    public VoltageCheckResult(Cnec.SecurityStatus status,
                              List<VoltageCheckConstraintElement> constraintElements) {
        this.status = Objects.requireNonNull(status,
                "The value of status cannot be null in VoltageCheckResult");
        this.constraintElements = Objects.requireNonNull(constraintElements,
                "The value of constraintElements cannot be null in VoltageCheckResult");
    }

    public Cnec.SecurityStatus getStatus() {
        return status;
    }

    public List<VoltageCheckConstraintElement> getConstraintElements() {
        return constraintElements;
    }
}

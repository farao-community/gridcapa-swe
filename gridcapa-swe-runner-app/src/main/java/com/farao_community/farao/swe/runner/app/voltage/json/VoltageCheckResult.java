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
 * JSON view of the voltage-monitoring outcome exported by the SWE runner. It carries the overall security status and
 * the list of violated voltage constraints detected on curative instants.
 *
 * @see VoltageCheckConstraintElement DTO used for each reported violation
 * @see com.farao_community.farao.swe.runner.app.voltage.VoltageResultMapper assembles this result from RAO monitoring data
 * @see FailureVoltageCheckResult fallback payload used when no monitoring result is available
 *
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

/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.voltage.json;

import java.util.Collections;
import java.util.List;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */
public class FailureVoltageCheckResult {

    private final List<VoltageCheckConstraintElement> constraintElements = Collections.emptyList();

    public String getIsSecure() {
        return "FAILURE";
    }

    public List<VoltageCheckConstraintElement> getConstraintElements() {
        return constraintElements;
    }
}

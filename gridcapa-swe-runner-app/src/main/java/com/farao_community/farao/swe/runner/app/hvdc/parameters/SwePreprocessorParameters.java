/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.swe.runner.app.hvdc.parameters;

import java.util.Set;

/**
 * @author Peter Mitri {@literal <peter.mitri@rte-france.com>}
 */
public class SwePreprocessorParameters {
    Set<HvdcCreationParameters> hvdcCreationParametersSet;

    public SwePreprocessorParameters(Set<HvdcCreationParameters> hvdcCreationParametersSet) {
        this.hvdcCreationParametersSet = hvdcCreationParametersSet;
    }

    public Set<HvdcCreationParameters> getHvdcCreationParametersSet() {
        return hvdcCreationParametersSet;
    }
}

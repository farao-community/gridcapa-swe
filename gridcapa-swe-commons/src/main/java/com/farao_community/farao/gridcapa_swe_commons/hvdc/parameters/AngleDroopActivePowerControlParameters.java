/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_swe_commons.hvdc.parameters;

import java.util.Objects;

/**
 * Holds the optional angle-droop control settings attached to a created SWE HVDC line.
 * Embedded in {@link HvdcCreationParameters} and consumed by {@link com.farao_community.farao.gridcapa_swe_commons.hvdc.HvdcLinkProcessor} when it adds the Powsybl HVDC angle-droop extension.
 * {@code p0} is the reference active-power target passed to the extension, while {@code droop} is the controller gain used to translate angle deviations into active-power adjustments.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class AngleDroopActivePowerControlParameters {
    private float p0;
    private float droop;

    public AngleDroopActivePowerControlParameters(Float p0, Float droop) {
        Objects.requireNonNull(p0);
        Objects.requireNonNull(droop);
        this.p0 = p0;
        this.droop = droop;
    }

    public float getP0() {
        return p0;
    }

    public float getDroop() {
        return droop;
    }
}

/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.swe.runner.app.hvdc.parameters;

import java.util.Objects;

/**
 * Parameters of angle droop active power control on an HVDC line
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

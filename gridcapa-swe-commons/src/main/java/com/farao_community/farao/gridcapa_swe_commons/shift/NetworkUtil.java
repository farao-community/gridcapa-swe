/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_swe_commons.shift;

import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Injection;

/**
 * Small network helpers shared by SWE scaling components when they need robust generator weights and connectivity checks.
 * Used by {@link ZonalScalableProvider} to build the fallback French proportional scalable from IIDM injections that are actually usable in the calculation.
 * {@code MINIMAL_ABS_POWER_VALUE} is a floor applied by {@link #pseudoTargetP(Generator)} so generators with zero target active power still receive a strictly positive scaling weight.
 *
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public final class NetworkUtil {

    private static final double MINIMAL_ABS_POWER_VALUE = 1e-5;

    private NetworkUtil() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static double pseudoTargetP(Generator generator) {
        return Math.max(MINIMAL_ABS_POWER_VALUE, Math.abs(generator.getTargetP()));
    }

    public static boolean isCorrect(Injection<?> injection) {
        return injection != null &&
               injection.getTerminal().isConnected() &&
               injection.getTerminal().getBusView().getBus() != null &&
               injection.getTerminal().getBusView().getBus().isInMainSynchronousComponent();
    }
}

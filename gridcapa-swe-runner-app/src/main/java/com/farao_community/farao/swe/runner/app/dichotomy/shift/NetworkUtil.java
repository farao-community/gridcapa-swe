/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy.shift;

import com.farao_community.farao.swe.runner.app.dichotomy.DichotomyDirection;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Network;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public final class NetworkUtil {
    private static final double MINIMAL_ABS_POWER_VALUE = 1e-5;

    private NetworkUtil() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    static double pseudoTargetP(Generator generator) {
        return Math.max(MINIMAL_ABS_POWER_VALUE, Math.abs(generator.getTargetP()));
    }

    static boolean isCorrect(Injection<?> injection) {
        return injection != null &&
               injection.getTerminal().isConnected() &&
               injection.getTerminal().getBusView().getBus() != null &&
               injection.getTerminal().getBusView().getBus().isInMainSynchronousComponent();
    }

    public static Network getNetworkByDirection(SweData sweData, DichotomyDirection direction) {
        Network network = null;
        switch (direction) {
            case ES_FR:
                network = sweData.getNetworkEsFr();
                break;
            case ES_PT:
                network =  sweData.getNetworkEsPt();
                break;
            case FR_ES:
                network =  sweData.getNetworkFrEs();
                break;
            case PT_ES:
                network =  sweData.getNetworkPtEs();
                break;
        }
        return network;
    }
}

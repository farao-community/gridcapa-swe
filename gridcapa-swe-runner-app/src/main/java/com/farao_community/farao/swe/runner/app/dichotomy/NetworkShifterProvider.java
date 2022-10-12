/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy;

import com.farao_community.farao.dichotomy.api.NetworkShifter;
import com.farao_community.farao.dichotomy.shift.LinearScaler;
import com.farao_community.farao.dichotomy.shift.ShiftDispatcher;
import com.farao_community.farao.swe.runner.api.resource.ProcessType;
import com.farao_community.farao.swe.runner.api.resource.SweRequest;
import com.powsybl.iidm.network.Network;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@Service
public class NetworkShifterProvider {
    private static final double SHIFT_TOLERANCE = 1;

    private final ZonalScalableProvider zonalScalableProvider;

    public NetworkShifterProvider(ZonalScalableProvider zonalScalableProvider) {
        this.zonalScalableProvider = zonalScalableProvider;
    }

    public NetworkShifter get(SweRequest request, Network network) throws IOException {
        return new LinearScaler(
                zonalScalableProvider.get(request.getGlsk().getUrl(), network, request.getProcessType(), request.getTargetProcessDateTime()),
                getShiftDispatcher(request.getProcessType(), network),
                SHIFT_TOLERANCE);
    }

    private ShiftDispatcher getShiftDispatcher(ProcessType processType, Network network) {
        //todo complete
        return null;
    }
}

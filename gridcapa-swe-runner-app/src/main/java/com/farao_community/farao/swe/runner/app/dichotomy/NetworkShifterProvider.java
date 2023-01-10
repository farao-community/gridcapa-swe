/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy;

import com.farao_community.farao.dichotomy.api.NetworkShifter;
import com.farao_community.farao.dichotomy.shift.ShiftDispatcher;
import com.farao_community.farao.swe.runner.api.exception.SweInvalidDataException;
import com.farao_community.farao.swe.runner.api.resource.ProcessType;
import com.farao_community.farao.swe.runner.app.configurations.DichotomyConfiguration;
import com.farao_community.farao.swe.runner.app.configurations.ProcessConfiguration;
import com.farao_community.farao.swe.runner.app.dichotomy.shift.*;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@Service
public class NetworkShifterProvider {

    private final DichotomyConfiguration dichotomyConfiguration;
    private final ZonalScalableProvider zonalScalableProvider;
    private final Logger businessLogger;
    private final ProcessConfiguration processConfiguration;

    public NetworkShifterProvider(DichotomyConfiguration dichotomyConfiguration, ZonalScalableProvider zonalScalableProvider, Logger businessLogger, ProcessConfiguration processConfiguration) {
        this.dichotomyConfiguration = dichotomyConfiguration;
        this.zonalScalableProvider = zonalScalableProvider;
        this.businessLogger = businessLogger;
        this.processConfiguration = processConfiguration;
    }

    public NetworkShifter get(SweData sweData, DichotomyDirection direction) {
        Network network = NetworkUtil.getNetworkByDirection(sweData, direction);
        Map<String, Double> initialNetPositions = CountryBalanceComputation.computeSweCountriesBalances(network);
        return new SweNetworkShifter(businessLogger, sweData.getProcessType(), direction,
                zonalScalableProvider.get(sweData.getGlskUrl(), network, sweData.getTimestamp()),
                getShiftDispatcher(sweData.getProcessType(), direction, initialNetPositions),
                new SweNetworkShifter.Tolerances(dichotomyConfiguration.getParameters().get(direction).getToleranceEsPt(), dichotomyConfiguration.getParameters().get(direction).getToleranceEsFr()),
                initialNetPositions,
                processConfiguration);
    }

    ShiftDispatcher getShiftDispatcher(ProcessType processType, DichotomyDirection direction, Map<String, Double> initialNetPositions) {
        switch (processType) {
            case D2CC:
                return new SweD2ccShiftDispatcher(direction, initialNetPositions);
            case IDCC:
                return new SweIdccShiftDispatcher(direction, initialNetPositions);
            default:
                throw new SweInvalidDataException(String.format("Unknown target process for SWE: %s", processType));
        }
    }

}


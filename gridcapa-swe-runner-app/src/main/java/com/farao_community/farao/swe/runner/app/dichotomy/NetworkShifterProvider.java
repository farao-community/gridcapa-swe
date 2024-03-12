/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy;

import com.farao_community.farao.dichotomy.api.NetworkShifter;
import com.farao_community.farao.dichotomy.shift.ShiftDispatcher;
import com.farao_community.farao.gridcapa_swe_commons.configuration.ProcessConfiguration;
import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import com.farao_community.farao.gridcapa_swe_commons.resource.ProcessType;
import com.farao_community.farao.gridcapa_swe_commons.shift.CountryBalanceComputation;
import com.farao_community.farao.gridcapa_swe_commons.shift.SweD2ccShiftDispatcher;
import com.farao_community.farao.gridcapa_swe_commons.shift.SweIdccShiftDispatcher;
import com.farao_community.farao.gridcapa_swe_commons.shift.SweNetworkShifter;
import com.farao_community.farao.gridcapa_swe_commons.shift.ZonalScalableProvider;
import com.farao_community.farao.swe.runner.app.configurations.DichotomyConfiguration;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.services.NetworkService;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@Service
public class NetworkShifterProvider {

    private final DichotomyConfiguration dichotomyConfiguration;
    private final Logger businessLogger;
    private final ProcessConfiguration processConfiguration;

    public NetworkShifterProvider(DichotomyConfiguration dichotomyConfiguration, Logger businessLogger, ProcessConfiguration processConfiguration) {
        this.dichotomyConfiguration = dichotomyConfiguration;
        this.businessLogger = businessLogger;
        this.processConfiguration = processConfiguration;
    }

    public NetworkShifter get(SweData sweData, DichotomyDirection direction, LoadFlowParameters loadFlowParameters) {
        ZonalScalableProvider zonalScalableProvider = new ZonalScalableProvider();
        Network network = NetworkService.getNetworkByDirection(sweData, direction);
        Map<String, Double> initialNetPositions = CountryBalanceComputation.computeSweCountriesBalances(network, loadFlowParameters);

        return new SweNetworkShifter(businessLogger, sweData.getProcessType(), direction,
                zonalScalableProvider.get(sweData.getGlskUrl(), network, sweData.getTimestamp()),
                getShiftDispatcher(sweData.getProcessType(), direction, initialNetPositions),
                dichotomyConfiguration.getParameters().get(direction).getToleranceEsPt(),
                dichotomyConfiguration.getParameters().get(direction).getToleranceEsFr(),
                initialNetPositions,
                processConfiguration,
                loadFlowParameters);
    }

    ShiftDispatcher getShiftDispatcher(ProcessType processType, DichotomyDirection direction, Map<String, Double> initialNetPositions) {
        return switch (processType) {
            case D2CC -> new SweD2ccShiftDispatcher(direction, initialNetPositions);
            case IDCC, IDCC_IDCF -> new SweIdccShiftDispatcher(direction, initialNetPositions);
        };
    }
}


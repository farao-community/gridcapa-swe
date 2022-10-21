/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy;

import com.farao_community.farao.dichotomy.api.NetworkShifter;
import com.farao_community.farao.dichotomy.shift.ShiftDispatcher;
import com.farao_community.farao.dichotomy.shift.SplittingFactors;
import com.farao_community.farao.swe.runner.api.exception.SweInvalidDataException;
import com.farao_community.farao.swe.runner.api.resource.ProcessType;
import com.farao_community.farao.swe.runner.app.configurations.DichotomyConfiguration;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import org.springframework.stereotype.Service;
import com.farao_community.farao.swe.runner.app.dichotomy.shift.ZonalScalableProvider;
import com.farao_community.farao.swe.runner.app.dichotomy.shift.CountryBalanceComputation;
import com.farao_community.farao.swe.runner.app.dichotomy.shift.SweNetworkShifter;
import com.farao_community.farao.swe.runner.app.dichotomy.shift.SweD2ccShiftDispatcher;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@Service
public class NetworkShifterProvider {

    private final DichotomyConfiguration dichotomyConfiguration;
    private final ZonalScalableProvider zonalScalableProvider;
    private static final double DEFAULT_TOLERANCE_ES_PT = 50;
    private static final double DEFAULT_TOLERANCE_ES_FR = 10;

    public NetworkShifterProvider(DichotomyConfiguration dichotomyConfiguration, ZonalScalableProvider zonalScalableProvider) {
        this.dichotomyConfiguration = dichotomyConfiguration;
        this.zonalScalableProvider = zonalScalableProvider;
    }

    public NetworkShifter get(SweData sweData, DichotomyDirection direction) throws IOException {
        Map<String, Double> initialNetPositions = CountryBalanceComputation.computeSweCountriesBalances(sweData.getNetwork());
        return new SweNetworkShifter(sweData.getProcessType(), direction,
                zonalScalableProvider.get(sweData.getGlskUrl(), sweData.getNetwork(), sweData.getTimestamp()),
                getShiftDispatcher(sweData.getProcessType(), direction, initialNetPositions),
                DEFAULT_TOLERANCE_ES_PT,
                dichotomyConfiguration.getParameters().get(direction).getTolerance()); //todo modify configuration with two values
    }

    private ShiftDispatcher getShiftDispatcher(ProcessType processType, DichotomyDirection direction, Map<String, Double> initialNetPositions) {
        switch (processType) {
            case D2CC:
                return new SweD2ccShiftDispatcher(direction, initialNetPositions);
            case IDCC:
                return getIdccSplittingFactors(direction);
            default:
                throw new SweInvalidDataException(String.format("Unknown target process for SWE: %s", processType));
        }
    }

    private SplittingFactors getIdccSplittingFactors(DichotomyDirection direction) {
        Map<String, Double> factors = new TreeMap<>();
        // todo
        return new SplittingFactors(factors);
    }

}


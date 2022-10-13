/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy;

import com.farao_community.farao.commons.EICode;
import com.farao_community.farao.dichotomy.api.NetworkShifter;
import com.farao_community.farao.dichotomy.shift.LinearScaler;
import com.farao_community.farao.dichotomy.shift.ShiftDispatcher;
import com.farao_community.farao.dichotomy.shift.SplittingFactors;
import com.farao_community.farao.swe.runner.api.exception.SweInvalidDataException;
import com.farao_community.farao.swe.runner.api.resource.ProcessType;
import com.farao_community.farao.swe.runner.app.configurations.DichotomyConfiguration;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.powsybl.iidm.network.Country;
import org.springframework.stereotype.Service;

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

    public NetworkShifterProvider(DichotomyConfiguration dichotomyConfiguration, ZonalScalableProvider zonalScalableProvider) {
        this.dichotomyConfiguration = dichotomyConfiguration;
        this.zonalScalableProvider = zonalScalableProvider;
    }

    public NetworkShifter get(SweData sweData, DichotomyDirection direction) throws IOException {
        return new LinearScaler(
                zonalScalableProvider.get(sweData.getGlskUrl(), sweData.getNetwork(), sweData.getTimestamp()),
                getShiftDispatcher(sweData.getProcessType(), direction),
                dichotomyConfiguration.getParameters().get(direction).getTolerance());
    }

    private ShiftDispatcher getShiftDispatcher(ProcessType processType, DichotomyDirection direction) {
        switch (processType) {
            case D2CC:
                return getD2ccSplittingFactors(direction);
            case IDCC:
                return getIdccSplittingFactors(direction);
            default:
                throw new SweInvalidDataException(String.format("Unknown target process for SWE: %s", processType));
        }
    }

    private SplittingFactors getD2ccSplittingFactors(DichotomyDirection direction) {
        // For D2CC the initial net position is zero for each country todo check with real test
        Map<String, Double> factors = new TreeMap<>();
        factors.put(toEic("PT"), 0.);
        factors.put(toEic("ES"), DichotomyDirection.ES_FR.equals(direction) ? 1. : -1.);
        factors.put(toEic("FR"), DichotomyDirection.ES_FR.equals(direction) ? -1. : 1.);
        return new SplittingFactors(factors);
    }

    private SplittingFactors getIdccSplittingFactors(DichotomyDirection direction) {
        Map<String, Double> factors = new TreeMap<>();
        factors.put(toEic("PT"), 0.); // NPPT= NPPTinitial
        //factors.put(toEic("ES"), DichotomyDirection.ES_FR.equals(direction) ? 1. : -1.); // NPES= -NPPTinitial - exchangeValuetodo complete later complicated
        //factors.put(toEic("FR"), DichotomyDirection.ES_FR.equals(direction) ? -1. : 1.);
        return new SplittingFactors(factors);
    }

    private String toEic(String country) {
        return new EICode(Country.valueOf(country)).getAreaCode();
    }
}


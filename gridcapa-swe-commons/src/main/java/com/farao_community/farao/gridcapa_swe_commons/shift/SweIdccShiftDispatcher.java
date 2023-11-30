/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_swe_commons.shift;

import com.farao_community.farao.commons.EICode;
import com.farao_community.farao.dichotomy.shift.ShiftDispatcher;
import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import com.farao_community.farao.gridcapa_swe_commons.exception.SweInvalidDataException;
import com.powsybl.iidm.network.Country;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class SweIdccShiftDispatcher implements ShiftDispatcher {
    private final Map<String, Double> initialNetPositions;
    private final DichotomyDirection direction;

    public SweIdccShiftDispatcher(DichotomyDirection direction, Map<String, Double> initialNetPositions) {
        this.direction = direction;
        this.initialNetPositions = initialNetPositions;
    }

    @Override
    public Map<String, Double> dispatch(double stepExchangeValue) {
        Map<String, Double> factors = new TreeMap<>();
        switch (direction) {
            case ES_FR:
                factors.put(toEic("PT"), 0.);
                factors.put(toEic("ES"), -initialNetPositions.get(toEic("PT")) - initialNetPositions.get(toEic("ES")) + stepExchangeValue);
                factors.put(toEic("FR"), -stepExchangeValue - initialNetPositions.get(toEic("FR")));
                break;
            case FR_ES:
                factors.put(toEic("PT"), 0.);
                factors.put(toEic("ES"), -initialNetPositions.get(toEic("PT")) - initialNetPositions.get(toEic("ES")) - stepExchangeValue);
                factors.put(toEic("FR"), stepExchangeValue - initialNetPositions.get(toEic("FR")));
                break;
            case ES_PT:
                factors.put(toEic("PT"), -stepExchangeValue - initialNetPositions.get(toEic("PT")));
                factors.put(toEic("ES"), initialNetPositions.get(toEic("PT")) + stepExchangeValue);
                factors.put(toEic("FR"), -initialNetPositions.get(toEic("PT")) - initialNetPositions.get(toEic("ES")) - initialNetPositions.get(toEic("FR")));
                break;
            case PT_ES:
                factors.put(toEic("PT"), stepExchangeValue - initialNetPositions.get(toEic("PT")));
                factors.put(toEic("ES"), initialNetPositions.get(toEic("PT")) - stepExchangeValue);
                factors.put(toEic("FR"), -initialNetPositions.get(toEic("PT")) - initialNetPositions.get(toEic("ES")) - initialNetPositions.get(toEic("FR")));
                break;
            default:
                throw new SweInvalidDataException(String.format("Unknown dichotomy direction for SWE: %s", direction));
        }
        return factors;
    }

    private String toEic(String country) {
        return new EICode(Country.valueOf(country)).getAreaCode();
    }
}

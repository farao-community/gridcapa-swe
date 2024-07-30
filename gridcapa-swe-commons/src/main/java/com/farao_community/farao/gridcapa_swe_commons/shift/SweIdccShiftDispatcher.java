/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_swe_commons.shift;

import com.farao_community.farao.dichotomy.shift.ShiftDispatcher;
import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import com.farao_community.farao.gridcapa_swe_commons.exception.SweInvalidDataException;
import com.farao_community.farao.gridcapa_swe_commons.resource.SweEICode;

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
                factors.put(SweEICode.PT_EIC, 0.);
                factors.put(SweEICode.ES_EIC, -initialNetPositions.get(SweEICode.PT_EIC) - initialNetPositions.get(SweEICode.ES_EIC) + stepExchangeValue);
                factors.put(SweEICode.FR_EIC, -stepExchangeValue - initialNetPositions.get(SweEICode.FR_EIC));
                break;
            case FR_ES:
                factors.put(SweEICode.PT_EIC, 0.);
                factors.put(SweEICode.ES_EIC, -initialNetPositions.get(SweEICode.PT_EIC) - initialNetPositions.get(SweEICode.ES_EIC) - stepExchangeValue);
                factors.put(SweEICode.FR_EIC, stepExchangeValue - initialNetPositions.get(SweEICode.FR_EIC));
                break;
            case ES_PT:
                factors.put(SweEICode.PT_EIC, -stepExchangeValue - initialNetPositions.get(SweEICode.PT_EIC));
                factors.put(SweEICode.ES_EIC, initialNetPositions.get(SweEICode.PT_EIC) + stepExchangeValue);
                factors.put(SweEICode.FR_EIC, -initialNetPositions.get(SweEICode.PT_EIC) - initialNetPositions.get(SweEICode.ES_EIC) - initialNetPositions.get(SweEICode.FR_EIC));
                break;
            case PT_ES:
                factors.put(SweEICode.PT_EIC, stepExchangeValue - initialNetPositions.get(SweEICode.PT_EIC));
                factors.put(SweEICode.ES_EIC, initialNetPositions.get(SweEICode.PT_EIC) - stepExchangeValue);
                factors.put(SweEICode.FR_EIC, -initialNetPositions.get(SweEICode.PT_EIC) - initialNetPositions.get(SweEICode.ES_EIC) - initialNetPositions.get(SweEICode.FR_EIC));
                break;
            default:
                throw new SweInvalidDataException(String.format("Unknown dichotomy direction for SWE: %s", direction));
        }
        return factors;
    }
}

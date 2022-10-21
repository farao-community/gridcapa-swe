/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy.shift;

import com.farao_community.farao.commons.EICode;
import com.farao_community.farao.dichotomy.shift.ShiftDispatcher;
import com.farao_community.farao.swe.runner.app.dichotomy.DichotomyDirection;
import com.powsybl.iidm.network.Country;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public class SweD2ccShiftDispatcher implements ShiftDispatcher {
    private final Map<String, Double> initialNetPositions;
    private final DichotomyDirection direction;

    public SweD2ccShiftDispatcher(DichotomyDirection direction, Map<String, Double> initialNetPositions) {
        this.direction = direction;
        this.initialNetPositions = initialNetPositions;
    }

    @Override
    public Map<String, Double> dispatch(double value) {
        Map<String, Double> factors = new TreeMap<>();
        factors.put(toEic("PT"), 0.);
        factors.put(toEic("ES"), DichotomyDirection.ES_FR.equals(direction) ? 1. : -1.);
        factors.put(toEic("FR"), DichotomyDirection.ES_FR.equals(direction) ? -1. : 1.);
        return factors.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue() * value - initialNetPositions.get(e.getKey())));
    }

    private String toEic(String country) {
        return new EICode(Country.valueOf(country)).getAreaCode();
    }
}

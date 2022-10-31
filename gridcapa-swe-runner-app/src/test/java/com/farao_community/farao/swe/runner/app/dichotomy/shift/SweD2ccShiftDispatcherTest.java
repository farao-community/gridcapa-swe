/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy.shift;

import com.farao_community.farao.swe.runner.app.dichotomy.DichotomyDirection;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
class SweD2ccShiftDispatcherTest {

    @Test
    void dispatch() {
        Map<String, Double> intialNetPositions = Map.of("10YES-REE------0", 50., "10YFR-RTE------C", 100., "10YPT-REN------W", 150.);
        SweD2ccShiftDispatcher sweD2ccShiftDispatcher = new SweD2ccShiftDispatcher(DichotomyDirection.ES_FR, intialNetPositions);
        Map<String, Double> shifts = sweD2ccShiftDispatcher.dispatch(1000);
        assertEquals(950., shifts.get("10YES-REE------0"), 0.001);
        assertEquals(-150., shifts.get("10YPT-REN------W"), 0.001);
        assertEquals(-1100., shifts.get("10YFR-RTE------C"), 0.001);
    }
}

/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_swe_commons.shift;

import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import com.farao_community.farao.gridcapa_swe_commons.resource.SweEICode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
class SweIdccShiftDispatcherTest {

    private final Map<String, Double> intialNetPositions = Map.of(SweEICode.ES_EIC, 50., SweEICode.FR_EIC, 100., SweEICode.PT_EIC, 150.);

    @Test
    void dispatchEsFrTest() {
        SweIdccShiftDispatcher sweIdccShiftDispatcher = new SweIdccShiftDispatcher(DichotomyDirection.ES_FR, intialNetPositions);
        Map<String, Double> shifts = sweIdccShiftDispatcher.dispatch(1000);
        assertEquals(0, shifts.get(SweEICode.PT_EIC), 0.001);
        assertEquals(-150 - 50 + 1000, shifts.get(SweEICode.ES_EIC), 0.001);
        assertEquals(-1100., shifts.get(SweEICode.FR_EIC), 0.001);
    }

    @Test
    void dispatchFrEsTest() {
        Map<String, Double> customIntialNetPositions = Map.of(SweEICode.ES_EIC, 50., SweEICode.FR_EIC, 100., SweEICode.PT_EIC, 150.);
        SweIdccShiftDispatcher sweIdccShiftDispatcher = new SweIdccShiftDispatcher(DichotomyDirection.FR_ES, customIntialNetPositions);
        Map<String, Double> shifts = sweIdccShiftDispatcher.dispatch(1000);
        assertEquals(0, shifts.get(SweEICode.PT_EIC), 0.001);
        assertEquals(-150 - 50 - 1000, shifts.get(SweEICode.ES_EIC), 0.001);
        assertEquals(900., shifts.get(SweEICode.FR_EIC), 0.001);
    }

    @Test
    void dispatchEsPtTest() {
        SweIdccShiftDispatcher sweIdccShiftDispatcher = new SweIdccShiftDispatcher(DichotomyDirection.ES_PT, intialNetPositions);
        Map<String, Double> shifts = sweIdccShiftDispatcher.dispatch(1000);
        assertEquals(-1000. - 150, shifts.get(SweEICode.PT_EIC), 0.001);
        assertEquals(150 + 1000, shifts.get(SweEICode.ES_EIC), 0.001);
        assertEquals(-300., shifts.get(SweEICode.FR_EIC), 0.001);
    }

    @Test
    void dispatchPtEsTest() {
        SweIdccShiftDispatcher sweIdccShiftDispatcher = new SweIdccShiftDispatcher(DichotomyDirection.PT_ES, intialNetPositions);
        Map<String, Double> shifts = sweIdccShiftDispatcher.dispatch(1000);
        assertEquals(1000 - 150, shifts.get(SweEICode.PT_EIC), 0.001);
        assertEquals(150 - 1000, shifts.get(SweEICode.ES_EIC), 0.001);
        assertEquals(-300., shifts.get(SweEICode.FR_EIC), 0.001);
    }
}

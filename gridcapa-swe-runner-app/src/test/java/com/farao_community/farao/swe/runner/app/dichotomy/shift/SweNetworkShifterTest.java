/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy.shift;

import com.farao_community.farao.swe.runner.api.resource.ProcessType;
import com.farao_community.farao.swe.runner.app.dichotomy.DichotomyDirection;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
class SweNetworkShifterTest {

    @Test
    void checkD2ccEsFrTargetExchangesCalculatedCorrectly() {
        Map<String, Double> intialNetPositions = Map.of("10YES-REE------0", 50., "10YFR-RTE------C", 100., "10YPT-REN------W", 150.);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(null, ProcessType.D2CC,
            DichotomyDirection.ES_FR, null, null, 0., 0., intialNetPositions);
        Map<String, Double> shifts = sweNetworkShifter.getTargetExchanges(1000);
        assertEquals(0., shifts.get("ES_PT"), 0.001);
        assertEquals(1000., shifts.get("ES_FR"), 0.001);
    }

    @Test
    void checkD2ccFrEsTargetExchangesCalculatedCorrectly() {
        Map<String, Double> intialNetPositions = Map.of("10YES-REE------0", 50., "10YFR-RTE------C", 100., "10YPT-REN------W", 150.);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(null, ProcessType.D2CC,
            DichotomyDirection.FR_ES, null, null, 0., 0., intialNetPositions);
        Map<String, Double> shifts = sweNetworkShifter.getTargetExchanges(1000);
        assertEquals(0., shifts.get("ES_PT"), 0.001);
        assertEquals(-1000., shifts.get("ES_FR"), 0.001);
    }

    @Test
    void checkD2ccPtEsTargetExchangesCalculatedCorrectly() {
        Map<String, Double> intialNetPositions = Map.of("10YES-REE------0", 50., "10YFR-RTE------C", 100., "10YPT-REN------W", 150.);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(null, ProcessType.D2CC,
            DichotomyDirection.PT_ES, null, null, 0., 0., intialNetPositions);
        Map<String, Double> shifts = sweNetworkShifter.getTargetExchanges(1000);
        assertEquals(-1000., shifts.get("ES_PT"), 0.001);
        assertEquals(0., shifts.get("ES_FR"), 0.001);
    }

    @Test
    void checkD2ccEsPtTargetExchangesCalculatedCorrectly() {
        Map<String, Double> intialNetPositions = Map.of("10YES-REE------0", 50., "10YFR-RTE------C", 100., "10YPT-REN------W", 150.);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(null, ProcessType.D2CC,
            DichotomyDirection.PT_ES, null, null, 0., 0., intialNetPositions);
        Map<String, Double> shifts = sweNetworkShifter.getTargetExchanges(1000);
        assertEquals(-1000., shifts.get("ES_PT"), 0.001);
        assertEquals(0., shifts.get("ES_FR"), 0.001);
    }

    @Test
    void checkIdccEsFrTargetExchangesCalculatedCorrectly() {
        Map<String, Double> intialNetPositions = Map.of("10YES-REE------0", 50., "10YFR-RTE------C", 100., "10YPT-REN------W", 150.);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(null, ProcessType.IDCC,
            DichotomyDirection.ES_FR, null, null, 0., 0., intialNetPositions);
        Map<String, Double> shifts = sweNetworkShifter.getTargetExchanges(1000);
        assertEquals(-150., shifts.get("ES_PT"), 0.001);
        assertEquals(1000., shifts.get("ES_FR"), 0.001);
    }

    @Test
    void checkIdccFrEsTargetExchangesCalculatedCorrectly() {
        Map<String, Double> intialNetPositions = Map.of("10YES-REE------0", 50., "10YFR-RTE------C", 100., "10YPT-REN------W", 150.);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(null, ProcessType.IDCC,
            DichotomyDirection.FR_ES, null, null, 0., 0., intialNetPositions);
        Map<String, Double> shifts = sweNetworkShifter.getTargetExchanges(1000);
        assertEquals(-150., shifts.get("ES_PT"), 0.001);
        assertEquals(-1000., shifts.get("ES_FR"), 0.001);
    }

    @Test
    void checkIdccEsPtTargetExchangesCalculatedCorrectly() {
        Map<String, Double> intialNetPositions = Map.of("10YES-REE------0", 50., "10YFR-RTE------C", 100., "10YPT-REN------W", 150.);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(null, ProcessType.IDCC,
            DichotomyDirection.ES_PT, null, null, 0., 0., intialNetPositions);
        Map<String, Double> shifts = sweNetworkShifter.getTargetExchanges(1000);
        assertEquals(1000., shifts.get("ES_PT"), 0.001);
        assertEquals(200, shifts.get("ES_FR"), 0.001);
    }

    @Test
    void checkIdccPtEsTargetExchangesCalculatedCorrectly() {
        Map<String, Double> intialNetPositions = Map.of("10YES-REE------0", 50., "10YFR-RTE------C", 100., "10YPT-REN------W", 150.);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(null, ProcessType.IDCC,
            DichotomyDirection.PT_ES, null, null, 0., 0., intialNetPositions);
        Map<String, Double> shifts = sweNetworkShifter.getTargetExchanges(1000);
        assertEquals(-1000., shifts.get("ES_PT"), 0.001);
        assertEquals(200., shifts.get("ES_FR"), 0.001);
    }
}

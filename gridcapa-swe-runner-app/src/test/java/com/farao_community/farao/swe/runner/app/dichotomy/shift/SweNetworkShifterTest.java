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
@SpringBootTest
class SweNetworkShifterTest {

    @MockBean
    private ProcessConfiguration processConfiguration;

    @MockBean
    private Logger businessLogger;
    private static ZonalDataImpl<Scalable> zonalScalable;
    private final String networkFileName = "/shift/TestCase_with_swe_countries.xiidm";

    @BeforeAll
    static void setup() {
        Scalable scalableFR = Scalable.onGenerator("FFR1AA11_generator");
        Scalable scalableES = Scalable.onGenerator("EES1AA11_generator");
        Scalable scalablePT = Scalable.onGenerator("PPT1AA11_generator");

        Map<String, Scalable> mapScalable = new HashMap<>();
        mapScalable.put("10YFR-RTE------C", scalableFR);
        mapScalable.put("10YES-REE------0", scalableES);
        mapScalable.put("10YPT-REN------W", scalablePT);
        zonalScalable = new ZonalDataImpl<>(mapScalable);
    }

    @Test
    void checkD2ccEsFrTargetExchangesCalculatedCorrectly() {
        Map<String, Double> intialNetPositions = Map.of("10YES-REE------0", 50., "10YFR-RTE------C", 100., "10YPT-REN------W", 150.);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(null, ProcessType.D2CC,
            DichotomyDirection.ES_FR, null, null, new SweNetworkShifter.Tolerances(0., 0.), intialNetPositions, null);
        Map<String, Double> shifts = sweNetworkShifter.getTargetExchanges(1000);
        assertEquals(0., shifts.get("ES_PT"), 0.001);
        assertEquals(1000., shifts.get("ES_FR"), 0.001);
    }

    @Test
    void checkD2ccFrEsTargetExchangesCalculatedCorrectly() {
        Map<String, Double> intialNetPositions = Map.of("10YES-REE------0", 50., "10YFR-RTE------C", 100., "10YPT-REN------W", 150.);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(null, ProcessType.D2CC,
            DichotomyDirection.FR_ES, null, null, new SweNetworkShifter.Tolerances(0., 0.), intialNetPositions, null);
        Map<String, Double> shifts = sweNetworkShifter.getTargetExchanges(1000);
        assertEquals(0., shifts.get("ES_PT"), 0.001);
        assertEquals(-1000., shifts.get("ES_FR"), 0.001);
    }

    @Test
    void checkD2ccPtEsTargetExchangesCalculatedCorrectly() {
        Map<String, Double> intialNetPositions = Map.of("10YES-REE------0", 50., "10YFR-RTE------C", 100., "10YPT-REN------W", 150.);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(null, ProcessType.D2CC,
            DichotomyDirection.PT_ES, null, null, new SweNetworkShifter.Tolerances(0., 0.), intialNetPositions, null);
        Map<String, Double> shifts = sweNetworkShifter.getTargetExchanges(1000);
        assertEquals(-1000., shifts.get("ES_PT"), 0.001);
        assertEquals(0., shifts.get("ES_FR"), 0.001);
    }

    @Test
    void checkD2ccEsPtTargetExchangesCalculatedCorrectly() {
        Map<String, Double> intialNetPositions = Map.of("10YES-REE------0", 50., "10YFR-RTE------C", 100., "10YPT-REN------W", 150.);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(null, ProcessType.D2CC,
            DichotomyDirection.PT_ES, null, null, new SweNetworkShifter.Tolerances(0., 0.), intialNetPositions, null);
        Map<String, Double> shifts = sweNetworkShifter.getTargetExchanges(1000);
        assertEquals(-1000., shifts.get("ES_PT"), 0.001);
        assertEquals(0., shifts.get("ES_FR"), 0.001);
    }

    @Test
    void checkIdccEsFrTargetExchangesCalculatedCorrectly() {
        Map<String, Double> intialNetPositions = Map.of("10YES-REE------0", 50., "10YFR-RTE------C", 100., "10YPT-REN------W", 150.);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(null, ProcessType.IDCC,
            DichotomyDirection.ES_FR, null, null, new SweNetworkShifter.Tolerances(0., 0.), intialNetPositions, null);
        Map<String, Double> shifts = sweNetworkShifter.getTargetExchanges(1000);
        assertEquals(-150., shifts.get("ES_PT"), 0.001);
        assertEquals(1000., shifts.get("ES_FR"), 0.001);
    }

    @Test
    void checkIdccFrEsTargetExchangesCalculatedCorrectly() {
        Map<String, Double> intialNetPositions = Map.of("10YES-REE------0", 50., "10YFR-RTE------C", 100., "10YPT-REN------W", 150.);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(null, ProcessType.IDCC,
            DichotomyDirection.FR_ES, null, null, new SweNetworkShifter.Tolerances(0., 0.), intialNetPositions, null);
        Map<String, Double> shifts = sweNetworkShifter.getTargetExchanges(1000);
        assertEquals(-150., shifts.get("ES_PT"), 0.001);
        assertEquals(-1000., shifts.get("ES_FR"), 0.001);
    }

    @Test
    void checkIdccEsPtTargetExchangesCalculatedCorrectly() {
        Map<String, Double> intialNetPositions = Map.of("10YES-REE------0", 50., "10YFR-RTE------C", 100., "10YPT-REN------W", 150.);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(null, ProcessType.IDCC,
            DichotomyDirection.ES_PT, null, null, new SweNetworkShifter.Tolerances(0., 0.), intialNetPositions, null);
        Map<String, Double> shifts = sweNetworkShifter.getTargetExchanges(1000);
        assertEquals(1000., shifts.get("ES_PT"), 0.001);
        assertEquals(200, shifts.get("ES_FR"), 0.001);
    }

    @Test
    void checkIdccPtEsTargetExchangesCalculatedCorrectly() {
        Map<String, Double> intialNetPositions = Map.of("10YES-REE------0", 50., "10YFR-RTE------C", 100., "10YPT-REN------W", 150.);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(null, ProcessType.IDCC,
            DichotomyDirection.PT_ES, null, null, new SweNetworkShifter.Tolerances(0., 0.), intialNetPositions, null);
        Map<String, Double> shifts = sweNetworkShifter.getTargetExchanges(1000);
        assertEquals(-1000., shifts.get("ES_PT"), 0.001);
        assertEquals(200., shifts.get("ES_FR"), 0.001);
    }

    @Test
    void shiftNetworkSuccessTest() throws GlskLimitationException, ShiftingException {
        Network network = Network.read(networkFileName, getClass().getResourceAsStream(networkFileName));
        Map<String, Double> intialNetPositions = Map.of("10YES-REE------0", 2317., "10YFR-RTE------C", -2317., "10YPT-REN------W", 0.);
        ShiftDispatcher shiftDispatcher = new SweD2ccShiftDispatcher(DichotomyDirection.ES_FR, intialNetPositions);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(businessLogger, ProcessType.D2CC,
                DichotomyDirection.ES_FR, zonalScalable, shiftDispatcher, 1., 1., intialNetPositions, processConfiguration);

        Mockito.when(processConfiguration.getShiftMaxIterationNumber()).thenReturn(5);
        sweNetworkShifter.shiftNetwork(1000., network);

        Map<String, Double> shiftedExchanges = CountryBalanceComputation.computeSweBordersExchanges(network);
        assertEquals(1000, shiftedExchanges.get("ES_FR"), 1.);
        assertEquals(0., shiftedExchanges.get("ES_PT"), 1.);

    }

    @Test
    void shiftNetworWithGlskLimitation() {
        Network network = Network.read(networkFileName, getClass().getResourceAsStream(networkFileName));
        Map<String, Double> intialNetPositions = Map.of("10YES-REE------0", 2317., "10YFR-RTE------C", -2317., "10YPT-REN------W", 0.);
        ShiftDispatcher shiftDispatcher = new SweD2ccShiftDispatcher(DichotomyDirection.ES_FR, intialNetPositions);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(businessLogger, ProcessType.D2CC,
                DichotomyDirection.ES_FR, zonalScalable, shiftDispatcher, 1., 1., intialNetPositions, processConfiguration);
        Mockito.when(processConfiguration.getShiftMaxIterationNumber()).thenReturn(5);
        assertThrows(GlskLimitationException.class, () -> sweNetworkShifter.shiftNetwork(10000., network));
    }

    @Test
    void shiftNetworWithShiftingException() {
        Network network = Network.read(networkFileName, getClass().getResourceAsStream(networkFileName));
        Map<String, Double> intialNetPositions = Map.of("10YES-REE------0", 2317., "10YFR-RTE------C", -2317., "10YPT-REN------W", 0.);
        ShiftDispatcher shiftDispatcher = new SweD2ccShiftDispatcher(DichotomyDirection.ES_FR, intialNetPositions);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(businessLogger, ProcessType.D2CC,
                DichotomyDirection.ES_FR, zonalScalable, shiftDispatcher, 1., 1., intialNetPositions, processConfiguration);
        Mockito.when(processConfiguration.getShiftMaxIterationNumber()).thenReturn(1);
        assertThrows(ShiftingException.class, () -> sweNetworkShifter.shiftNetwork(1000., network));
    }
}

/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy.shift;

import com.farao_community.farao.dichotomy.api.exceptions.GlskLimitationException;
import com.farao_community.farao.dichotomy.api.exceptions.ShiftingException;
import com.farao_community.farao.dichotomy.shift.ShiftDispatcher;
import com.farao_community.farao.swe.runner.api.resource.ProcessType;
import com.farao_community.farao.swe.runner.app.configurations.ProcessConfiguration;
import com.farao_community.farao.swe.runner.app.dichotomy.DichotomyDirection;
import com.powsybl.glsk.commons.ZonalDataImpl;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Network;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@SpringBootTest
class SweNetworkShifterTest {

    private static final String EIC_FR = "10YFR-RTE------C";
    private static final String EIC_ES = "10YES-REE------0";
    private static final String EIC_PT = "10YPT-REN------W";

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
                DichotomyDirection.ES_FR, null, null, 0., 0., intialNetPositions, null);
        Map<String, Double> shifts = sweNetworkShifter.getTargetExchanges(1000);
        assertEquals(0., shifts.get("ES_PT"), 0.001);
        assertEquals(1000., shifts.get("ES_FR"), 0.001);
    }

    @Test
    void checkD2ccFrEsTargetExchangesCalculatedCorrectly() {
        Map<String, Double> intialNetPositions = Map.of("10YES-REE------0", 50., "10YFR-RTE------C", 100., "10YPT-REN------W", 150.);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(null, ProcessType.D2CC,
                DichotomyDirection.FR_ES, null, null, 0., 0., intialNetPositions, null);
        Map<String, Double> shifts = sweNetworkShifter.getTargetExchanges(1000);
        assertEquals(0., shifts.get("ES_PT"), 0.001);
        assertEquals(-1000., shifts.get("ES_FR"), 0.001);
    }

    @Test
    void checkD2ccPtEsTargetExchangesCalculatedCorrectly() {
        Map<String, Double> intialNetPositions = Map.of("10YES-REE------0", 50., "10YFR-RTE------C", 100., "10YPT-REN------W", 150.);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(null, ProcessType.D2CC,
                DichotomyDirection.PT_ES, null, null, 0., 0., intialNetPositions, null);
        Map<String, Double> shifts = sweNetworkShifter.getTargetExchanges(1000);
        assertEquals(-1000., shifts.get("ES_PT"), 0.001);
        assertEquals(0., shifts.get("ES_FR"), 0.001);
    }

    @Test
    void checkD2ccEsPtTargetExchangesCalculatedCorrectly() {
        Map<String, Double> intialNetPositions = Map.of("10YES-REE------0", 50., "10YFR-RTE------C", 100., "10YPT-REN------W", 150.);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(null, ProcessType.D2CC,
                DichotomyDirection.PT_ES, null, null, 0., 0., intialNetPositions, null);
        Map<String, Double> shifts = sweNetworkShifter.getTargetExchanges(1000);
        assertEquals(-1000., shifts.get("ES_PT"), 0.001);
        assertEquals(0., shifts.get("ES_FR"), 0.001);
    }

    @Test
    void checkIdccEsFrTargetExchangesCalculatedCorrectly() {
        Map<String, Double> intialNetPositions = Map.of("10YES-REE------0", 50., "10YFR-RTE------C", 100., "10YPT-REN------W", 150.);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(null, ProcessType.IDCC,
                DichotomyDirection.ES_FR, null, null, 0., 0., intialNetPositions, null);
        Map<String, Double> shifts = sweNetworkShifter.getTargetExchanges(1000);
        assertEquals(-150., shifts.get("ES_PT"), 0.001);
        assertEquals(1000., shifts.get("ES_FR"), 0.001);
    }

    @Test
    void checkIdccFrEsTargetExchangesCalculatedCorrectly() {
        Map<String, Double> intialNetPositions = Map.of("10YES-REE------0", 50., "10YFR-RTE------C", 100., "10YPT-REN------W", 150.);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(null, ProcessType.IDCC,
                DichotomyDirection.FR_ES, null, null, 0., 0., intialNetPositions, null);
        Map<String, Double> shifts = sweNetworkShifter.getTargetExchanges(1000);
        assertEquals(-150., shifts.get("ES_PT"), 0.001);
        assertEquals(-1000., shifts.get("ES_FR"), 0.001);
    }

    @Test
    void checkIdccEsPtTargetExchangesCalculatedCorrectly() {
        Map<String, Double> intialNetPositions = Map.of("10YES-REE------0", 50., "10YFR-RTE------C", 100., "10YPT-REN------W", 150.);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(null, ProcessType.IDCC,
                DichotomyDirection.ES_PT, null, null, 0., 0., intialNetPositions, null);
        Map<String, Double> shifts = sweNetworkShifter.getTargetExchanges(1000);
        assertEquals(1000., shifts.get("ES_PT"), 0.001);
        assertEquals(200, shifts.get("ES_FR"), 0.001);
    }

    @Test
    void checkIdccPtEsTargetExchangesCalculatedCorrectly() {
        Map<String, Double> intialNetPositions = Map.of("10YES-REE------0", 50., "10YFR-RTE------C", 100., "10YPT-REN------W", 150.);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(null, ProcessType.IDCC,
                DichotomyDirection.PT_ES, null, null, 0., 0., intialNetPositions, null);
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
        assertThrows(GlskLimitationException.class, () -> sweNetworkShifter.shiftNetwork(11000., network));
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

    ZonalDataImpl<Scalable> getZonalWithMinMax() {
        Scalable scalableFR = Scalable.onGenerator("FFR1AA11_generator", -9100.0, 9100.0);
        Scalable scalableES = Scalable.onGenerator("EES1AA11_generator", -9100.0, 9100.0);
        Scalable scalablePT = Scalable.onGenerator("PPT1AA11_generator", -9100.0, 9100.0);

        Map<String, Scalable> mapScalable = new HashMap<>();
        mapScalable.put("10YFR-RTE------C", scalableFR);
        mapScalable.put("10YES-REE------0", scalableES);
        mapScalable.put("10YPT-REN------W", scalablePT);
        return new ZonalDataImpl<>(mapScalable);
    }

    @Test
    void shiftNetworkSuccessWithChangePminPmaxFromGlskTest() throws GlskLimitationException, ShiftingException {
        Network network = Network.read(networkFileName, getClass().getResourceAsStream(networkFileName));
        Map<String, Double> intialNetPositions = Map.of("10YES-REE------0", 2310., "10YFR-RTE------C", -2310., "10YPT-REN------W", 0.);
        ShiftDispatcher shiftDispatcher = new SweD2ccShiftDispatcher(DichotomyDirection.ES_FR, intialNetPositions);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(businessLogger, ProcessType.D2CC,
                DichotomyDirection.ES_FR, getZonalWithMinMax(), shiftDispatcher, 1., 1., intialNetPositions, processConfiguration);

        Mockito.when(processConfiguration.getShiftMaxIterationNumber()).thenReturn(5);
        sweNetworkShifter.shiftNetwork(9050., network);

        Map<String, Double> shiftedExchanges = CountryBalanceComputation.computeSweBordersExchanges(network);
        assertEquals(9050., shiftedExchanges.get("ES_FR"), 1.);
        assertEquals(0., shiftedExchanges.get("ES_PT"), 1.);
    }

    @Test
    void shiftNetworWithGlskLimitationWithChangePminPmaxFromGlskTest() {
        Network network = Network.read(networkFileName, getClass().getResourceAsStream(networkFileName));
        Map<String, Double> intialNetPositions = Map.of("10YES-REE------0", 231., "10YFR-RTE------C", -231., "10YPT-REN------W", 0.);
        ShiftDispatcher shiftDispatcher = new SweD2ccShiftDispatcher(DichotomyDirection.ES_FR, intialNetPositions);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(businessLogger, ProcessType.D2CC,
                DichotomyDirection.ES_FR, getZonalWithMinMax(), shiftDispatcher, 1., 1., intialNetPositions, processConfiguration);
        Mockito.when(processConfiguration.getShiftMaxIterationNumber()).thenReturn(5);
        assertThrows(GlskLimitationException.class, () -> sweNetworkShifter.shiftNetwork(11000., network));
    }

    @Test
    void updateScalingValuesWithMismatchPtEsTest() {
        SweNetworkShifter networkShifter = new SweNetworkShifter(businessLogger, ProcessType.D2CC, DichotomyDirection.PT_ES, zonalScalable, null, 10, 10, Map.of(), processConfiguration);
        Map<String, Double> scalingValuesByCountry = new HashMap<>(
            Map.of(EIC_FR, 12.0,
                EIC_ES, 27.0,
                EIC_PT, 1515.0));

        networkShifter.updateScalingValuesWithMismatch(scalingValuesByCountry, 5.0, 13.0);

        Assertions.assertThat(scalingValuesByCountry)
            .containsEntry(EIC_FR, 12.0)
            .containsEntry(EIC_ES, 45.0)
            .containsEntry(EIC_PT, 1510.0);
    }

    @Test
    void updateScalingValuesWithMismatchEsPtTest() {
        SweNetworkShifter networkShifter = new SweNetworkShifter(businessLogger, ProcessType.D2CC, DichotomyDirection.ES_PT, zonalScalable, null, 10, 10, Map.of(), processConfiguration);
        Map<String, Double> scalingValuesByCountry = new HashMap<>(
            Map.of(EIC_FR, 12.0,
                EIC_ES, 27.0,
                EIC_PT, 1515.0));

        networkShifter.updateScalingValuesWithMismatch(scalingValuesByCountry, 5.0, 13.0);

        Assertions.assertThat(scalingValuesByCountry)
            .containsEntry(EIC_FR, 12.0)
            .containsEntry(EIC_ES, 45.0)
            .containsEntry(EIC_PT, 1510.0);
    }

    @Test
    void updateScalingValuesWithMismatchFrEsTest() {
        SweNetworkShifter networkShifter = new SweNetworkShifter(businessLogger, ProcessType.D2CC, DichotomyDirection.FR_ES, zonalScalable, null, 10, 10, Map.of(), processConfiguration);
        Map<String, Double> scalingValuesByCountry = new HashMap<>(
            Map.of(EIC_FR, 12.0,
                EIC_ES, 27.0,
                EIC_PT, 1515.0));

        networkShifter.updateScalingValuesWithMismatch(scalingValuesByCountry, 5.0, 13.0);

        Assertions.assertThat(scalingValuesByCountry)
            .containsEntry(EIC_FR, -1.0)
            .containsEntry(EIC_ES, 45.0)
            .containsEntry(EIC_PT, 1515.0);
    }

    @Test
    void updateScalingValuesWithMismatchEsFrTest() {
        SweNetworkShifter networkShifter = new SweNetworkShifter(businessLogger, ProcessType.D2CC, DichotomyDirection.ES_FR, zonalScalable, null, 10, 10, Map.of(), processConfiguration);
        Map<String, Double> scalingValuesByCountry = new HashMap<>(
            Map.of(EIC_FR, 12.0,
                EIC_ES, 27.0,
                EIC_PT, 1515.0));

        networkShifter.updateScalingValuesWithMismatch(scalingValuesByCountry, 5.0, 13.0);

        Assertions.assertThat(scalingValuesByCountry)
            .containsEntry(EIC_FR, -1.0)
            .containsEntry(EIC_ES, 45.0)
            .containsEntry(EIC_PT, 1515.0);
    }
}

/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_swe_commons.shift;

import com.farao_community.farao.dichotomy.api.exceptions.GlskLimitationException;
import com.farao_community.farao.dichotomy.api.exceptions.ShiftingException;
import com.farao_community.farao.dichotomy.shift.ShiftDispatcher;
import com.farao_community.farao.gridcapa_swe_commons.configuration.ProcessConfiguration;
import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import com.farao_community.farao.gridcapa_swe_commons.resource.ProcessType;
import com.farao_community.farao.gridcapa_swe_commons.resource.SweEICode;
import com.powsybl.glsk.cim.CimGlskDocument;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.commons.ZonalDataImpl;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.commons.EICode;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@SpringBootTest(classes = ProcessConfiguration.class)
class SweNetworkShifterTest {

    private static final Double TARGET_P_TOLERANCE = 1e-3;

    @MockitoBean
    ProcessConfiguration processConfiguration;

    @MockitoBean
    private Logger businessLogger;
    private final String networkFileName = "/shift/TestCase_with_swe_countries.xiidm";
    private static ZonalDataImpl<Scalable> zonalScalable;

    @BeforeAll
    static void setup() {
        Scalable scalableFR = Scalable.onGenerator("FFR1AA11_generator");
        Scalable scalableES = Scalable.onGenerator("EES1AA11_generator");
        Scalable scalablePT = Scalable.onGenerator("PPT1AA11_generator");

        Map<String, Scalable> mapScalable = new HashMap<>();
        mapScalable.put(SweEICode.FR_EIC, scalableFR);
        mapScalable.put(SweEICode.ES_EIC, scalableES);
        mapScalable.put(SweEICode.PT_EIC, scalablePT);
        zonalScalable = new ZonalDataImpl<>(mapScalable);
    }

    @Test
    void checkD2ccEsFrTargetExchangesCalculatedCorrectly() {
        Map<String, Double> intialNetPositions = Map.of(SweEICode.ES_EIC, 50., SweEICode.FR_EIC, 100., SweEICode.PT_EIC, 150.);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(null, ProcessType.D2CC,
                DichotomyDirection.ES_FR, null, null, 0., 0., intialNetPositions, null, null, null);
        Map<String, Double> shifts = sweNetworkShifter.getTargetExchanges(1000);
        assertEquals(0., shifts.get("ES_PT"), 0.001);
        assertEquals(1000., shifts.get("ES_FR"), 0.001);
    }

    @Test
    void checkD2ccFrEsTargetExchangesCalculatedCorrectly() {
        Map<String, Double> intialNetPositions = Map.of(SweEICode.ES_EIC, 50., SweEICode.FR_EIC, 100., SweEICode.PT_EIC, 150.);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(null, ProcessType.D2CC,
                DichotomyDirection.FR_ES, null, null, 0., 0., intialNetPositions, null, null, null);
        Map<String, Double> shifts = sweNetworkShifter.getTargetExchanges(1000);
        assertEquals(0., shifts.get("ES_PT"), 0.001);
        assertEquals(-1000., shifts.get("ES_FR"), 0.001);
    }

    @Test
    void checkD2ccPtEsTargetExchangesCalculatedCorrectly() {
        Map<String, Double> intialNetPositions = Map.of(SweEICode.ES_EIC, 50., SweEICode.FR_EIC, 100., SweEICode.PT_EIC, 150.);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(null, ProcessType.D2CC,
                DichotomyDirection.PT_ES, null, null, 0., 0., intialNetPositions, null, null, null);
        Map<String, Double> shifts = sweNetworkShifter.getTargetExchanges(1000);
        assertEquals(-1000., shifts.get("ES_PT"), 0.001);
        assertEquals(0., shifts.get("ES_FR"), 0.001);
    }

    @Test
    void checkD2ccEsPtTargetExchangesCalculatedCorrectly() {
        Map<String, Double> intialNetPositions = Map.of(SweEICode.ES_EIC, 50., SweEICode.FR_EIC, 100., SweEICode.PT_EIC, 150.);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(null, ProcessType.D2CC,
                DichotomyDirection.ES_PT, null, null, 0., 0., intialNetPositions, null, null, null);
        Map<String, Double> shifts = sweNetworkShifter.getTargetExchanges(1000);
        assertEquals(1000., shifts.get("ES_PT"), 0.001);
        assertEquals(0., shifts.get("ES_FR"), 0.001);
    }

    @Test
    void checkIdccEsFrTargetExchangesCalculatedCorrectly() {
        Map<String, Double> intialNetPositions = Map.of(SweEICode.ES_EIC, 50., SweEICode.FR_EIC, 100., SweEICode.PT_EIC, 150.);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(null, ProcessType.IDCC,
                DichotomyDirection.ES_FR, null, null, 0., 0., intialNetPositions, null, null, null);
        Map<String, Double> shifts = sweNetworkShifter.getTargetExchanges(1000);
        assertEquals(-150., shifts.get("ES_PT"), 0.001);
        assertEquals(1000., shifts.get("ES_FR"), 0.001);
    }

    @Test
    void checkIdccFrEsTargetExchangesCalculatedCorrectly() {
        Map<String, Double> intialNetPositions = Map.of(SweEICode.ES_EIC, 50., SweEICode.FR_EIC, 100., SweEICode.PT_EIC, 150.);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(null, ProcessType.IDCC,
                DichotomyDirection.FR_ES, null, null, 0., 0., intialNetPositions, null, null, null);
        Map<String, Double> shifts = sweNetworkShifter.getTargetExchanges(1000);
        assertEquals(-150., shifts.get("ES_PT"), 0.001);
        assertEquals(-1000., shifts.get("ES_FR"), 0.001);
    }

    @Test
    void checkIdccEsPtTargetExchangesCalculatedCorrectly() {
        Map<String, Double> intialNetPositions = Map.of(SweEICode.ES_EIC, 50., SweEICode.FR_EIC, 100., SweEICode.PT_EIC, 150.);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(null, ProcessType.IDCC,
                DichotomyDirection.ES_PT, null, null, 0., 0., intialNetPositions, null, null, null);
        Map<String, Double> shifts = sweNetworkShifter.getTargetExchanges(1000);
        assertEquals(1000., shifts.get("ES_PT"), 0.001);
        assertEquals(200, shifts.get("ES_FR"), 0.001);
    }

    @Test
    void checkIdccPtEsTargetExchangesCalculatedCorrectly() {
        Map<String, Double> intialNetPositions = Map.of(SweEICode.ES_EIC, 50., SweEICode.FR_EIC, 100., SweEICode.PT_EIC, 150.);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(null, ProcessType.IDCC,
                DichotomyDirection.PT_ES, null, null, 0., 0., intialNetPositions, null, null, null);
        Map<String, Double> shifts = sweNetworkShifter.getTargetExchanges(1000);
        assertEquals(-1000., shifts.get("ES_PT"), 0.001);
        assertEquals(200., shifts.get("ES_FR"), 0.001);
    }

    @Test
    void shiftNetworkSuccessTest() throws GlskLimitationException, ShiftingException {
        Network network = Network.read(networkFileName, getClass().getResourceAsStream(networkFileName));
        Map<String, Double> intialNetPositions = Map.of(SweEICode.ES_EIC, 2317., SweEICode.FR_EIC, -2317., SweEICode.PT_EIC, 0.);
        ShiftDispatcher shiftDispatcher = new SweD2ccShiftDispatcher(DichotomyDirection.ES_FR, intialNetPositions);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(businessLogger, ProcessType.D2CC,
                DichotomyDirection.ES_FR, zonalScalable, shiftDispatcher, 1., 1., intialNetPositions, processConfiguration, LoadFlowParameters.load(), null);

        Mockito.when(processConfiguration.getShiftMaxIterationNumber()).thenReturn(5);
        sweNetworkShifter.shiftNetwork(1000., network);

        Map<String, Double> shiftedExchanges = CountryBalanceComputation.computeSweBordersExchanges(network);
        assertEquals(1000, shiftedExchanges.get("ES_FR"), 1.);
        assertEquals(0., shiftedExchanges.get("ES_PT"), 1.);

    }

    @Test
    void shiftNetworWithGlskLimitationEs() {
        Network network = Network.read(networkFileName, getClass().getResourceAsStream(networkFileName));
        Map<String, Double> intialNetPositions = Map.of(SweEICode.ES_EIC, 2317., SweEICode.FR_EIC, -2317., SweEICode.PT_EIC, 0.);
        ShiftDispatcher shiftDispatcher = new SweD2ccShiftDispatcher(DichotomyDirection.ES_FR, intialNetPositions);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(businessLogger, ProcessType.D2CC,
                DichotomyDirection.ES_FR, zonalScalable, shiftDispatcher, 1., 1., intialNetPositions, processConfiguration, LoadFlowParameters.load(), null);
        Mockito.when(processConfiguration.getShiftMaxIterationNumber()).thenReturn(5);
        assertThrows(GlskLimitationException.class, () -> sweNetworkShifter.shiftNetwork(11000., network));
    }

    @Test
    void shiftNetworSucceedWithIncompleteVariationEs() throws GlskLimitationException, ShiftingException {
        Network network = Network.read(networkFileName, getClass().getResourceAsStream(networkFileName));
        Map<String, Double> intialNetPositions = Map.of(SweEICode.ES_EIC, 2317., SweEICode.FR_EIC, -2317., SweEICode.PT_EIC, 0.);
        ShiftDispatcher shiftDispatcher = new SweD2ccShiftDispatcher(DichotomyDirection.ES_FR, intialNetPositions);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(businessLogger, ProcessType.D2CC,
                DichotomyDirection.ES_FR, zonalScalable, shiftDispatcher, 1., 1., intialNetPositions, processConfiguration, LoadFlowParameters.load(), null);
        Mockito.when(processConfiguration.getShiftMaxIterationNumber()).thenReturn(5);

        sweNetworkShifter.shiftNetwork(10820, network); // incomplete shift for ES in the first iteration , but no Glsk limitation error

        Map<String, Double> shiftedExchanges = CountryBalanceComputation.computeSweBordersExchanges(network);
        assertEquals(10820, shiftedExchanges.get("ES_FR"), 1.);
        assertEquals(0., shiftedExchanges.get("ES_PT"), 1.);

    }

    @Test
    void shiftNetworkSuccedWithIncompleteVariationFr() throws GlskLimitationException, ShiftingException {
        Network network = Network.read(networkFileName, getClass().getResourceAsStream(networkFileName));
        Map<String, Double> intialNetPositions = Map.of(SweEICode.ES_EIC, 2317., SweEICode.FR_EIC, -2317., SweEICode.PT_EIC, 0.);
        ShiftDispatcher shiftDispatcher = new SweD2ccShiftDispatcher(DichotomyDirection.FR_ES, intialNetPositions);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(businessLogger, ProcessType.D2CC,
                DichotomyDirection.FR_ES, zonalScalable, shiftDispatcher, 1., 1., intialNetPositions, processConfiguration, LoadFlowParameters.load(), null);
        Mockito.when(processConfiguration.getShiftMaxIterationNumber()).thenReturn(5);

        sweNetworkShifter.shiftNetwork(4645, network); // incomplete shift for FR in the first iteration , but no Glsk limitation error

        Map<String, Double> shiftedExchanges = CountryBalanceComputation.computeSweBordersExchanges(network);
        assertEquals(-4645, shiftedExchanges.get("ES_FR"), 1.);
        assertEquals(0., shiftedExchanges.get("ES_PT"), 1.);

    }

    @Test
    void shiftNetworkWithGlskLimitationForFr() {
        Network network = Network.read(networkFileName, getClass().getResourceAsStream(networkFileName));
        Map<String, Double> intialNetPositions = Map.of(SweEICode.ES_EIC, 2317., SweEICode.FR_EIC, -2317., SweEICode.PT_EIC, 0.);
        ShiftDispatcher shiftDispatcher = new SweD2ccShiftDispatcher(DichotomyDirection.FR_ES, intialNetPositions);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(businessLogger, ProcessType.D2CC,
                DichotomyDirection.FR_ES, zonalScalable, shiftDispatcher, 1., 1., intialNetPositions, processConfiguration, LoadFlowParameters.load(), null);
        Mockito.when(processConfiguration.getShiftMaxIterationNumber()).thenReturn(5);

        assertThrows(GlskLimitationException.class, () -> sweNetworkShifter.shiftNetwork(4647, network));

    }

    @Test
    void shiftNetworWithGlskLimitationPt() {
        Network network = Network.read(networkFileName, getClass().getResourceAsStream(networkFileName));
        Map<String, Double> intialNetPositions = Map.of(SweEICode.ES_EIC, 2317., SweEICode.FR_EIC, -2317., SweEICode.PT_EIC, 0.);
        ShiftDispatcher shiftDispatcher = new SweD2ccShiftDispatcher(DichotomyDirection.ES_PT, intialNetPositions);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(businessLogger, ProcessType.D2CC,
                DichotomyDirection.ES_PT, getZonalWithMinMax(), shiftDispatcher, 1., 1., intialNetPositions, processConfiguration, LoadFlowParameters.load(), null);
        Mockito.when(processConfiguration.getShiftMaxIterationNumber()).thenReturn(5);
        assertThrows(GlskLimitationException.class, () -> sweNetworkShifter.shiftNetwork(3000., network));
    }

    @Test
    void shiftNetworWithShiftingException() {
        Network network = Network.read(networkFileName, getClass().getResourceAsStream(networkFileName));
        Map<String, Double> intialNetPositions = Map.of(SweEICode.ES_EIC, 2317., SweEICode.FR_EIC, -2317., SweEICode.PT_EIC, 0.);
        ShiftDispatcher shiftDispatcher = new SweD2ccShiftDispatcher(DichotomyDirection.ES_FR, intialNetPositions);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(businessLogger, ProcessType.D2CC,
                DichotomyDirection.ES_FR, zonalScalable, shiftDispatcher, 1., 1., intialNetPositions, processConfiguration, LoadFlowParameters.load(), null);
        Mockito.when(processConfiguration.getShiftMaxIterationNumber()).thenReturn(1);
        assertThrows(ShiftingException.class, () -> sweNetworkShifter.shiftNetwork(1000., network));
    }

    ZonalDataImpl<Scalable> getZonalWithMinMax() {
        Scalable scalableFR = Scalable.onGenerator("FFR1AA11_generator", -9100.0, 9100.0);
        Scalable scalableES = Scalable.onGenerator("EES1AA11_generator", -9100.0, 9100.0);
        Scalable scalablePT = Scalable.onGenerator("PPT1AA11_generator", 0., 9100.0);

        Map<String, Scalable> mapScalable = new HashMap<>();
        mapScalable.put(SweEICode.FR_EIC, scalableFR);
        mapScalable.put(SweEICode.ES_EIC, scalableES);
        mapScalable.put(SweEICode.PT_EIC, scalablePT);
        return new ZonalDataImpl<>(mapScalable);
    }

    @Test
    void shiftNetworkSuccessWithChangePminPmaxFromGlskTest() throws GlskLimitationException, ShiftingException {
        Network network = Network.read(networkFileName, getClass().getResourceAsStream(networkFileName));
        Map<String, Double> intialNetPositions = Map.of(SweEICode.ES_EIC, 2310., SweEICode.FR_EIC, -2310., SweEICode.PT_EIC, 0.);
        ShiftDispatcher shiftDispatcher = new SweD2ccShiftDispatcher(DichotomyDirection.ES_FR, intialNetPositions);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(businessLogger, ProcessType.D2CC,
                DichotomyDirection.ES_FR, getZonalWithMinMax(), shiftDispatcher, 1., 1., intialNetPositions, processConfiguration, LoadFlowParameters.load(), null);

        Mockito.when(processConfiguration.getShiftMaxIterationNumber()).thenReturn(5);
        sweNetworkShifter.shiftNetwork(9050., network);

        Map<String, Double> shiftedExchanges = CountryBalanceComputation.computeSweBordersExchanges(network);
        assertEquals(9050., shiftedExchanges.get("ES_FR"), 1.);
        assertEquals(0., shiftedExchanges.get("ES_PT"), 1.);
    }

    @Test
    void shiftNetworWithGlskLimitationWithChangePminPmaxFromGlskTest() {
        Network network = Network.read(networkFileName, getClass().getResourceAsStream(networkFileName));
        Map<String, Double> intialNetPositions = Map.of(SweEICode.ES_EIC, 231., SweEICode.FR_EIC, -231., SweEICode.PT_EIC, 0.);
        ShiftDispatcher shiftDispatcher = new SweD2ccShiftDispatcher(DichotomyDirection.ES_FR, intialNetPositions);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(businessLogger, ProcessType.D2CC,
                DichotomyDirection.ES_FR, getZonalWithMinMax(), shiftDispatcher, 1., 1., intialNetPositions, processConfiguration, LoadFlowParameters.load(), null);
        Mockito.when(processConfiguration.getShiftMaxIterationNumber()).thenReturn(5);
        assertThrows(GlskLimitationException.class, () -> sweNetworkShifter.shiftNetwork(11000., network));
    }

    @Test
    void updateScalingValuesWithMismatchPtEsTest() {
        SweNetworkShifter networkShifter = new SweNetworkShifter(businessLogger, ProcessType.D2CC, DichotomyDirection.PT_ES, zonalScalable, null, 10, 10, Map.of(), processConfiguration, LoadFlowParameters.load(), null);
        Map<String, Double> scalingValuesByCountry = new HashMap<>(
                Map.of(SweEICode.FR_EIC, 12.0,
                        SweEICode.ES_EIC, 27.0,
                        SweEICode.PT_EIC, 1515.0));

        networkShifter.updateScalingValuesWithMismatch(scalingValuesByCountry, 5.0, 13.0);

        Assertions.assertThat(scalingValuesByCountry)
                .containsEntry(SweEICode.FR_EIC, 12.0)
                .containsEntry(SweEICode.ES_EIC, 45.0)
                .containsEntry(SweEICode.PT_EIC, 1510.0);
    }

    @Test
    void updateScalingValuesWithMismatchEsPtTest() {
        SweNetworkShifter networkShifter = new SweNetworkShifter(businessLogger, ProcessType.D2CC, DichotomyDirection.ES_PT, zonalScalable, null, 10, 10, Map.of(), processConfiguration, LoadFlowParameters.load(), null);
        Map<String, Double> scalingValuesByCountry = new HashMap<>(
                Map.of(SweEICode.FR_EIC, 12.0,
                        SweEICode.ES_EIC, 27.0,
                        SweEICode.PT_EIC, 1515.0));

        networkShifter.updateScalingValuesWithMismatch(scalingValuesByCountry, 5.0, 13.0);

        Assertions.assertThat(scalingValuesByCountry)
                .containsEntry(SweEICode.FR_EIC, 12.0)
                .containsEntry(SweEICode.ES_EIC, 45.0)
                .containsEntry(SweEICode.PT_EIC, 1510.0);
    }

    @Test
    void updateScalingValuesWithMismatchFrEsTest() {
        SweNetworkShifter networkShifter = new SweNetworkShifter(businessLogger, ProcessType.D2CC, DichotomyDirection.FR_ES, zonalScalable, null, 10, 10, Map.of(), processConfiguration, LoadFlowParameters.load(), null);
        Map<String, Double> scalingValuesByCountry = new HashMap<>(
                Map.of(SweEICode.FR_EIC, 12.0,
                        SweEICode.ES_EIC, 27.0,
                        SweEICode.PT_EIC, 1515.0));

        networkShifter.updateScalingValuesWithMismatch(scalingValuesByCountry, 5.0, 13.0);

        Assertions.assertThat(scalingValuesByCountry)
                .containsEntry(SweEICode.FR_EIC, -1.0)
                .containsEntry(SweEICode.ES_EIC, 45.0)
                .containsEntry(SweEICode.PT_EIC, 1515.0);
    }

    @Test
    void updateScalingValuesWithMismatchEsFrTest() {
        SweNetworkShifter networkShifter = new SweNetworkShifter(businessLogger, ProcessType.D2CC, DichotomyDirection.ES_FR, zonalScalable, null, 10, 10, Map.of(), processConfiguration, LoadFlowParameters.load(), null);
        Map<String, Double> scalingValuesByCountry = new HashMap<>(
                Map.of(SweEICode.FR_EIC, 12.0,
                        SweEICode.ES_EIC, 27.0,
                        SweEICode.PT_EIC, 1515.0));

        networkShifter.updateScalingValuesWithMismatch(scalingValuesByCountry, 5.0, 13.0);

        Assertions.assertThat(scalingValuesByCountry)
                .containsEntry(SweEICode.FR_EIC, -1.0)
                .containsEntry(SweEICode.ES_EIC, 45.0)
                .containsEntry(SweEICode.PT_EIC, 1515.0);
    }

    @Test
    void testConnectGeneratorsEs() throws GlskLimitationException, ShiftingException {
        Network network = Network.read("shift/TestCase_with_transformers.xiidm", getClass().getResourceAsStream("/shift/TestCase_with_transformers.xiidm"));
        CimGlskDocument doc = CimGlskDocument.importGlsk(getClass().getResourceAsStream("/shift/TestCase_with_transformers_glsk.xml"));
        Instant instant = LocalDateTime.of(2023, 7, 31, 7, 30).toInstant(ZoneOffset.UTC);
        ZonalData<Scalable> customZonalScalable = doc.getZonalScalable(network, instant);
        customZonalScalable.addAll(new ZonalDataImpl<>(Collections.singletonMap(new EICode(Country.FR).getAreaCode(), getCountryGeneratorsScalableForFR(network))));
        Map<String, Double> initialNetPositions = CountryBalanceComputation.computeSweCountriesBalances(network, LoadFlowParameters.load());
        ShiftDispatcher shiftDispatcher = new SweD2ccShiftDispatcher(DichotomyDirection.ES_FR, initialNetPositions);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(businessLogger, ProcessType.D2CC,
                DichotomyDirection.ES_FR, customZonalScalable, shiftDispatcher, 1., 1., initialNetPositions, processConfiguration, LoadFlowParameters.load(), null);
        Mockito.when(processConfiguration.getShiftMaxIterationNumber()).thenReturn(100);
        sweNetworkShifter.shiftNetwork(1000., network);

        assertEquals(Set.of("InitialState"), network.getVariantManager().getVariantIds());
        Map<String, Double> shiftedExchanges = CountryBalanceComputation.computeSweBordersExchanges(network);
        assertEquals(1000., shiftedExchanges.get("ES_FR"), 1.);
        assertEquals(0., shiftedExchanges.get("ES_PT"), 1.);
        // 1st generator in merit order is linked to grid through a line that is disconnected: it should remain that way and targetP remain at 0
        assertFalse(network.getGenerator("ESCDGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(0., network.getGenerator("ESCDGU1 _generator").getTargetP(), TARGET_P_TOLERANCE);
        // 2nd generator is connected to grid through 2 transformers that are disconnected: it should be reconnected and targetP set to 200 (its max)
        assertTrue(network.getGenerator("ESD2GU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(200., network.getGenerator("ESD2GU1 _generator").getTargetP(), TARGET_P_TOLERANCE);
        // 3rd generator is directly connected to grid: it should remain that way and targetP set to 1200 (its max)
        assertTrue(network.getGenerator("ESDCGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(1200., network.getGenerator("ESDCGU1 _generator").getTargetP(), TARGET_P_TOLERANCE);
        // 4th generator is connected to grid through a transformer that is connected: it should remain that way and targetP set to 1200 (its max)
        assertTrue(network.getGenerator("ESCTGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(1200., network.getGenerator("ESCTGU1 _generator").getTargetP(), TARGET_P_TOLERANCE);
        // 5th generator is directly connected to grid but its terminal is disconnected: it should be reconnected and targetP set to 200 (its max)
        assertTrue(network.getGenerator("ESDDGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(200., network.getGenerator("ESDDGU1 _generator").getTargetP(), TARGET_P_TOLERANCE);
        // 6th generator is connected to grid through a transformer but both are disconnected: they should be reconnected and
        // targetP set to 200 (to reach the 800MW) + some more to compensate for losses
        assertTrue(network.getGenerator("ESDTGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(233., network.getGenerator("ESDTGU1 _generator").getTargetP(), 2.);
        // +1000MW is already reached, the rest of the generators should not be changed nor scaled
        assertFalse(network.getGenerator("ESCDGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(0., network.getGenerator("ESCDGN1 _generator").getTargetP(), TARGET_P_TOLERANCE);
        assertFalse(network.getGenerator("ESD2GN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(0., network.getGenerator("ESD2GN1 _generator").getTargetP(), TARGET_P_TOLERANCE);
        assertTrue(network.getGenerator("ESDCGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(1000., network.getGenerator("ESDCGN1 _generator").getTargetP(), TARGET_P_TOLERANCE);
        assertTrue(network.getGenerator("ESCTGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(1000., network.getGenerator("ESCTGN1 _generator").getTargetP(), TARGET_P_TOLERANCE);
        assertFalse(network.getGenerator("ESDDGN1 _generator").getTerminal().isConnected());
        assertEquals(0., network.getGenerator("ESDDGN1 _generator").getTargetP(), TARGET_P_TOLERANCE);
        assertFalse(network.getGenerator("ESDTGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(0., network.getGenerator("ESDTGN1 _generator").getTargetP(), TARGET_P_TOLERANCE);
    }

    @Test
    void testConnectGeneratorsPt() throws GlskLimitationException, ShiftingException {
        // Same test as above but in the PR->ES direction
        Network network = Network.read("shift/TestCase_with_transformers.xiidm", getClass().getResourceAsStream("/shift/TestCase_with_transformers.xiidm"));

        CimGlskDocument doc = CimGlskDocument.importGlsk(getClass().getResourceAsStream("/shift/TestCase_with_transformers_glsk.xml"));
        Instant instant = LocalDateTime.of(2023, 7, 31, 7, 30).toInstant(ZoneOffset.UTC);
        ZonalData<Scalable> customZonalScalable = doc.getZonalScalable(network, instant);
        customZonalScalable.addAll(new ZonalDataImpl<>(Collections.singletonMap(new EICode(Country.FR).getAreaCode(), getCountryGeneratorsScalableForFR(network))));
        Map<String, Double> initialNetPositions = CountryBalanceComputation.computeSweCountriesBalances(network, LoadFlowParameters.load());
        ShiftDispatcher shiftDispatcher = new SweD2ccShiftDispatcher(DichotomyDirection.PT_ES, initialNetPositions);
        SweNetworkShifter sweNetworkShifter = new SweNetworkShifter(businessLogger, ProcessType.D2CC,
                DichotomyDirection.PT_ES, customZonalScalable, shiftDispatcher, 1., 1., initialNetPositions, processConfiguration, LoadFlowParameters.load(), null);
        Mockito.when(processConfiguration.getShiftMaxIterationNumber()).thenReturn(100);
        sweNetworkShifter.shiftNetwork(1000., network);

        assertEquals(Set.of("InitialState"), network.getVariantManager().getVariantIds());
        Map<String, Double> shiftedExchanges = CountryBalanceComputation.computeSweBordersExchanges(network);
        assertEquals(-1000., shiftedExchanges.get("ES_PT"), 1.);
        assertEquals(0., shiftedExchanges.get("ES_FR"), 1.);
        // 1st generator in merit order is linked to grid through a line that is disconnected: it should remain that way and targetP remain at 0
        assertFalse(network.getGenerator("PTCDGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(0., network.getGenerator("PTCDGU1 _generator").getTargetP(), TARGET_P_TOLERANCE);
        // 2nd generator is connected to grid through 2 transformers that are disconnected: it should be reconnected and targetP set to 200 (its max)
        assertTrue(network.getGenerator("PTD2GU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(200., network.getGenerator("PTD2GU1 _generator").getTargetP(), TARGET_P_TOLERANCE);
        // 3rd generator is directly connected to grid: it should remain that way and targetP set to 1200 (its max)
        assertTrue(network.getGenerator("PTDCGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(1200., network.getGenerator("PTDCGU1 _generator").getTargetP(), TARGET_P_TOLERANCE);
        // 4th generator is connected to grid through a transformer that is connected: it should remain that way and targetP set to 1200 (its max)
        assertTrue(network.getGenerator("PTCTGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(1200., network.getGenerator("PTCTGU1 _generator").getTargetP(), TARGET_P_TOLERANCE);
        // 5th generator is directly connected to grid but its terminal is disconnected: it should be reconnected and targetP set to 200 (its max)
        assertTrue(network.getGenerator("PTDDGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(200., network.getGenerator("PTDDGU1 _generator").getTargetP(), TARGET_P_TOLERANCE);
        // 6th generator is connected to grid through a transformer but both are disconnected: they should be reconnected and
        // targetP set to 200 (to reach the 800MW) + some more to compensate for losses
        assertTrue(network.getGenerator("PTDTGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(233., network.getGenerator("PTDTGU1 _generator").getTargetP(), 2.);
        // +1000MW is already reached, the rest of the generators should not be changed nor scaled
        assertFalse(network.getGenerator("PTCDGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(0., network.getGenerator("PTCDGN1 _generator").getTargetP(), TARGET_P_TOLERANCE);
        assertFalse(network.getGenerator("PTD2GN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(0., network.getGenerator("PTD2GN1 _generator").getTargetP(), TARGET_P_TOLERANCE);
        assertTrue(network.getGenerator("PTDCGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(1000., network.getGenerator("PTDCGN1 _generator").getTargetP(), TARGET_P_TOLERANCE);
        assertTrue(network.getGenerator("PTCTGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(1000., network.getGenerator("PTCTGN1 _generator").getTargetP(), TARGET_P_TOLERANCE);
        assertFalse(network.getGenerator("PTDDGN1 _generator").getTerminal().isConnected());
        assertEquals(0., network.getGenerator("PTDDGN1 _generator").getTargetP(), TARGET_P_TOLERANCE);
        assertFalse(network.getGenerator("PTDTGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(0., network.getGenerator("PTDTGN1 _generator").getTargetP(), TARGET_P_TOLERANCE);
    }

    private Scalable getCountryGeneratorsScalableForFR(Network network) {
        List<Scalable> scalables = new ArrayList<>();
        List<Double> percentages = new ArrayList<>();
        List<Generator> generators;
        generators = network.getGeneratorStream()
                .filter(generator -> Country.FR.equals(generator.getTerminal().getVoltageLevel().getSubstation().map(Substation::getNullableCountry).orElse(null)))
                .filter(NetworkUtil::isCorrect)
                .toList();
        //calculate sum P of country's generators
        double totalCountryP = generators.stream().mapToDouble(NetworkUtil::pseudoTargetP).sum();
        //calculate factor of each generator
        generators.forEach(generator -> {
            double generatorPercentage = 100 * NetworkUtil.pseudoTargetP(generator) / totalCountryP;
            percentages.add(generatorPercentage);
            scalables.add(Scalable.onGenerator(generator.getId()));
        });
        return Scalable.proportional(percentages, scalables);
    }
}

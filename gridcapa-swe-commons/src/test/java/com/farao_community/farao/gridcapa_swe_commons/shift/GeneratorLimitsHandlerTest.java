/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_swe_commons.shift;

import com.powsybl.glsk.cim.CimGlskDocument;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.commons.ZonalDataImpl;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.openrao.commons.EICode;
import org.apache.commons.math3.util.Precision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
class GeneratorLimitsHandlerTest {
    private Network network;
    private GeneratorLimitsHandler generatorLimitsHandler;
    private ZonalData<Scalable> zonalScalable;

    private static final double DEFAULT_PMAX = 9999.0;
    private static final double DEFAULT_PMIN = -9999.0;

    private static final double INITIAL_PMAX = 9000.0;
    private static final double INITIAL_PMIN = -9000.0;

    @BeforeEach
    void setUp() {
        network = Network.read("shift/TestCase_with_transformers.xiidm", getClass().getResourceAsStream("/shift/TestCase_with_transformers.xiidm"));
        CimGlskDocument doc = CimGlskDocument.importGlsk(getClass().getResourceAsStream("/shift/TestCase_with_transformers_glsk.xml"));
        Instant instant = LocalDateTime.of(2023, 7, 31, 7, 30).toInstant(ZoneOffset.UTC);
        zonalScalable = doc.getZonalScalable(network, instant);
        zonalScalable.addAll(new ZonalDataImpl<>(Collections.singletonMap(new EICode(Country.FR).getAreaCode(), getCountryGeneratorsScalableForFR(network))));
        generatorLimitsHandler = new GeneratorLimitsHandler(zonalScalable);
        generatorLimitsHandler.setPminPmaxToDefaultValue(network, Set.of(Country.ES, Country.PT));
    }

    @Test
    void testLimitsChangedToDefaultValue() {
        // All generators for ES have default Pmin, Pmax
        assertTrue(isCountryGeneratorPminEquals(Country.ES, DEFAULT_PMIN));
        assertTrue(isCountryGeneratorPmaxEquals(Country.ES, DEFAULT_PMAX));
        // All generators for PT have default Pmin, Pmax
        assertTrue(isCountryGeneratorPminEquals(Country.PT, DEFAULT_PMIN));
        assertTrue(isCountryGeneratorPmaxEquals(Country.PT, DEFAULT_PMAX));
        // Nothing change for generators FR Pmin = -9000., Pmax = 9000
        assertTrue(isCountryGeneratorPminEquals(Country.FR, INITIAL_PMIN));
        assertTrue(isCountryGeneratorPmaxEquals(Country.FR, INITIAL_PMAX));
    }

    private boolean isCountryGeneratorPminEquals(Country country, double expectedValue) {
        return zonalScalable.getData(new EICode(country).getAreaCode()).filterInjections(network).stream().map(injection -> (Generator) injection)
                .allMatch(generator -> Precision.equals(generator.getMinP(), expectedValue));
    }

    private boolean isCountryGeneratorPmaxEquals(Country country, double expectedValue) {
        return zonalScalable.getData(new EICode(country).getAreaCode()).filterInjections(network).stream().map(injection -> (Generator) injection)
                .allMatch(generator -> Precision.equals(generator.getMaxP(), expectedValue));
    }

    @Test
    void testResetInitialLimits() {
        // For this generator targetP > maxP
        network.getGenerator("ESCTGU1 _generator").setTargetP(9001);
        generatorLimitsHandler.resetInitialPminPmax(network);
        //If targetP > initial Pmax, the new Pmax should correspond to targetP
        assertEquals(9001., network.getGenerator("ESCTGU1 _generator").getMaxP(), 0.01);
        // All generators for ES "ESCTGU1 _generator" have initial Pmin, Pmax
        assertTrue(zonalScalable.getData(new EICode(Country.ES).getAreaCode()).filterInjections(network).stream().map(injection -> (Generator) injection)
                .filter(generator -> !"ESCTGU1 _generator".equals(generator.getId()))
                .allMatch(generator -> Precision.equals(generator.getMinP(), INITIAL_PMIN)));
        assertTrue(zonalScalable.getData(new EICode(Country.ES).getAreaCode()).filterInjections(network).stream().map(injection -> (Generator) injection)
                .filter(generator -> !"ESCTGU1 _generator".equals(generator.getId()))
                .allMatch(generator -> Precision.equals(generator.getMaxP(), INITIAL_PMAX)));
        // All generators for PT have initial Pmin, Pmax
        assertTrue(isCountryGeneratorPminEquals(Country.PT, INITIAL_PMIN));
        assertTrue(isCountryGeneratorPmaxEquals(Country.PT, INITIAL_PMAX));
        // Nothing change for generators FR Pmin = -9000., Pmax = 9000
        assertTrue(isCountryGeneratorPminEquals(Country.FR, INITIAL_PMIN));
        assertTrue(isCountryGeneratorPmaxEquals(Country.FR, INITIAL_PMAX));
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

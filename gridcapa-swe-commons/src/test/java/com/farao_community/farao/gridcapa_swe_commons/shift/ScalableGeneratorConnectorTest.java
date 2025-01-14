/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_swe_commons.shift;

import com.farao_community.farao.dichotomy.api.exceptions.ShiftingException;
import com.powsybl.glsk.cim.CimGlskDocument;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.commons.ZonalDataImpl;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.modification.scalable.ScalingParameters;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.openrao.commons.EICode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class ScalableGeneratorConnectorTest {
    private Network network;
    private ScalableGeneratorConnector scalableGeneratorConnector;
    private ZonalData<Scalable> zonalScalable;
    private ScalingParameters scalingParameters;

    @BeforeEach
    void setUp() throws ShiftingException {
        network = Network.read("shift/TestCase_with_transformers.xiidm", getClass().getResourceAsStream("/shift/TestCase_with_transformers.xiidm"));
        CimGlskDocument doc = CimGlskDocument.importGlsk(getClass().getResourceAsStream("/shift/TestCase_with_transformers_glsk.xml"));
        Instant instant = LocalDateTime.of(2023, 7, 31, 7, 30).toInstant(ZoneOffset.UTC);
        zonalScalable = doc.getZonalScalable(network, instant);
        zonalScalable.addAll(new ZonalDataImpl<>(Collections.singletonMap(new EICode(Country.FR).getAreaCode(), getCountryGeneratorsScalableForFR(network))));
        scalingParameters = new ScalingParameters().setReconnect(true);
        scalableGeneratorConnector = new ScalableGeneratorConnector(zonalScalable);
        scalableGeneratorConnector.fillGeneratorsInitialState(network, Set.of(Country.ES, Country.PT));
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

    @Test
    void testFillGeneratorsInitialState() {
        Map<String, ScalableGeneratorConnector.GeneratorState> changedGeneratorsInitialState = scalableGeneratorConnector.getChangedGeneratorsInitialState();
        //Generators not connected to the main component for ES
        assertTrue(changedGeneratorsInitialState.containsKey("ESCDGU1 _generator"));
        assertTrue(changedGeneratorsInitialState.containsKey("ESCDGN1 _generator"));
        assertTrue(changedGeneratorsInitialState.containsKey("ESD2GU1 _generator"));
        assertTrue(changedGeneratorsInitialState.containsKey("ESD2GN1 _generator"));
        assertTrue(changedGeneratorsInitialState.containsKey("ESDTGU1 _generator"));
        assertTrue(changedGeneratorsInitialState.containsKey("ESDTGN1 _generator"));
        //Generators not connected to the main component for ES
        assertFalse(changedGeneratorsInitialState.containsKey("ESDCGU1 _generator"));
        assertFalse(changedGeneratorsInitialState.containsKey("ESDCGN1 _generator"));
        assertFalse(changedGeneratorsInitialState.containsKey("ESCTGU1 _generator"));
        assertFalse(changedGeneratorsInitialState.containsKey("ESDDGN1 _generator"));

        // No FR generators in the changedGeneratorsInitialState
        assertTrue(changedGeneratorsInitialState.keySet().stream().filter(id -> id.startsWith("FR")).collect(Collectors.toSet()).isEmpty());

        //Generators not connected to the main component for PT
        assertTrue(changedGeneratorsInitialState.containsKey("PTCDGU1 _generator"));
        assertTrue(changedGeneratorsInitialState.containsKey("PTD2GU1 _generator"));
        assertTrue(changedGeneratorsInitialState.containsKey("PTDTGU1 _generator"));
        assertTrue(changedGeneratorsInitialState.containsKey("PTD2GN1 _generator"));
        assertTrue(changedGeneratorsInitialState.containsKey("PTCDGN1 _generator"));
        assertTrue(changedGeneratorsInitialState.containsKey("PTDTGN1 _generator"));

    }

    @Test
    void testScalingFr() {
        assertFalse(network.getGenerator("FRCDGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertFalse(network.getGenerator("FRCDGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertFalse(network.getGenerator("FRD2GU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertFalse(network.getGenerator("FRD2GN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertTrue(network.getGenerator("FRDCGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertTrue(network.getGenerator("FRDCGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertTrue(network.getGenerator("FRCTGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertTrue(network.getGenerator("FRCTGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertFalse(network.getGenerator("FRDDGU1 _generator").getTerminal().isConnected());
        assertFalse(network.getGenerator("FRDDGN1 _generator").getTerminal().isConnected());
        assertFalse(network.getGenerator("FRDTGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertFalse(network.getGenerator("FRDTGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        double done = zonalScalable.getData(new EICode(Country.FR).getAreaCode()).scale(network, 2000, scalingParameters);
        assertEquals(2000., done, 0.1);
        scalableGeneratorConnector.connectGeneratorsTransformers(network, Set.of(Country.ES, Country.PT));
        // connectGeneratorsTransformers should not have effect in FR
        assertFalse(network.getGenerator("FRCDGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertFalse(network.getGenerator("FRCDGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertFalse(network.getGenerator("FRD2GU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertFalse(network.getGenerator("FRD2GN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertTrue(network.getGenerator("FRDCGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertTrue(network.getGenerator("FRDCGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertTrue(network.getGenerator("FRCTGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertTrue(network.getGenerator("FRCTGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertFalse(network.getGenerator("FRDDGU1 _generator").getTerminal().isConnected());
        assertFalse(network.getGenerator("FRDDGN1 _generator").getTerminal().isConnected());
        assertFalse(network.getGenerator("FRDTGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertFalse(network.getGenerator("FRDTGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
    }

    @Test
    void testScalingEs() {
        double done = zonalScalable.getData(new EICode(Country.ES).getAreaCode()).scale(network, 2000, scalingParameters);
        assertEquals(2000., done, 0.1);
        scalableGeneratorConnector.connectGeneratorsTransformers(network, Set.of(Country.ES, Country.PT));
        // These generators in merit order are linked to grid through a line that is disconnected: it should remain that way
        assertFalse(network.getGenerator("ESCDGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(0., network.getGenerator("ESCDGU1 _generator").getTargetP(), 0.01);
        // These generators are connected to grid through 2 transformers that are disconnected: they should be reconnected
        assertTrue(network.getGenerator("ESD2GU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(200., network.getGenerator("ESD2GU1 _generator").getTargetP(), 0.01);
        // These generators are directly connected to grid: it should remain that way
        assertTrue(network.getGenerator("ESDCGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(1200., network.getGenerator("ESDCGU1 _generator").getTargetP(), 0.01);

        // These generators are connected to grid through a transformer that is connected: it should remain that way
        assertTrue(network.getGenerator("ESCTGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(1200., network.getGenerator("ESCTGU1 _generator").getTargetP(), 0.01);

        // These generators are directly connected to grid but its terminal is disconnected: they should be reconnected
        assertTrue(network.getGenerator("ESDDGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(200., network.getGenerator("ESDDGU1 _generator").getTargetP(), 0.01);

        // These generators are connected to grid through a transformer but both are disconnected: they should be reconnected
        assertTrue(network.getGenerator("ESDTGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(250., network.getGenerator("ESDTGU1 _generator").getTargetP(), 0.01);

        // These generators in merit order are linked to grid through a line that is disconnected: it should remain that way
        assertFalse(network.getGenerator("ESCDGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(0., network.getGenerator("ESCDGN1 _generator").getTargetP(), 0.01);

        // These generators are connected to grid through 2 transformers that are disconnected: they should be reconnected
        assertTrue(network.getGenerator("ESD2GN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(200., network.getGenerator("ESD2GN1 _generator").getTargetP(), 0.01);

        // These generators are directly connected to grid: it should remain that way
        assertTrue(network.getGenerator("ESDCGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(1200., network.getGenerator("ESDCGN1 _generator").getTargetP(), 0.01);

        // These generators are connected to grid through a transformer that is connected: it should remain that way
        assertTrue(network.getGenerator("ESCTGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(1150, network.getGenerator("ESCTGN1 _generator").getTargetP(), 0.01); //The last generator shifted for 2000

        // These generators are directly connected to grid but its terminal is disconnected, it is not used in the shift of 2000, it should not be connected
        assertFalse(network.getGenerator("ESDDGN1 _generator").getTerminal().isConnected());
        assertTrue(network.getGenerator("ESDDGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(0., network.getGenerator("ESDDGN1 _generator").getTargetP(), 0.01);
        // These generators are connected to grid through a transformer but both are disconnected, but not used for the shift, it should be disconnected
        assertFalse(network.getGenerator("ESDTGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(0., network.getGenerator("ESDTGN1 _generator").getTargetP(), 0.01);
    }

    @Test
    void testScalingPt() {
        ScalingParameters customScalingParameters = new ScalingParameters().setReconnect(true);
        double done = zonalScalable.getData(new EICode(Country.PT).getAreaCode()).scale(network, 2000, customScalingParameters);
        assertEquals(2000., done, 0.1);
        scalableGeneratorConnector.connectGeneratorsTransformers(network, Set.of(Country.ES, Country.PT));
        // These generators in merit order are linked to grid through a line that is disconnected: it should remain that way
        assertFalse(network.getGenerator("PTCDGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(0., network.getGenerator("PTCDGU1 _generator").getTargetP(), 0.01);
        // These generators are connected to grid through 2 transformers that are disconnected: they should be reconnected
        assertTrue(network.getGenerator("PTD2GU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(200., network.getGenerator("PTD2GU1 _generator").getTargetP(), 0.01);
        // These generators are directly connected to grid: it should remain that way
        assertTrue(network.getGenerator("PTDCGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(1200., network.getGenerator("PTDCGU1 _generator").getTargetP(), 0.01);

        // These generators are connected to grid through a transformer that is connected: it should remain that way
        assertTrue(network.getGenerator("PTCTGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(1200., network.getGenerator("PTCTGU1 _generator").getTargetP(), 0.01);

        // These generators are directly connected to grid but its terminal is disconnected: it should be reconnected
        assertTrue(network.getGenerator("PTDDGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertTrue(network.getGenerator("PTDDGU1 _generator").getTerminal().isConnected());
        assertEquals(200, network.getGenerator("PTDDGU1 _generator").getTargetP(), 0.01);

        // These generators are connected to grid through a transformer but both are disconnected: they should be reconnected
        assertTrue(network.getGenerator("PTDTGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(250., network.getGenerator("PTDTGU1 _generator").getTargetP(), 0.01);

        // These generators in merit order are linked to grid through a line that is disconnected: it should remain that way
        assertFalse(network.getGenerator("PTCDGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(0., network.getGenerator("PTCDGN1 _generator").getTargetP(), 0.01);

        // These generators are connected to grid through 2 transformers that are disconnected: they should be reconnected
        assertTrue(network.getGenerator("PTD2GN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(200., network.getGenerator("PTD2GN1 _generator").getTargetP(), 0.01);

        // These generators are directly connected to grid: it should remain that way
        assertTrue(network.getGenerator("PTDCGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(1200., network.getGenerator("PTDCGN1 _generator").getTargetP(), 0.01);

        // These generators are connected to grid through a transformer that is connected: it should remain that way
        assertTrue(network.getGenerator("PTCTGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(1150., network.getGenerator("PTCTGN1 _generator").getTargetP(), 0.01);

        // These generators are directly connected to grid, but generator terminal disconnected. As it not used for the shift, the terminal will not be connected
        assertTrue(network.getGenerator("PTDDGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertFalse(network.getGenerator("PTDDGN1 _generator").getTerminal().isConnected());
        assertEquals(0., network.getGenerator("PTDDGN1 _generator").getTargetP(), 0.01);

        // These generators are connected to grid through a transformer but both are disconnected, but it's not used in the merit order shift 2000
        assertFalse(network.getGenerator("PTDTGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertEquals(0., network.getGenerator("PTDTGN1 _generator").getTargetP(), 0.01);
    }
}

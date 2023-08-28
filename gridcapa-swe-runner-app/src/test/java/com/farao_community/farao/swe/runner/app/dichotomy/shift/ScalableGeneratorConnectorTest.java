/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy.shift;

import com.farao_community.farao.commons.EICode;
import com.farao_community.farao.dichotomy.api.exceptions.ShiftingException;
import com.powsybl.glsk.cim.CimGlskDocument;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.commons.ZonalDataImpl;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class ScalableGeneratorConnectorTest {
    private Network network;
    private ScalableGeneratorConnector scalableGeneratorConnector;

    @BeforeEach
    void setUp() throws ShiftingException {
        network = Network.read("shift/TestCase_with_transformers.xiidm", getClass().getResourceAsStream("/shift/TestCase_with_transformers.xiidm"));
        CimGlskDocument doc = CimGlskDocument.importGlsk(getClass().getResourceAsStream("/shift/TestCase_with_transformers_glsk.xml"));
        Instant instant = LocalDateTime.of(2023, 7, 31, 7, 30).toInstant(ZoneOffset.UTC);
        ZonalData<Scalable> zonalScalable = doc.getZonalScalable(network, instant);
        zonalScalable.addAll(new ZonalDataImpl<>(Collections.singletonMap(new EICode(Country.FR).getAreaCode(), getCountryGeneratorsScalableForFR(network))));
        scalableGeneratorConnector = new ScalableGeneratorConnector(zonalScalable);
        scalableGeneratorConnector.prepareForScaling(network);
    }

    private Scalable getCountryGeneratorsScalableForFR(Network network) {
        List<Scalable> scalables = new ArrayList<>();
        List<Float> percentages = new ArrayList<>();
        List<Generator> generators;
        generators = network.getGeneratorStream()
            .filter(generator -> Country.FR.equals(generator.getTerminal().getVoltageLevel().getSubstation().map(Substation::getNullableCountry).orElse(null)))
            .filter(NetworkUtil::isCorrect)
            .collect(Collectors.toList());
        //calculate sum P of country's generators
        double totalCountryP = generators.stream().mapToDouble(NetworkUtil::pseudoTargetP).sum();
        //calculate factor of each generator
        generators.forEach(generator -> {
            float generatorPercentage = (float) (100 * NetworkUtil.pseudoTargetP(generator) / totalCountryP);
            percentages.add(generatorPercentage);
            scalables.add(Scalable.onGenerator(generator.getId()));
        });
        return Scalable.proportional(percentages, scalables);
    }

    @Test
    void tetPrepareForScalingFr() {
        // Preparation for scaling should have no effect on FR
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
    void tetRevertChangesFr() {
        scalableGeneratorConnector.revertUnnecessaryChanges(network);
        // Preparation for scaling should have no effect on FR
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
    void tetPrepareForScalingEs() {
        // These generators in merit order are linked to grid through a line that is disconnected: it should remain that way
        assertFalse(network.getGenerator("ESCDGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertFalse(network.getGenerator("ESCDGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        // These generators are connected to grid through 2 transformers that are disconnected: they should remain that way
        assertFalse(network.getGenerator("ESD2GU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertFalse(network.getGenerator("ESD2GN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        // These generators are directly connected to grid: it should remain that way
        assertTrue(network.getGenerator("ESDCGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertTrue(network.getGenerator("ESDCGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        // These generators are connected to grid through a transformer that is connected: it should remain that way
        assertTrue(network.getGenerator("ESCTGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertTrue(network.getGenerator("ESCTGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        // These generators are directly connected to grid but its terminal is disconnected: it should be reconnected
        assertTrue(network.getGenerator("ESDDGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertTrue(network.getGenerator("ESDDGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        // These generators are connected to grid through a transformer but both are disconnected: they should be reconnected
        assertTrue(network.getGenerator("ESDTGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertTrue(network.getGenerator("ESDTGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
    }

    @Test
    void tetRevertChangesEs() {
        scalableGeneratorConnector.revertUnnecessaryChanges(network);
        assertFalse(network.getGenerator("ESCDGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertFalse(network.getGenerator("ESCDGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertFalse(network.getGenerator("ESD2GU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertFalse(network.getGenerator("ESD2GN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertTrue(network.getGenerator("ESDCGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertTrue(network.getGenerator("ESDCGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertTrue(network.getGenerator("ESCTGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertTrue(network.getGenerator("ESCTGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertFalse(network.getGenerator("ESDDGU1 _generator").getTerminal().isConnected());
        assertFalse(network.getGenerator("ESDDGN1 _generator").getTerminal().isConnected());
        assertFalse(network.getGenerator("ESDTGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertFalse(network.getGenerator("ESDTGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
    }

    @Test
    void tetPrepareForScalingPt() {
        // These generators in merit order are linked to grid through a line that is disconnected: it should remain that way
        assertFalse(network.getGenerator("PTCDGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertFalse(network.getGenerator("PTCDGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        // These generators are connected to grid through 2 transformers that are disconnected: they should remain that way
        assertFalse(network.getGenerator("PTD2GU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertFalse(network.getGenerator("PTD2GN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        // These generators are directly connected to grid: it should remain that way
        assertTrue(network.getGenerator("PTDCGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertTrue(network.getGenerator("PTDCGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        // These generators are connected to grid through a transformer that is connected: it should remain that way
        assertTrue(network.getGenerator("PTCTGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertTrue(network.getGenerator("PTCTGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        // These generators are directly connected to grid but its terminal is disconnected: it should be reconnected
        assertTrue(network.getGenerator("PTDDGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertTrue(network.getGenerator("PTDDGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        // These generators are connected to grid through a transformer but both are disconnected: they should be reconnected
        assertTrue(network.getGenerator("PTDTGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertTrue(network.getGenerator("PTDTGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
    }

    @Test
    void tetRevertChangesPt() {
        scalableGeneratorConnector.revertUnnecessaryChanges(network);
        assertFalse(network.getGenerator("PTCDGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertFalse(network.getGenerator("PTCDGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertFalse(network.getGenerator("PTD2GU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertFalse(network.getGenerator("PTD2GN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertTrue(network.getGenerator("PTDCGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertTrue(network.getGenerator("PTDCGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertTrue(network.getGenerator("PTCTGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertTrue(network.getGenerator("PTCTGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertFalse(network.getGenerator("PTDDGU1 _generator").getTerminal().isConnected());
        assertFalse(network.getGenerator("PTDDGN1 _generator").getTerminal().isConnected());
        assertFalse(network.getGenerator("PTDTGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertFalse(network.getGenerator("PTDTGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
    }

    @Test
    void testRevertOnlyUnnecessaryChanges() {
        network.getGenerator("ESDDGU1 _generator").getTerminal().connect();
        network.getGenerator("ESDDGU1 _generator").setTargetP(100.);

        network.getGenerator("ESDTGN1 _generator").getTerminal().connect();
        network.getGenerator("ESDTGN1 _generator").setTargetP(10.);

        network.getGenerator("PTDTGU1 _generator").getTerminal().connect();
        network.getGenerator("PTDTGU1 _generator").setTargetP(10.);

        scalableGeneratorConnector.revertUnnecessaryChanges(network);

        assertFalse(network.getGenerator("ESCDGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertFalse(network.getGenerator("ESCDGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertFalse(network.getGenerator("ESD2GU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertFalse(network.getGenerator("ESD2GN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertTrue(network.getGenerator("ESDCGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertTrue(network.getGenerator("ESDCGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertTrue(network.getGenerator("ESCTGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertTrue(network.getGenerator("ESCTGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertTrue(network.getGenerator("ESDDGU1 _generator").getTerminal().isConnected());
        assertFalse(network.getGenerator("ESDDGN1 _generator").getTerminal().isConnected());
        assertFalse(network.getGenerator("ESDTGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertTrue(network.getGenerator("ESDTGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());

        assertFalse(network.getGenerator("PTCDGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertFalse(network.getGenerator("PTCDGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertFalse(network.getGenerator("PTD2GU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertFalse(network.getGenerator("PTD2GN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertTrue(network.getGenerator("PTDCGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertTrue(network.getGenerator("PTDCGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertTrue(network.getGenerator("PTCTGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertTrue(network.getGenerator("PTCTGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertFalse(network.getGenerator("PTDDGU1 _generator").getTerminal().isConnected());
        assertFalse(network.getGenerator("PTDDGN1 _generator").getTerminal().isConnected());
        assertTrue(network.getGenerator("PTDTGU1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
        assertFalse(network.getGenerator("PTDTGN1 _generator").getTerminal().getBusBreakerView().getConnectableBus().isInMainSynchronousComponent());
    }
}

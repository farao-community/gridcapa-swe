/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_swe_commons.hvdc;

import com.farao_community.farao.gridcapa_swe_commons.exception.SweInvalidDataNoDetailsException;
import com.farao_community.farao.gridcapa_swe_commons.hvdc.parameters.HvdcCreationParameters;
import com.farao_community.farao.gridcapa_swe_commons.hvdc.parameters.SwePreprocessorParameters;
import com.farao_community.farao.gridcapa_swe_commons.hvdc.parameters.json.JsonSwePreprocessorImporter;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Mitri {@literal <peter.mitri@rte-france.com>}
 */
class HvdcLinkProcessorTest {

    @Test
    void testHvdcCreationRoundTrip() {
        // Test that when we create the HVDC then remove it, the network is the same
        Network network = Network.read("hvdc/TestCase16Nodes.xiidm", getClass().getResourceAsStream("/hvdc/TestCase16Nodes.xiidm"));
        SwePreprocessorParameters params = JsonSwePreprocessorImporter.read(getClass().getResourceAsStream("/hvdc/SwePreprocessorParameters_16nodes.json"));
        HvdcLinkProcessor.replaceEquivalentModelByHvdc(network, params.getHvdcCreationParametersSet());
        HvdcLinkProcessor.replaceHvdcByEquivalentModel(network, params.getHvdcCreationParametersSet(), null);
        TestUtils.assertNetworksAreEqual(network, "/hvdc/TestCase16Nodes.xiidm", getClass());
    }

    @Test
    void testHvdcCreation() {
        // Inspect the contents of the created HVDC lines
        Network network = Network.read("hvdc/TestCase16Nodes.xiidm", getClass().getResourceAsStream("/hvdc/TestCase16Nodes.xiidm"));
        SwePreprocessorParameters params = JsonSwePreprocessorImporter.read(getClass().getResourceAsStream("/hvdc/SwePreprocessorParameters_16nodes.json"));
        HvdcLinkProcessor.replaceEquivalentModelByHvdc(network, params.getHvdcCreationParametersSet());
        // Check generators was disconnected
        assertFalse(network.getGenerator("FFR4AA1 _generator").getTerminal().isConnected());
        assertFalse(network.getGenerator("DDE1AA1 _generator").getTerminal().isConnected());
        assertFalse(network.getGenerator("BBE2AA1 _generator").getTerminal().isConnected());
        assertFalse(network.getGenerator("FFR3AA1 _generator").getTerminal().isConnected());

        // Check loads was disconnected
        assertFalse(network.getLoad("FFR4AA1 _load").getTerminal().isConnected());
        assertFalse(network.getLoad("DDE1AA1 _load").getTerminal().isConnected());
        assertFalse(network.getLoad("BBE2AA1 _load").getTerminal().isConnected());
        assertFalse(network.getLoad("FFR3AA1 _load").getTerminal().isConnected());

        // Check hvdc line 1 creation
        checkHvdcLine1Creation(network);

        // Check hvdc line 2 creation
        checkHvdcLine2Creation(network);

        TestUtils.assertNetworksAreEqual(network, "/hvdc/TestCase16Nodes_2HVDCs.xiidm", getClass());
    }

    private static void checkHvdcLine1Creation(final Network network) {
        assertEquals(2, network.getHvdcLineCount());
        HvdcLine hvdcLine1 = network.getHvdcLine("HVDC_FR4-DE1");
        assertEquals(1000.0, hvdcLine1.getMaxP());
        assertEquals(0.5, hvdcLine1.getR());
        assertEquals(400, hvdcLine1.getNominalV());
        assertEquals(HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER, hvdcLine1.getConvertersMode());
        assertEquals(0, hvdcLine1.getActivePowerSetpoint());
        assertEquals("HVDC_FR4-DE1_VSC1", hvdcLine1.getConverterStation1().getId());
        assertEquals(1.0, hvdcLine1.getConverterStation1().getLossFactor());
        assertEquals("HVDC_FR4-DE1_VSC2", hvdcLine1.getConverterStation2().getId());
        assertEquals(10.0, hvdcLine1.getConverterStation2().getLossFactor());
    }

    private static void checkHvdcLine2Creation(final Network network) {
        HvdcLine hvdcLine2 = network.getHvdcLine("HVDC_BE2-FR3");
        assertEquals(200.0, hvdcLine2.getMaxP());
        assertEquals(0.75, hvdcLine2.getR());
        assertEquals(220, hvdcLine2.getNominalV());
        assertEquals(HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER, hvdcLine2.getConvertersMode());
        assertEquals(0, hvdcLine2.getActivePowerSetpoint());
        assertEquals("HVDC_BE2-FR3_VSC1", hvdcLine2.getConverterStation1().getId());
        assertEquals(2.0, hvdcLine2.getConverterStation1().getLossFactor());
        assertEquals("HVDC_BE2-FR3_VSC2", hvdcLine2.getConverterStation2().getId());
        assertEquals(15.0, hvdcLine2.getConverterStation2().getLossFactor());
    }

    @Test
    void testHvdcCreationRoundTripInverted() {
        // Test that when we create the HVDC then remove it, the network is the same
        // in this case the power flow on HVDC_FR4-DE1 is inverted
        Network network = Network.read("hvdc/TestCase16Nodes_inverted.xiidm", getClass().getResourceAsStream("/hvdc/TestCase16Nodes_inverted.xiidm"));
        SwePreprocessorParameters params = JsonSwePreprocessorImporter.read(getClass().getResourceAsStream("/hvdc/SwePreprocessorParameters_16nodes.json"));
        HvdcLinkProcessor.replaceEquivalentModelByHvdc(network, params.getHvdcCreationParametersSet());
        HvdcLinkProcessor.replaceHvdcByEquivalentModel(network, params.getHvdcCreationParametersSet(), null);
        TestUtils.assertNetworksAreEqual(network, "/hvdc/TestCase16Nodes_inverted.xiidm", getClass());
    }

    @Test
    void testHvdcCreationInverted() {
        // Inspect the contents of the created HVDC lines
        // in this case the power flow on HVDC_FR4-DE1 is inverted
        Network network = Network.read("hvdc/TestCase16Nodes_inverted.xiidm", getClass().getResourceAsStream("/hvdc/TestCase16Nodes_inverted.xiidm"));
        SwePreprocessorParameters params = JsonSwePreprocessorImporter.read(getClass().getResourceAsStream("/hvdc/SwePreprocessorParameters_16nodes.json"));
        HvdcLinkProcessor.replaceEquivalentModelByHvdc(network, params.getHvdcCreationParametersSet());
        TestUtils.assertNetworksAreEqual(network, "/hvdc/TestCase16Nodes_2HVDCs_inverted.xiidm", getClass());
    }

    @Test
    void testHvdcCreationAndSetpointModification() {
        Network network = Network.read("hvdc/TestCase16Nodes.xiidm", getClass().getResourceAsStream("/hvdc/TestCase16Nodes.xiidm"));
        SwePreprocessorParameters params = JsonSwePreprocessorImporter.read(getClass().getResourceAsStream("/hvdc/SwePreprocessorParameters_16nodes.json"));
        HvdcLinkProcessor.replaceEquivalentModelByHvdc(network, params.getHvdcCreationParametersSet());
        // Modify the state of the HVDC lines and check that this state is impacted on equivalent AC model
        HvdcLine hvdcLine1 = network.getHvdcLine("HVDC_FR4-DE1");
        hvdcLine1.setActivePowerSetpoint(200.);
        hvdcLine1.getExtension(HvdcAngleDroopActivePowerControl.class).setEnabled(true);
        HvdcLine hvdcLine2 = network.getHvdcLine("HVDC_BE2-FR3");
        hvdcLine2.setActivePowerSetpoint(200.);
        hvdcLine2.setConvertersMode(HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER);
        hvdcLine2.getExtension(HvdcAngleDroopActivePowerControl.class).setEnabled(false);
        HvdcLinkProcessor.replaceHvdcByEquivalentModel(network, params.getHvdcCreationParametersSet(), null);
        TestUtils.assertNetworksAreEqual(network, "/hvdc/TestCase16Nodes_afterModif.xiidm", getClass());
    }

    @Test
    void testHvdcCreationWithoutAngleDroopParameter() {
        Network network = Network.read("hvdc/TestCase16Nodes.xiidm", getClass().getResourceAsStream("/hvdc/TestCase16Nodes.xiidm"));
        SwePreprocessorParameters params = JsonSwePreprocessorImporter.read(getClass().getResourceAsStream("/hvdc/SwePreprocessorParameters_no_angle_droop.json"));
        HvdcLinkProcessor.replaceEquivalentModelByHvdc(network, params.getHvdcCreationParametersSet());
        // Modify the state of the HVDC lines and check that activePowerPoint has not changed => it should remain at 0 in xiidm
        HvdcLine hvdcLine1 = network.getHvdcLine("HVDC_FR4-DE1");
        hvdcLine1.setActivePowerSetpoint(200.);
        HvdcLine hvdcLine2 = network.getHvdcLine("HVDC_BE2-FR3");
        hvdcLine2.setActivePowerSetpoint(200.);
        hvdcLine2.setConvertersMode(HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER);
        HvdcLinkProcessor.replaceHvdcByEquivalentModel(network, params.getHvdcCreationParametersSet(), null);
        TestUtils.assertNetworksAreEqual(network, "/hvdc/TestCase16Nodes_no_angle_droop_afterModif.xiidm", getClass());
    }

    @Test
    void testDisconnectedAcLine() {
        Network network = Network.read("hvdc/TestCase16Nodes.xiidm", getClass().getResourceAsStream("/hvdc/TestCase16Nodes.xiidm"));
        SwePreprocessorParameters params = JsonSwePreprocessorImporter.read(getClass().getResourceAsStream("/hvdc/SwePreprocessorParameters_16nodes.json"));
        network.getLine("FFR4AA1  DDE1AA1  1").getTerminal1().disconnect();
        HvdcLinkProcessor.replaceEquivalentModelByHvdc(network, params.getHvdcCreationParametersSet());
        assertFalse(network.getHvdcLine("HVDC_FR4-DE1").getConverterStation(TwoSides.ONE).getTerminal().isConnected());
        assertTrue(network.getHvdcLine("HVDC_FR4-DE1").getConverterStation(TwoSides.TWO).getTerminal().isConnected());
    }

    @Test
    void testDisconnectedAcLine2() {
        Network network = Network.read("hvdc/TestCase16Nodes.xiidm", getClass().getResourceAsStream("/hvdc/TestCase16Nodes.xiidm"));
        SwePreprocessorParameters params = JsonSwePreprocessorImporter.read(getClass().getResourceAsStream("/hvdc/SwePreprocessorParameters_16nodes.json"));
        HvdcLinkProcessor.replaceEquivalentModelByHvdc(network, params.getHvdcCreationParametersSet());
        network.getHvdcLine("HVDC_FR4-DE1").getConverterStation(TwoSides.TWO).getTerminal().disconnect();
        HvdcLinkProcessor.replaceHvdcByEquivalentModel(network, params.getHvdcCreationParametersSet(), null);
        assertTrue(network.getLine("FFR4AA1  DDE1AA1  1").getTerminal1().isConnected());
        assertFalse(network.getLine("FFR4AA1  DDE1AA1  1").getTerminal2().isConnected());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/hvdc/TestCase16Nodes_noload.xiidm", "/hvdc/TestCase16Nodes_nogenerator.xiidm", "/hvdc/TestCase16Nodes_noline.xiidm"})
    void testNoinputFails(String xiidm) {
        Network network = Network.read("hvdc/TestCase16Nodes.xiidm", getClass().getResourceAsStream(xiidm));
        Set<HvdcCreationParameters> params = JsonSwePreprocessorImporter.read(getClass().getResourceAsStream("/hvdc/SwePreprocessorParameters_16nodes.json")).getHvdcCreationParametersSet();
        assertThrows(SweInvalidDataNoDetailsException.class, () -> HvdcLinkProcessor.replaceEquivalentModelByHvdc(network, params));
    }

    @Test
    void testCopyInformationFromInitialNetwork() {
        final Load load1 = Mockito.mock(Load.class);
        final Load load2 = Mockito.mock(Load.class);
        final Generator generator1 = Mockito.mock(Generator.class);
        final Generator generator2 = Mockito.mock(Generator.class);
        final Line line = Mockito.mock(Line.class);
        final Terminal terminal1 = Mockito.mock(Terminal.class);
        final Terminal terminal2 = Mockito.mock(Terminal.class);
        final Optional<Load> optLoad1 = Optional.of(load1);
        final Optional<Load> optLoad2 = Optional.of(load2);
        final Optional<Generator> optGenerator1 = Optional.of(generator1);
        final Optional<Generator> optGenerator2 = Optional.of(generator2);
        final Optional<Line> optLine = Optional.of(line);
        Mockito.when(line.getTerminal1()).thenReturn(terminal1);
        Mockito.when(line.getTerminal2()).thenReturn(terminal2);
        final HvdcInformation hvdcInformation = new HvdcInformation("testId");
        hvdcInformation.setSide1LoadConnected(true);
        hvdcInformation.setSide2LoadConnected(true);
        hvdcInformation.setSide1GeneratorConnected(true);
        hvdcInformation.setSide2GeneratorConnected(true);
        hvdcInformation.setAcLineTerminal1Connected(true);
        hvdcInformation.setAcLineTerminal2Connected(true);
        final double load1P = 42.;
        final double load2P = 27.;
        final double generator1P = 12.;
        final double generator2P = 314.;
        hvdcInformation.setSide1LoadP(load1P);
        hvdcInformation.setSide2LoadP(load2P);
        hvdcInformation.setSide1GeneratorTargetP(generator1P);
        hvdcInformation.setSide2GeneratorTargetP(generator2P);

        HvdcLinkProcessor.copyInformationFromInitialNetwork(optLoad1, optGenerator1, optLoad2, optGenerator2, optLine, hvdcInformation);

        Mockito.verify(load1, Mockito.times(1)).setP0(load1P);
        Mockito.verify(load1, Mockito.times(1)).connect();
        Mockito.verify(load2, Mockito.times(1)).setP0(load2P);
        Mockito.verify(load2, Mockito.times(1)).connect();
        Mockito.verify(generator1, Mockito.times(1)).setTargetP(generator1P);
        Mockito.verify(generator1, Mockito.times(1)).connect();
        Mockito.verify(generator2, Mockito.times(1)).setTargetP(generator2P);
        Mockito.verify(generator2, Mockito.times(1)).connect();
        Mockito.verify(terminal1, Mockito.times(1)).connect();
        Mockito.verify(terminal2, Mockito.times(1)).connect();
    }
}

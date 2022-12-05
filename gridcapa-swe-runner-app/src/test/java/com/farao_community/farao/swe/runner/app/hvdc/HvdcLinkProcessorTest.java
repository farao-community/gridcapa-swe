/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.swe.runner.app.hvdc;

import com.farao_community.farao.swe.runner.app.hvdc.parameters.SwePreprocessorParameters;
import com.farao_community.farao.swe.runner.app.hvdc.parameters.json.JsonSwePreprocessorImporter;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
        HvdcLinkProcessor.replaceHvdcByEquivalentModel(network, params.getHvdcCreationParametersSet());
        TestUtils.assertNetworksAreEqual(network, "/hvdc/TestCase16Nodes.xiidm", getClass());
    }

    @Test
    void testHvdcCreation() {
        // Inspect the contents of the created HVDC lines
        Network network = Network.read("hvdc/TestCase16Nodes.xiidm", getClass().getResourceAsStream("/hvdc/TestCase16Nodes.xiidm"));
        SwePreprocessorParameters params = JsonSwePreprocessorImporter.read(getClass().getResourceAsStream("/hvdc/SwePreprocessorParameters_16nodes.json"));
        HvdcLinkProcessor.replaceEquivalentModelByHvdc(network, params.getHvdcCreationParametersSet());
        TestUtils.assertNetworksAreEqual(network, "/hvdc/TestCase16Nodes_2HVDCs.xiidm", getClass());
    }

    @Test
    void testHvdcCreationRoundTripInverted() {
        // Test that when we create the HVDC then remove it, the network is the same
        // in this case the power flow on HVDC_FR4-DE1 is inverted
        Network network = Network.read("hvdc/TestCase16Nodes_inverted.xiidm", getClass().getResourceAsStream("/hvdc/TestCase16Nodes_inverted.xiidm"));
        SwePreprocessorParameters params = JsonSwePreprocessorImporter.read(getClass().getResourceAsStream("/hvdc/SwePreprocessorParameters_16nodes.json"));
        HvdcLinkProcessor.replaceEquivalentModelByHvdc(network, params.getHvdcCreationParametersSet());
        HvdcLinkProcessor.replaceHvdcByEquivalentModel(network, params.getHvdcCreationParametersSet());
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
        HvdcLinkProcessor.replaceHvdcByEquivalentModel(network, params.getHvdcCreationParametersSet());
        TestUtils.assertNetworksAreEqual(network, "/hvdc/TestCase16Nodes_afterModif.xiidm", getClass());
    }

    @Test
    void testDisconnectedAcLine() {
        Network network = Network.read("hvdc/TestCase16Nodes.xiidm", getClass().getResourceAsStream("/hvdc/TestCase16Nodes.xiidm"));
        SwePreprocessorParameters params = JsonSwePreprocessorImporter.read(getClass().getResourceAsStream("/hvdc/SwePreprocessorParameters_16nodes.json"));
        network.getLine("FFR4AA1  DDE1AA1  1").getTerminal1().disconnect();
        HvdcLinkProcessor.replaceEquivalentModelByHvdc(network, params.getHvdcCreationParametersSet());
        assertFalse(network.getHvdcLine("HVDC_FR4-DE1").getConverterStation(HvdcLine.Side.ONE).getTerminal().isConnected());
        assertTrue(network.getHvdcLine("HVDC_FR4-DE1").getConverterStation(HvdcLine.Side.TWO).getTerminal().isConnected());
    }

    @Test
    void testDisconnectedAcLine2() {
        Network network = Network.read("hvdc/TestCase16Nodes.xiidm", getClass().getResourceAsStream("/hvdc/TestCase16Nodes.xiidm"));
        SwePreprocessorParameters params = JsonSwePreprocessorImporter.read(getClass().getResourceAsStream("/hvdc/SwePreprocessorParameters_16nodes.json"));
        HvdcLinkProcessor.replaceEquivalentModelByHvdc(network, params.getHvdcCreationParametersSet());
        network.getHvdcLine("HVDC_FR4-DE1").getConverterStation(HvdcLine.Side.TWO).getTerminal().disconnect();
        HvdcLinkProcessor.replaceHvdcByEquivalentModel(network, params.getHvdcCreationParametersSet());
        assertTrue(network.getLine("FFR4AA1  DDE1AA1  1").getTerminal1().isConnected());
        assertFalse(network.getLine("FFR4AA1  DDE1AA1  1").getTerminal2().isConnected());
    }
}

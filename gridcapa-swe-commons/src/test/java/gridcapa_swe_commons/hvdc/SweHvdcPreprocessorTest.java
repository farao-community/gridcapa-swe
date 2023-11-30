/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package gridcapa_swe_commons.hvdc;

import com.farao_community.farao.gridcapa_swe_commons.hvdc.SweHvdcPreprocessor;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
class SweHvdcPreprocessorTest {

    @Test
    void addHvdcTest() {
        SweHvdcPreprocessor swePreprocessor = new SweHvdcPreprocessor();
        InputStream parameters = getClass().getResourceAsStream("/hvdc/SwePreprocessorParameters.json");
        Network network = Network.read("hvdc/TestCase16Nodes.xiidm", getClass().getResourceAsStream("/hvdc/TestCase16Nodes.xiidm"));
        swePreprocessor.applyParametersToNetwork(parameters, network);
        TestUtils.assertNetworksAreEqual(network, "/hvdc/TestCase16Nodes_2HVDCs.xiidm", getClass());
    }
}

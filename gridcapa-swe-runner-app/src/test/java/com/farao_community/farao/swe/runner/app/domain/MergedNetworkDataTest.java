/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.domain;

import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@SpringBootTest
class MergedNetworkDataTest {

    @Mock
    private Network network;

    private Map<String, String> networkIdsByCountry = new HashMap<>();

    @Test
    void simpleTest() {
        MergedNetworkData mergdNetworkData = new MergedNetworkData(network, networkIdsByCountry);
        assertEquals(this.network, mergdNetworkData.getMergedNetwork());
        assertEquals(this.networkIdsByCountry, mergdNetworkData.getSubnetworkIdByCountry());
    }
}

/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy.shift;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@SpringBootTest
class CountryBalanceComputationTest {

    @Test
    void testCountryGeneratorsScalableforFR() {
        Network network = Importers.loadNetwork("hvdc/TestCase16Nodes.xiidm", getClass().getResourceAsStream("/hvdc/TestCase16Nodes.xiidm"));
        Map<String, Double> countriesBalances = CountryBalanceComputation.computeSweCountriesBalances(network);
        assertEquals(3, countriesBalances.size());

    }
}
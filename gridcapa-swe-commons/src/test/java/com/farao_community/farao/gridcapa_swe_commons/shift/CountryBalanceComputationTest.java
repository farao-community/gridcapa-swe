/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_swe_commons.shift;

import com.farao_community.farao.commons.EICode;
import com.powsybl.cgmes.extensions.CgmesControlArea;
import com.powsybl.cgmes.extensions.CgmesControlAreas;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@SpringBootTest(classes = CountryBalanceComputation.class)
class CountryBalanceComputationTest {

    @Autowired
    CountryBalanceComputation countryBalanceComputation;

    @MockBean
    Logger businessLogger;

    @Test
    void testCountryGeneratorsScalableforFR() {
        Network network = Network.read("hvdc/TestCase16Nodes.xiidm", getClass().getResourceAsStream("/hvdc/TestCase16Nodes.xiidm"));
        Map<String, Double> countriesBalances = countryBalanceComputation.computeSweCountriesBalances(network);
        assertEquals(3, countriesBalances.size());
    }

    @Test
    void testCountryGeneratorsScalable() {
        Network network = Network.read("hvdc/TestCase16Nodes_zerogenerator.xiidm", getClass().getResourceAsStream("/hvdc/TestCase16Nodes_zerogenerator.xiidm"));
        Collection<CgmesControlArea> collectionAreas = new ArrayList<>();
        CgmesControlArea frArea = Mockito.mock(CgmesControlArea.class);
        Mockito.when(frArea.getNetInterchange()).thenReturn(333.33);
        Mockito.when(frArea.getEnergyIdentificationCodeEIC()).thenReturn(new EICode(Country.valueOf("FR")).getAreaCode());
        CgmesControlArea esArea = Mockito.mock(CgmesControlArea.class);
        Mockito.when(frArea.getNetInterchange()).thenReturn(444.44);
        Mockito.when(frArea.getEnergyIdentificationCodeEIC()).thenReturn(new EICode(Country.valueOf("ES")).getAreaCode());
        CgmesControlArea ptArea = Mockito.mock(CgmesControlArea.class);
        Mockito.when(frArea.getNetInterchange()).thenReturn(555.55);
        Mockito.when(frArea.getEnergyIdentificationCodeEIC()).thenReturn(new EICode(Country.valueOf("PT")).getAreaCode());
        collectionAreas.add(frArea);
        collectionAreas.add(esArea);
        collectionAreas.add(ptArea);
        CgmesControlAreas areas = Mockito.mock(CgmesControlAreas.class);
        Mockito.when(areas.getCgmesControlAreas()).thenReturn(collectionAreas);
        network.addExtension(CgmesControlAreas.class, areas);
        Map<String, Double> countriesBalances = countryBalanceComputation.computeSweCountriesBalances(network);
        assertEquals(3, countriesBalances.size());

    }
}

/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.powsybl.commons.datasource.DataSource;
import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.iidm.network.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@SpringBootTest
class RemoveRemoteVoltageRegulationInFranceServiceTest {

    @Autowired
    private RemoveRemoteVoltageRegulationInFranceService removeRemoteVoltageRegulationInFranceService;

    @Test
    void testRemoteVoltageRegulationRemoval() {
        Network network = generateNetwork();
        assertEquals(410, network.getGenerator("GeneratorFR").getTargetV(), 1e-3);
        assertEquals(network.getLoad("LoadFR").getTerminal(), network.getGenerator("GeneratorFR").getRegulatingTerminal());
        assertEquals(410, network.getGenerator("GeneratorES").getTargetV(), 1e-3);
        assertEquals(network.getLoad("LoadES").getTerminal(), network.getGenerator("GeneratorES").getRegulatingTerminal());

        Map<String, RemoveRemoteVoltageRegulationInFranceService.ReplacedVoltageRegulation> oldValues = removeRemoteVoltageRegulationInFranceService.removeRemoteVoltageRegulationInFrance(network);

        assertEquals(15.375, network.getGenerator("GeneratorFR").getTargetV(), 1e-3);
        assertEquals(network.getGenerator("GeneratorFR").getTerminal(), network.getGenerator("GeneratorFR").getRegulatingTerminal());
        assertEquals(410, network.getGenerator("GeneratorES").getTargetV(), 1e-3);
        assertEquals(network.getLoad("LoadES").getTerminal(), network.getGenerator("GeneratorES").getRegulatingTerminal());

        DataSource dataSource = new MemDataSource();
        network.write("XIIDM", new Properties(), dataSource);
        Network networkCopy = Network.read(dataSource);

        removeRemoteVoltageRegulationInFranceService.resetRemoteVoltageRegulationInFrance(networkCopy, oldValues);

        assertEquals(410, networkCopy.getGenerator("GeneratorFR").getTargetV(), 1e-3);
        assertEquals(networkCopy.getLoad("LoadFR").getTerminal(), networkCopy.getGenerator("GeneratorFR").getRegulatingTerminal());
        assertEquals(410, networkCopy.getGenerator("GeneratorES").getTargetV(), 1e-3);
        assertEquals(networkCopy.getLoad("LoadES").getTerminal(), networkCopy.getGenerator("GeneratorES").getRegulatingTerminal());
    }

    private static Network generateNetwork() {
        Network network = Network.create("Network", "code");
        createSubstationInCountry(network, Country.FR);
        createSubstationInCountry(network, Country.ES);
        network.newLine()
                .setId("Line")
                .setR(0.1)
                .setX(0.1)
                .setBus1("BusFR400")
                .setBus2("BusES400")
                .add();
        return network;
    }

    private static void createSubstationInCountry(Network network, Country country) {
        Substation substation = network.newSubstation()
                .setId(replaceCountry("Substation%s", country))
                .setCountry(country)
                .add();
        VoltageLevel voltageLevel15 = substation.newVoltageLevel()
                .setId(replaceCountry("VoltageLevel%s15", country))
                .setNominalV(15)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .setLowVoltageLimit(14)
                .setHighVoltageLimit(16)
                .add();
        voltageLevel15.getBusBreakerView().newBus()
                .setId(replaceCountry("Bus%s15", country))
                .add();
        VoltageLevel voltageLevel400 = substation.newVoltageLevel()
                .setId(replaceCountry("VoltageLevel%s400", country))
                .setNominalV(400)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .setLowVoltageLimit(380)
                .setHighVoltageLimit(420)
                .add();
        voltageLevel400.getBusBreakerView().newBus()
                .setId(replaceCountry("Bus%s400", country))
                .add();
        Load load = voltageLevel400.newLoad()
                .setId(replaceCountry("Load%s", country))
                .setP0(1000)
                .setQ0(0)
                .setBus(replaceCountry("Bus%s400", country))
                .add();
        voltageLevel15.newGenerator()
                .setId(replaceCountry("Generator%s", country))
                .setTargetP(1000)
                .setMinP(0)
                .setMaxP(2000)
                .setTargetV(410)
                .setRegulatingTerminal(load.getTerminal())
                .setVoltageRegulatorOn(true)
                .setBus(replaceCountry("Bus%s15", country))
                .add();
        substation.newTwoWindingsTransformer()
                .setId(replaceCountry("TwoWindingsTransformer%s", country))
                .setR(0.1)
                .setX(0.1)
                .setRatedU1(15)
                .setRatedU2(400)
                .setBus1(replaceCountry("Bus%s15", country))
                .setBus2(replaceCountry("Bus%s400", country))
                .add();
    }

    private static String replaceCountry(String formattedString, Country country) {
        return String.format(formattedString, country);
    }
}

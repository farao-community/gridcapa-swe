/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.openloadflow.OpenLoadFlowParameters;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@SpringBootTest
class FixRemoteVoltageTargetServiceTest {

    @Autowired
    private FixRemoteVoltageTargetService fixRemoteVoltageTargetService;

    @Test
    void testRemoteVoltageTargetFix() {
        Network network = generateNetwork(390, 410);
        LoadFlowParameters parameters = new LoadFlowParameters();
        parameters.addExtension(OpenLoadFlowParameters.class, new OpenLoadFlowParameters());
        ReflectionTestUtils.setField(fixRemoteVoltageTargetService, "loadFlowRunner", buildMockRunner());

        LoadFlow.run(network, parameters);
        assertEquals(390, network.getGenerator("Generator151").getTargetV(), 1e-3);
        assertEquals(410, network.getGenerator("Generator152").getTargetV(), 1e-3);

        fixRemoteVoltageTargetService.fixUnrealisticRemoteTargetVoltages(network, parameters);
        assertEquals(390, network.getGenerator("Generator151").getTargetV(), 1e-3);
        assertEquals(400, network.getGenerator("Generator152").getTargetV(), 1e-3);
    }

    @Test
    void testRemoteVoltageTargetFixUnsufficientShift() {
        Network network = generateNetwork(396, 405);
        LoadFlowParameters parameters = new LoadFlowParameters();
        parameters.addExtension(OpenLoadFlowParameters.class, new OpenLoadFlowParameters());
        ReflectionTestUtils.setField(fixRemoteVoltageTargetService, "loadFlowRunner", buildMockRunner());

        LoadFlow.run(network, parameters);
        assertEquals(396, network.getGenerator("Generator151").getTargetV(), 1e-3);
        assertEquals(405, network.getGenerator("Generator152").getTargetV(), 1e-3);

        fixRemoteVoltageTargetService.fixUnrealisticRemoteTargetVoltages(network, parameters);
        assertEquals(396, network.getGenerator("Generator151").getTargetV(), 1e-3);
        assertEquals(405, network.getGenerator("Generator152").getTargetV(), 1e-3);
    }

    @Test
    void testRemoteVoltageTargetFixButInitiallyConvergent() {
        Network network = generateNetwork(390, 410);
        LoadFlowParameters parameters = new LoadFlowParameters();
        parameters.addExtension(OpenLoadFlowParameters.class, new OpenLoadFlowParameters());

        LoadFlow.run(network, parameters);
        assertEquals(390, network.getGenerator("Generator151").getTargetV(), 1e-3);
        assertEquals(410, network.getGenerator("Generator152").getTargetV(), 1e-3);

        fixRemoteVoltageTargetService.fixUnrealisticRemoteTargetVoltages(network, parameters);
        assertEquals(390, network.getGenerator("Generator151").getTargetV(), 1e-3);
        assertEquals(410, network.getGenerator("Generator152").getTargetV(), 1e-3);
    }

    private LoadFlow.Runner buildMockRunner() {
        LoadFlow.Runner runner = Mockito.mock(LoadFlow.Runner.class);
        Mockito.when(runner.run(Mockito.any(), Mockito.any()))
                .thenReturn(new LoadFlowResult() {
                    @Override
                    public boolean isOk() {
                        return false;
                    }

                    @Override
                    public Status getStatus() {
                        return Status.FAILED;
                    }

                    @Override
                    public Map<String, String> getMetrics() {
                        return Map.of();
                    }

                    @Override
                    public String getLogs() {
                        return "";
                    }
                })
                .thenCallRealMethod()
                .getMock();
        return runner;
    }

    private static Network generateNetwork(double target1, double target2) {
        Network network = Network.create("Network", "code");
        Substation substation = network.newSubstation()
                .setId("Substation")
                .setCountry(Country.FR)
                .add();
        VoltageLevel voltageLevel151 = substation.newVoltageLevel()
                .setId("VoltageLevel151")
                .setNominalV(15)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .setLowVoltageLimit(14)
                .setHighVoltageLimit(16)
                .add();
        voltageLevel151.getBusBreakerView().newBus()
                .setId("151Bus")
                .add();
        VoltageLevel voltageLevel152 = substation.newVoltageLevel()
                .setId("VoltageLevel152")
                .setNominalV(15)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .setLowVoltageLimit(14)
                .setHighVoltageLimit(16)
                .add();
        voltageLevel152.getBusBreakerView().newBus()
                .setId("152Bus")
                .add();
        VoltageLevel voltageLevel400 = substation.newVoltageLevel()
                .setId("VoltageLevel400")
                .setNominalV(400)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .setLowVoltageLimit(380)
                .setHighVoltageLimit(420)
                .add();
        voltageLevel400.getBusBreakerView().newBus()
                .setId("400Bus")
                .add();
        Load load = voltageLevel400.newLoad()
                .setId("Load")
                .setP0(1000)
                .setQ0(0)
                .setBus("400Bus")
                .add();
        Generator generator151 = voltageLevel151.newGenerator()
                .setId("Generator151")
                .setTargetP(500)
                .setMinP(0)
                .setMaxP(2000)
                .setTargetV(target1)
                .setRegulatingTerminal(load.getTerminal())
                .setVoltageRegulatorOn(true)
                .setBus("151Bus")
                .add();
        generator151.newMinMaxReactiveLimits()
                .setMinQ(-100)
                .setMaxQ(100)
                .add();
        Generator generator152 = voltageLevel152.newGenerator()
                .setId("Generator152")
                .setTargetP(500)
                .setMinP(0)
                .setMaxP(2000)
                .setTargetV(target2)
                .setRegulatingTerminal(load.getTerminal())
                .setVoltageRegulatorOn(true)
                .setBus("152Bus")
                .add();
        generator152.newMinMaxReactiveLimits()
                .setMinQ(-100)
                .setMaxQ(100)
                .add();
        substation.newTwoWindingsTransformer()
                .setId("TwoWindingsTransformer1")
                .setR(0.1)
                .setX(0.1)
                .setRatedU1(15)
                .setRatedU2(400)
                .setBus1("151Bus")
                .setBus2("400Bus")
                .add();
        substation.newTwoWindingsTransformer()
                .setId("TwoWindingsTransformer2")
                .setR(0.1)
                .setX(0.1)
                .setRatedU1(15)
                .setRatedU2(400)
                .setBus1("152Bus")
                .setBus2("400Bus")
                .add();
        return network;
    }
}

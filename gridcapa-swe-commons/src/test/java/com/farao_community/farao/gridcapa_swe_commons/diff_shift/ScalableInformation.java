/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_swe_commons.diff_shift;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import org.jgrapht.alg.util.Pair;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public class ScalableInformation {
    private String id;
    private int position;
    private double pMin;
    private double pMax;
    private  FlowDirectionType flowDirectionType;

    public ScalableInformation(String id, int position, double pMin, double pMax, FlowDirectionType flowDirectionType) {
        this.id = id;
        this.position = position;
        this.pMin = pMin;
        this.pMax = pMax;
        this.flowDirectionType = flowDirectionType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public double getpMin() {
        return pMin;
    }

    public void setpMin(double pMin) {
        this.pMin = pMin;
    }

    public double getpMax() {
        return pMax;
    }

    public void setpMax(double pMax) {
        this.pMax = pMax;
    }

    public FlowDirectionType getFlowDirectionType() {
        return flowDirectionType;
    }

    public void setFlowDirectionType(FlowDirectionType flowDirectionType) {
        this.flowDirectionType = flowDirectionType;
    }

    @Override
    public String toString() {
        return flowDirectionType.toString() + ";" + position + ";" + id + ";" + pMin + ";" + pMax;
    }

    public GeneratorInformation getGeneratorInformation(Network network) {
        Generator generator = network.getGenerator(id);
        boolean isInMainConnectedComponent = getBus(generator.getTerminal()).isInMainConnectedComponent();

        Map<String, Pair<Boolean, Boolean>> twoWindingsTransformersInfo = new HashMap<>();
        Bus genBus = getBus(generator.getTerminal());
        generator.getTerminal().getVoltageLevel().getTwoWindingsTransformers().forEach(twt -> {
            if (genBus.equals(getBus(twt.getTerminal1())) || genBus.equals(getBus(twt.getTerminal2()))) {
                twoWindingsTransformersInfo.put(twt.getId(), Pair.of(twt.getTerminal1().isConnected(), twt.getTerminal2().isConnected()));
            }
        });
        return new GeneratorInformation(generator.getId(), generator.getMinP(), generator.getMaxP(), generator.getTargetP(), generator.getTerminal().isConnected(), isInMainConnectedComponent, twoWindingsTransformersInfo);
    }

    private static Bus getBus(Terminal terminal) {
        return terminal.getBusBreakerView().getConnectableBus();
    }

    public enum FlowDirectionType {
        UP,
        DOWN
    }
}

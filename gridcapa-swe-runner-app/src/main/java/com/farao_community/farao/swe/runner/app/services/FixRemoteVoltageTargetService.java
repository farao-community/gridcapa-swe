/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.swe.runner.app.configurations.DataFixConfiguration;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openloadflow.OpenLoadFlowParameters;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@Service
public class FixRemoteVoltageTargetService {
    public static final int ACCEPTABLE_VOLTAGE_SHIFT_LIMIT = 10;

    private final DataFixConfiguration dataFixConfiguration;
    private final Logger businessLogger;
    private final LoadFlow.Runner loadFlowRunner;

    public FixRemoteVoltageTargetService(DataFixConfiguration dataFixConfiguration, Logger businessLogger) {
        this.dataFixConfiguration = dataFixConfiguration;
        this.businessLogger = businessLogger;
        this.loadFlowRunner = LoadFlow.find();
    }

    public void fixUnrealisticRemoteTargetVoltages(Network network, LoadFlowParameters loadFlowParameters) {
        if (!dataFixConfiguration.fixUnrealisticRemoteVoltageTarget()) {
            return;
        }
        OpenLoadFlowParameters openLoadFlowParameters = loadFlowParameters.getExtension(OpenLoadFlowParameters.class);
        if (openLoadFlowParameters == null) {
            return;
        }
        if (!loadFlowRunner.run(network, loadFlowParameters).isFullyConverged()) {
            businessLogger.info("Basecase unsecure, trying to fix it by improving remote target voltages.");
            openLoadFlowParameters.setVoltageRemoteControl(false);
            loadFlowRunner.run(network, loadFlowParameters);
            network.getGeneratorStream()
                    .filter(this::isRemoteVoltageRegulationOn)
                    .filter(this::isInFrance)
                    .filter(this::remoteVoltageTargetSeemsUnrealistic)
                    .forEach(this::fixRemoteVoltageTarget);
        }
    }

    private void fixRemoteVoltageTarget(Generator generator) {
        double oldVoltageTarget = generator.getTargetV();
        double regulatingBusVoltage = generator.getRegulatingTerminal().getBusView().getBus().getV();
        double newVoltageTarget = Math.round(oldVoltageTarget + (regulatingBusVoltage - oldVoltageTarget) / 2);
        generator.setTargetV(newVoltageTarget);
        businessLogger.info(String.format("Shifting generator '%s' target voltage from %fkV to %fkV", generator.getId(), oldVoltageTarget, newVoltageTarget));
    }

    private boolean remoteVoltageTargetSeemsUnrealistic(Generator generator) {
        if (generator.getTerminal().getBusView().getBus() == null || generator.getRegulatingTerminal().getBusView().getBus() == null) {
            return false;
        }
        double remoteTerminalVoltage = generator.getRegulatingTerminal().getBusView().getBus().getV();
        double remoteTargetVoltage = generator.getTargetV();
        return Math.abs(remoteTerminalVoltage - remoteTargetVoltage) > ACCEPTABLE_VOLTAGE_SHIFT_LIMIT;
    }

    private boolean isInFrance(Generator generator) {
        Optional<Substation> substation = generator.getTerminal().getVoltageLevel().getSubstation();
        if (substation.isEmpty()) {
            return false;
        }
        Optional<Country> country = substation.get().getCountry();
        if (country.isEmpty()) {
            return false;
        }
        return country.get() == Country.FR;
    }

    private boolean isRemoteVoltageRegulationOn(Generator generator) {
        return generator.isVoltageRegulatorOn() && generator.getTerminal() != generator.getRegulatingTerminal();
    }
}

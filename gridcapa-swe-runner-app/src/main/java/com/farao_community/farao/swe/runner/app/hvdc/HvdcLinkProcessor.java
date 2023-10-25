/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.swe.runner.app.hvdc;

import com.farao_community.farao.swe.runner.api.exception.SweInvalidDataException;
import com.farao_community.farao.swe.runner.app.hvdc.parameters.HvdcCreationParameters;
import com.farao_community.farao.swe.runner.app.hvdc.parameters.VscStationCreationParameters;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControl;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControlAdder;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Replaces an HVDC line's equivalent AC model with an actual HVDC line, and vice-versa
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class HvdcLinkProcessor {
    private HvdcLinkProcessor() {
        // should not be instantiated
    }

    /* ================================ */
    /* AC equivalent model to HVDC line */
    /* ================================ */

    public static void replaceEquivalentModelByHvdc(Network network, Set<HvdcCreationParameters> creationParametersSet) {
        // Sort HvdcCreationParameters to ensure repeatability
        List<HvdcCreationParameters> sortedHvdcCreationParameters = creationParametersSet.stream()
                .sorted(Comparator.comparing(HvdcCreationParameters::getId)).toList();
        for (HvdcCreationParameters parameter : sortedHvdcCreationParameters) {
            replaceEquivalentModelByHvdc(network, parameter);
        }
    }

    private static void replaceEquivalentModelByHvdc(Network network, HvdcCreationParameters creationParameters) {
        // Disconnect equivalent generators & loads on both sides
        disconnectGeneratorAndLoad(network, creationParameters, HvdcLine.Side.ONE);
        disconnectGeneratorAndLoad(network, creationParameters, HvdcLine.Side.TWO);

        // Create one VSC converter station on each side
        createVscStation(network, creationParameters, HvdcLine.Side.ONE);
        createVscStation(network, creationParameters, HvdcLine.Side.TWO);

        // Create the HVDC line
        createHvdcLine(network, creationParameters);
    }

    private static void disconnectGeneratorAndLoad(Network network, HvdcCreationParameters creationParameters, HvdcLine.Side side) {
        getGeneratorOrThrow(network, creationParameters.getEquivalentGeneratorId(side)).getTerminal().disconnect();
        getLoadOrThrow(network, creationParameters.getEquivalentLoadId(side)).getTerminal().disconnect();
    }

    private static void createVscStation(Network network, HvdcCreationParameters creationParameters, HvdcLine.Side side) {
        Generator equivalentGenerator = getGeneratorOrThrow(network, creationParameters.getEquivalentGeneratorId(side));
        Terminal terminal = equivalentGenerator.getTerminal();
        // WARNING : in CVG, this is done for the equivalent generator of HVDC line 1 for both HVDC lines. Here it is more generic => check if OK
        VscStationCreationParameters vscStationCreationParameters = creationParameters.getVscCreationParameters(side);
        double voltageSetpoint = terminal.isConnected() ? equivalentGenerator.getTargetV() : vscStationCreationParameters.getDefaultVoltageSetpoint();
        VoltageLevel voltageLevel = terminal.getVoltageLevel();
        voltageLevel.newVscConverterStation()
                .setId(vscStationCreationParameters.getId())
                .setReactivePowerSetpoint(vscStationCreationParameters.getReactivePowerSetpoint())
                .setLossFactor(vscStationCreationParameters.getLossFactor())
                .setVoltageRegulatorOn(vscStationCreationParameters.isVoltageRegulatorOn())
                .setVoltageSetpoint(voltageSetpoint)
                .setConnectableBus(terminal.getBusBreakerView().getConnectableBus().getId())
                .setFictitious(false)
                .setEnsureIdUnicity(true)
                .add();
    }

    private static void createHvdcLine(Network network, HvdcCreationParameters creationParameters) {
        HvdcLine hvdcLine = network.newHvdcLine()
                .setMaxP(creationParameters.getMaxP())
                .setR(creationParameters.getR())
                .setNominalV(creationParameters.getNominalV())
                .setId(creationParameters.getId())
                .setEnsureIdUnicity(true)
                .setConverterStationId1(creationParameters.getVscCreationParameters(HvdcLine.Side.ONE).getId())
                .setConverterStationId2(creationParameters.getVscCreationParameters(HvdcLine.Side.TWO).getId())
                .setConvertersMode(HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER)
                .setActivePowerSetpoint(0)
                .add();
        // Check if equivalent line is connected before connecting the new HVDC line
        Line equivalentAcLine = getLineOrThrow(network, creationParameters.getEquivalentAcLineId());
        if (equivalentAcLine.getTerminal1().isConnected()) {
            hvdcLine.getConverterStation1().getTerminal().connect();
            equivalentAcLine.getTerminal1().disconnect();
        }
        if (equivalentAcLine.getTerminal2().isConnected()) {
            hvdcLine.getConverterStation2().getTerminal().connect();
            equivalentAcLine.getTerminal2().disconnect();
        }
        // Create angle droop active power control extension
        if (creationParameters.getAngleDroopActivePowerControlParameters() != null) {
            hvdcLine.newExtension(HvdcAngleDroopActivePowerControlAdder.class)
                    .withP0(creationParameters.getAngleDroopActivePowerControlParameters().getP0())
                    .withDroop(creationParameters.getAngleDroopActivePowerControlParameters().getDroop())
                    .withEnabled(true)
                    .add();
        }
        // WARNING : check if it is also necessary to disconnect the two tie-lines (which are currently not in the HvdcCreationParameters)
    }

    /* ================================ */
    /* HVDC line to AC equivalent model */
    /* ================================ */

    public static void replaceHvdcByEquivalentModel(Network network, Set<HvdcCreationParameters> creationParametersSet) {
        creationParametersSet.forEach(parameter -> replaceHvdcByEquivalentModel(network, parameter));
    }

    private static void replaceHvdcByEquivalentModel(Network network, HvdcCreationParameters creationParameters) {
        HvdcLine hvdcLine = network.getHvdcLine(creationParameters.getId());
        connectEquivalentGeneratorsAndLoads(network, creationParameters, hvdcLine);
        connectEquivalentAcLine(network, creationParameters);

        network.getHvdcLine(creationParameters.getId()).remove();
        network.getVscConverterStation(creationParameters.getVscCreationParameters(HvdcLine.Side.ONE).getId()).remove();
        network.getVscConverterStation(creationParameters.getVscCreationParameters(HvdcLine.Side.TWO).getId()).remove();
    }

    private static void connectEquivalentAcLine(Network network, HvdcCreationParameters creationParameters) {
        HvdcLine hvdcLine = network.getHvdcLine(creationParameters.getId());
        Line equivalentAcLine = getLineOrThrow(network, creationParameters.getEquivalentAcLineId());
        if (hvdcLine.getConverterStation1().getTerminal().isConnected()) {
            equivalentAcLine.getTerminal1().connect();
        }
        if (hvdcLine.getConverterStation2().getTerminal().isConnected()) {
            equivalentAcLine.getTerminal2().connect();
        }
    }

    public static void connectEquivalentGeneratorsAndLoads(Network network, HvdcCreationParameters creationParameters,  HvdcLine hvdcLine) {

        Load load1 = getLoadOrThrow(network, creationParameters.getEquivalentLoadId(HvdcLine.Side.ONE));
        Generator gen1 = getGeneratorOrThrow(network, creationParameters.getEquivalentGeneratorId(HvdcLine.Side.ONE));
        Load load2 = getLoadOrThrow(network, creationParameters.getEquivalentLoadId(HvdcLine.Side.TWO));
        Generator gen2 = getGeneratorOrThrow(network, creationParameters.getEquivalentGeneratorId(HvdcLine.Side.TWO));

        load1.getTerminal().connect();
        gen1.getTerminal().connect();
        load2.getTerminal().connect();
        gen2.getTerminal().connect();

        HvdcAngleDroopActivePowerControl angleDroopActivePowerControl = hvdcLine.getExtension(HvdcAngleDroopActivePowerControl.class);
        if (angleDroopActivePowerControl != null && angleDroopActivePowerControl.isEnabled()) {
            load1.setP0(0);
            gen1.setTargetP(0);
            load2.setP0(0);
            gen2.setTargetP(0);
        } else {
            double setpoint = hvdcLine.getActivePowerSetpoint();
            if (hvdcLine.getConvertersMode().equals(HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER)) {
                // If power flow is from Side1 -> Side2 : set power on load1 & gen2
                load1.setP0(setpoint);
                gen2.setTargetP(setpoint);
                load2.setP0(0);
                gen1.setTargetP(0);
            } else {
                // If power flow is from Side2 -> Side1 : set power on load2 & gen1
                load2.setP0(setpoint);
                gen1.setTargetP(setpoint);
                load1.setP0(0);
                gen2.setTargetP(0);
            }
        }
    }

    private static Generator getGeneratorOrThrow(Network network, String id) {
        Generator generator = network.getGenerator(id);
        if (generator == null) {
            throw new SweInvalidDataException(String.format("Generator with id: %s needed for HVDC link modelling does not exist in network ", id));
        }
        return generator;
    }

    private static Load getLoadOrThrow(Network network, String id) {
        Load load = network.getLoad(id);
        if (load == null) {
            throw new SweInvalidDataException(String.format("Load with id: %s needed for HVDC link modelling does not exist in network ", id));
        }
        return load;
    }

    private static Line getLineOrThrow(Network network, String id) {
        Line line = network.getLine(id);
        if (line == null) {
            throw new SweInvalidDataException(String.format("Line with id: %s needed for HVDC link modelling does not exist in network ", id));
        }
        return line;
    }
}

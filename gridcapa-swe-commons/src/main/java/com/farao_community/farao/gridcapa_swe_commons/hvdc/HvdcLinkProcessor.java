/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_swe_commons.hvdc;

import com.farao_community.farao.gridcapa_swe_commons.exception.SweInvalidDataNoDetailsException;
import com.farao_community.farao.gridcapa_swe_commons.hvdc.parameters.HvdcCreationParameters;
import com.farao_community.farao.gridcapa_swe_commons.hvdc.parameters.VscStationCreationParameters;
import com.powsybl.iidm.modification.NetworkModification;
import com.powsybl.iidm.modification.topology.CreateFeederBayBuilder;
import com.powsybl.iidm.modification.topology.RemoveFeederBayBuilder;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControl;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControlAdder;
import com.powsybl.math.graph.TraverseResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Replaces an HVDC line's equivalent AC model with an actual HVDC line, and vice-versa
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
public final class HvdcLinkProcessor {
    private HvdcLinkProcessor() {
        // should not be instantiated
    }

    private enum NetworkElement {
        GENERATOR("Generator"), LOAD("Load"), LINE("AC line");
        private final String message;

        NetworkElement(final String generator) {
            this.message = generator;
        }
    }

    /* ================================ */
    /* AC equivalent model to HVDC line */
    /* ================================ */

    public static void replaceEquivalentModelByHvdc(Network network, Set<HvdcCreationParameters> creationParametersSet) {
        // Sort HvdcCreationParameters to ensure repeatability

        List<HvdcCreationParameters> sortedHvdcCreationParameters = creationParametersSet.stream()
                .sorted(Comparator.comparing(HvdcCreationParameters::getId)).toList();
        final Map<NetworkElement, Collection<String>> missingElementsMap = new EnumMap<>(NetworkElement.class);
        for (HvdcCreationParameters parameter : sortedHvdcCreationParameters) {
            replaceEquivalentModelByHvdc(network, parameter, missingElementsMap);
        }
        if (!missingElementsMap.isEmpty()) {
            throw new SweInvalidDataNoDetailsException(String.format(buildExceptionMessage(missingElementsMap)));
        }
    }

    private static void disconnectElement(Optional<? extends Injection> optionalElement) {
        optionalElement.ifPresent(element -> element.getTerminal().disconnect());
    }

    private static void disconnectGeneratorAndLoad(Optional<Load> optionalLoad1, Optional<Load> optionalLoad2,
                                                   Optional<Generator> optionalGenerator1, Optional<Generator> optionalGenerator2) {
        // Disconnect both loads and generators
        disconnectElement(optionalLoad1);
        disconnectElement(optionalLoad2);
        disconnectElement(optionalGenerator1);
        disconnectElement(optionalGenerator2);
    }

    private static void replaceEquivalentModelByHvdc(Network network, HvdcCreationParameters creationParameters,
                                                     Map<NetworkElement, Collection<String>> missingElementsMap) {
        // Search Load and Generator
        Optional<Load> optionalLoad1 = getLoadWithFallback(network, creationParameters.getEquivalentLoadId(TwoSides.ONE), missingElementsMap);
        Optional<Load> optionalLoad2 = getOptionalLoad(network, creationParameters.getEquivalentLoadId(TwoSides.TWO), missingElementsMap);
        Optional<Generator> optionalGenerator1 = getOptionalGenerator(network, creationParameters.getEquivalentGeneratorId(TwoSides.ONE), missingElementsMap);
        Optional<Generator> optionalGenerator2 = getOptionalGenerator(network, creationParameters.getEquivalentGeneratorId(TwoSides.TWO), missingElementsMap);
        Optional<Line> optionalLine = getOptionalLine(network, creationParameters.getEquivalentAcLineId(), missingElementsMap);

        if (optionalGenerator1.isPresent() && optionalGenerator2.isPresent() && optionalLine.isPresent()) {
            // Create one VSC converter station on each side
            createVscStation(network, creationParameters, TwoSides.ONE, missingElementsMap);
            createVscStation(network, creationParameters, TwoSides.TWO, missingElementsMap);
            // Create the HVDC line
            createHvdcLine(optionalLine, network, creationParameters, missingElementsMap);
        }

        // Disconnect Load and Generator if present
        disconnectGeneratorAndLoad(optionalLoad1, optionalLoad2, optionalGenerator1, optionalGenerator2);

        //Disconnect the Ac line
        optionalLine.ifPresent(line -> {
            line.getTerminal1().disconnect();
            line.getTerminal2().disconnect();
        });

    }

    private static void createVscStation(Network network, HvdcCreationParameters creationParameters, TwoSides side,
                                         Map<NetworkElement, Collection<String>> missingElementsMap) {
        getOptionalGenerator(network, creationParameters.getEquivalentGeneratorId(side), missingElementsMap)
                .ifPresent(gen -> {
                    Terminal terminal = gen.getTerminal();
                    // WARNING : in CVG, this is done for the equivalent generator of HVDC line 1 for both HVDC lines. Here it is more generic => check if OK
                    VscStationCreationParameters vscStationCreationParameters = creationParameters.getVscCreationParameters(side);
                    double voltageSetpoint = terminal.isConnected() ? gen.getTargetV() : vscStationCreationParameters.getDefaultVoltageSetpoint();
                    VoltageLevel voltageLevel = terminal.getVoltageLevel();
                    VscConverterStationAdder adder = voltageLevel.newVscConverterStation()
                            .setId(vscStationCreationParameters.getId())
                            .setReactivePowerSetpoint(vscStationCreationParameters.getReactivePowerSetpoint())
                            .setLossFactor(vscStationCreationParameters.getLossFactor())
                            .setVoltageRegulatorOn(vscStationCreationParameters.isVoltageRegulatorOn())
                            .setVoltageSetpoint(voltageSetpoint)
                            .setFictitious(false)
                            .setEnsureIdUnicity(true);
                    NetworkModification modification = buildNetworkModification(adder, voltageLevel, terminal);
                    modification.apply(network);
                });
    }

    private static NetworkModification buildNetworkModification(VscConverterStationAdder adder, VoltageLevel voltageLevel, Terminal terminal) {
        String busOrBusbarId = switch (voltageLevel.getTopologyKind()) {
            case NODE_BREAKER -> {
                List<BusbarSection> busbarSections = getConnectedBusbarSectionListFromTerminal(terminal);
                if (busbarSections.isEmpty()) {
                    throw new IllegalStateException(
                            "No busbar sections found for terminal of " + terminal.getConnectable().getId()
                                    + " in voltage level " + voltageLevel.getId());
                }
                yield busbarSections.getFirst().getId();
            }
            case BUS_BREAKER -> {
                Bus connectableBus = terminal.getBusBreakerView().getConnectableBus();
                if (connectableBus == null) {
                    throw new IllegalStateException(
                            "Connectable bus is null for terminal of " + terminal.getConnectable().getId()
                                    + " in voltage level " + voltageLevel.getId());
                }
                yield connectableBus.getId();
            }
            default -> throw new IllegalArgumentException("Unsupported topology kind: " + voltageLevel.getTopologyKind());
        };
        return new CreateFeederBayBuilder()
                .withInjectionAdder(adder)
                .withLogOrThrowIfIncorrectPositionOrder(false)
                .withInjectionPositionOrder(0)
                .withBusOrBusbarSectionId(busOrBusbarId)
                .build();
    }

    public static List<BusbarSection> getConnectedBusbarSectionListFromTerminal(Terminal terminal) {
        List<BusbarSection> busbarSections = new ArrayList<>();
        int node = terminal.getNodeBreakerView().getNode();
        VoltageLevel.NodeBreakerView vlNbv = terminal.getVoltageLevel().getNodeBreakerView();
        boolean isConnected = terminal.isConnected();
        if (isConnected) {
            vlNbv.traverse(node, (node1, sw, node2) -> {
                if (sw != null && sw.isOpen()) {
                    return TraverseResult.TERMINATE_PATH;
                }
                return getTraverseResult(vlNbv, node2, busbarSections);
            });
        } else {
            vlNbv.traverse(node, (node1, sw, node2) -> {
                if (sw != null) {
                    return TraverseResult.CONTINUE;
                }
                return getTraverseResult(vlNbv, node2, busbarSections);
            });
        }
        return busbarSections;
    }

    private static TraverseResult getTraverseResult(VoltageLevel.NodeBreakerView view, int node,
                                                    List<BusbarSection> busbarSections) {
        Optional<Terminal> t = view.getOptionalTerminal(node);
        if (t.isPresent() && t.get().getConnectable() instanceof BusbarSection busbarSection) {
            busbarSections.add(busbarSection);
            return TraverseResult.TERMINATE_TRAVERSER;
        } else if (t.isPresent()) {
            return TraverseResult.TERMINATE_PATH;
        }
        return TraverseResult.CONTINUE;
    }

    private static void createHvdcLine(Optional<Line> optionalLine, Network network, HvdcCreationParameters creationParameters,
                                       Map<NetworkElement, Collection<String>> missingElementsMap) {

        // Check if equivalent line is connected before connecting the new HVDC line
        optionalLine.ifPresent(line -> {
            if (!missingElementsMap.containsKey(NetworkElement.GENERATOR)) {
                HvdcLine hvdcLine = network.newHvdcLine()
                        .setMaxP(creationParameters.getMaxP())
                        .setR(creationParameters.getR())
                        .setNominalV(creationParameters.getNominalV())
                        .setId(creationParameters.getId())
                        .setEnsureIdUnicity(true)
                        .setConverterStationId1(creationParameters.getVscCreationParameters(TwoSides.ONE).getId())
                        .setConverterStationId2(creationParameters.getVscCreationParameters(TwoSides.TWO).getId())
                        .setConvertersMode(HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER)
                        .setActivePowerSetpoint(0)
                        .add();
                if (line.getTerminal1().isConnected()) {
                    hvdcLine.getConverterStation1().getTerminal().connect();
                } else {
                    hvdcLine.getConverterStation1().getTerminal().disconnect();
                }
                if (line.getTerminal2().isConnected()) {
                    hvdcLine.getConverterStation2().getTerminal().connect();
                } else {
                    hvdcLine.getConverterStation2().getTerminal().disconnect();
                }
                // Create angle droop active power control extension
                if (creationParameters.getAngleDroopActivePowerControlParameters() != null) {
                    hvdcLine.newExtension(HvdcAngleDroopActivePowerControlAdder.class)
                            .withP0(creationParameters.getAngleDroopActivePowerControlParameters().getP0())
                            .withDroop(creationParameters.getAngleDroopActivePowerControlParameters().getDroop())
                            .withEnabled(true)
                            .add();
                }
            }
        });
        // WARNING : check if it is also necessary to disconnect the two tie-lines (which are currently not in the HvdcCreationParameters)
    }

    /* ================================ */
    /* HVDC line to AC equivalent model */
    /* ================================ */

    public static void replaceHvdcByEquivalentModel(Network network, Set<HvdcCreationParameters> creationParametersSet, List<HvdcInformation> hvdcInformationList) {
        final Map<NetworkElement, Collection<String>> missingElementsMap = new EnumMap<>(NetworkElement.class);
        creationParametersSet.forEach(parameter -> replaceHvdcByEquivalentModel(network, parameter, missingElementsMap, hvdcInformationList));
        if (!missingElementsMap.isEmpty()) {
            throw new SweInvalidDataNoDetailsException(String.format(buildExceptionMessage(missingElementsMap)));
        }
    }

    private static void replaceHvdcByEquivalentModel(Network network, HvdcCreationParameters creationParameters,
                                                     Map<NetworkElement, Collection<String>> missingElementsMap, List<HvdcInformation> hvdcInformationList) {

        Optional<Load> optionalLoad1 = getLoadWithFallback(network, creationParameters.getEquivalentLoadId(TwoSides.ONE), missingElementsMap);
        Optional<Generator> optionalGen1 = getOptionalGenerator(network, creationParameters.getEquivalentGeneratorId(TwoSides.ONE), missingElementsMap);
        Optional<Load> optionalLoad2 = getOptionalLoad(network, creationParameters.getEquivalentLoadId(TwoSides.TWO), missingElementsMap);
        Optional<Generator> optionalGen2 = getOptionalGenerator(network, creationParameters.getEquivalentGeneratorId(TwoSides.TWO), missingElementsMap);
        Optional<Line> optionalLine = getOptionalLine(network, creationParameters.getEquivalentAcLineId(), missingElementsMap);

        HvdcLine hvdcLine = network.getHvdcLine(creationParameters.getId());
        if (hvdcLine != null) {
            connectEquivalentGenerators(optionalGen1, optionalGen2, hvdcLine);
            connectEquivalentLoad(optionalLoad1, hvdcLine, true);
            connectEquivalentLoad(optionalLoad2, hvdcLine, false);
            connectEquivalentAcLine(optionalLine, hvdcLine);

            network.getHvdcLine(creationParameters.getId()).remove();
            new RemoveFeederBayBuilder().withConnectableId(creationParameters.getVscCreationParameters(TwoSides.ONE).getId()).build().apply(network);
            new RemoveFeederBayBuilder().withConnectableId(creationParameters.getVscCreationParameters(TwoSides.TWO).getId()).build().apply(network);
        } else {
            hvdcInformationList.forEach(hvdcInformation -> {
                if (hvdcInformation.getId().equalsIgnoreCase(creationParameters.getId())) {
                    copyInformationFromInitialNetwork(optionalLoad1, optionalGen1, optionalLoad2, optionalGen2, optionalLine, hvdcInformation);
                }
            });
        }
    }

    static void copyInformationFromInitialNetwork(Optional<Load> optionalLoad1, Optional<Generator> optionalGen1, Optional<Load> optionalLoad2, Optional<Generator> optionalGen2, Optional<Line> optionalAcLine, HvdcInformation hvdcInformation) {
        copyLoadInformation(optionalLoad1, hvdcInformation.getSide1LoadP(), hvdcInformation.isSide1LoadConnected());
        copyLoadInformation(optionalLoad2, hvdcInformation.getSide2LoadP(), hvdcInformation.isSide2LoadConnected());

        copyGeneratorInformation(optionalGen1, hvdcInformation.getSide1GeneratorTargetP(), hvdcInformation.isSide1GeneratorConnected());
        copyGeneratorInformation(optionalGen2, hvdcInformation.getSide2GeneratorTargetP(), hvdcInformation.isSide2GeneratorConnected());

        if (optionalAcLine.isPresent()) {
            if (hvdcInformation.isAcLineTerminal1Connected()) {
                optionalAcLine.get().getTerminal1().connect();
            }
            if (hvdcInformation.isAcLineTerminal2Connected()) {
                optionalAcLine.get().getTerminal2().connect();
            }
        }
    }

    private static void copyLoadInformation(final Optional<Load> optionalLoad, final double sideLoadP, final boolean isSideConnected) {
        if (optionalLoad.isPresent()) {
            optionalLoad.get().setP0(sideLoadP);
            if (isSideConnected) {
                optionalLoad.get().connect();
            }
        }
    }

    private static void copyGeneratorInformation(final Optional<Generator> optionalGenerator, final double sideTargetP, final boolean isSideConnected) {
        if (optionalGenerator.isPresent()) {
            optionalGenerator.get().setTargetP(sideTargetP);
            if (isSideConnected) {
                optionalGenerator.get().connect();
            }
        }
    }

    private static void connectEquivalentAcLine(Optional<Line> optionalLine, HvdcLine hvdcLine) {
        optionalLine.ifPresent(ol -> {
            HvdcAngleDroopActivePowerControl angleDroopActivePowerControl = hvdcLine.getExtension(HvdcAngleDroopActivePowerControl.class);
            if (angleDroopActivePowerControl != null && angleDroopActivePowerControl.isEnabled()) {
                if (hvdcLine.getConverterStation1().getTerminal().isConnected()) {
                    ol.getTerminal1().connect();
                }
                if (hvdcLine.getConverterStation2().getTerminal().isConnected()) {
                    ol.getTerminal2().connect();
                }
            }
        });
    }

    private static void connectEquivalentGenerators(Optional<Generator> optionalGen1, Optional<Generator> optionalGen2,
                                                    HvdcLine hvdcLine) {
        if (optionalGen1.isPresent()  && optionalGen2.isPresent()) {
            Generator gen1 = optionalGen1.get();
            Generator gen2 = optionalGen2.get();
            gen1.getTerminal().connect();
            gen2.getTerminal().connect();
            HvdcAngleDroopActivePowerControl angleDroopActivePowerControl = hvdcLine.getExtension(HvdcAngleDroopActivePowerControl.class);
            if (angleDroopActivePowerControl != null && angleDroopActivePowerControl.isEnabled()) {
                gen1.setTargetP(0);
                gen2.setTargetP(0);
            } else {
                double setpoint = hvdcLine.getActivePowerSetpoint();
                if (hvdcLine.getConvertersMode().equals(HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER)) {
                    // If power flow is from Side1 -> Side2 : set power on gen2
                    gen2.setTargetP(setpoint);
                    gen1.setTargetP(0);
                } else {
                    // If power flow is from Side2 -> Side1 : set power on gen1
                    gen1.setTargetP(setpoint);
                    gen2.setTargetP(0);
                }
            }
        }
    }

    private static void connectEquivalentLoad(Optional<Load> optionalLoad, HvdcLine hvdcLine, boolean isSide1) {
        if (optionalLoad.isPresent()) {
            Load load = optionalLoad.get();
            load.getTerminal().connect();

            HvdcAngleDroopActivePowerControl angleDroopActivePowerControl = hvdcLine.getExtension(HvdcAngleDroopActivePowerControl.class);
            if (angleDroopActivePowerControl != null && angleDroopActivePowerControl.isEnabled()) {
                load.setP0(0);
            } else {
                double setpoint = hvdcLine.getActivePowerSetpoint();
                if (hvdcLine.getConvertersMode().equals(HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER)) {
                    load.setP0(isSide1 ? setpoint : 0);
                } else {
                    load.setP0(isSide1 ? 0 : setpoint);
                }
            }
        }
    }

    private static Optional<Generator> getOptionalGenerator(Network network, String id,
                                                            Map<NetworkElement, Collection<String>> missingElementsMap) {
        Optional<Generator> generator = Optional.ofNullable(network.getGenerator(id));
        if (generator.isEmpty()) {
            missingElementsMap.computeIfAbsent(NetworkElement.GENERATOR, k -> new HashSet<>()).add(id);
        }
        return generator;
    }

    private static Optional<Load> getOptionalLoad(Network network, Map<Integer, String> idsBypriority,
                                                  Map<NetworkElement, Collection<String>> missingElementsMap) {
        Optional<Load> load = Optional.ofNullable(network.getLoad(idsBypriority.get(1)));
        if (load.isEmpty()) {
            missingElementsMap.computeIfAbsent(NetworkElement.LOAD, k -> new HashSet<>()).add(idsBypriority.get(1));
        }
        return load;
    }

    private static Optional<Load> getOptionalLoadWithSecondIdOption(Network network, Map<Integer, String> idsBypriority,
                                                                    Map<NetworkElement, Collection<String>> missingElementsMap) {
        Optional<Load> load = Optional.ofNullable(network.getLoad(idsBypriority.get(2)));
        if (load.isEmpty()) {
            missingElementsMap.computeIfAbsent(NetworkElement.LOAD, k -> new HashSet<>()).add(idsBypriority.get(2));
        }
        return load;
    }

    // Utility method to get Load, falling back to the second ID option if needed
    private static Optional<Load> getLoadWithFallback(Network network, Map<Integer, String> idsByPriority,
                                                      Map<NetworkElement, Collection<String>> missingElementsMap) {
        Optional<Load> load = getOptionalLoad(network, idsByPriority, missingElementsMap);
        if (load.isEmpty()) {
            // Fallback to second load ID if the first one is not present
            load = getOptionalLoadWithSecondIdOption(network, idsByPriority, missingElementsMap);
        }
        return load;
    }

    private static Optional<Line> getOptionalLine(Network network, String id,
                                                  Map<NetworkElement, Collection<String>> missingElementsMap) {
        Optional<Line> line = Optional.ofNullable(network.getLine(id));
        if (line.isEmpty()) {
            missingElementsMap.computeIfAbsent(NetworkElement.LINE, k -> new HashSet<>()).add(id);
        }
        return line;
    }

    private static String buildExceptionMessage(Map<NetworkElement, Collection<String>> missingElementsMap) {
        final StringBuilder sb = new StringBuilder();
        if (missingElementsMap.containsKey(NetworkElement.GENERATOR) || missingElementsMap.containsKey(NetworkElement.LINE)) {
            sb.append("HVDC not created. Some network elements missing in input CGM prevent from creating HVDC devices needed by the process: ");
        } else {
            sb.append("HVDC created even if Load is missing. Missing elements in input CGM: ");
        }

        missingElementsMap.forEach((k, v) -> {
            sb.append("\n")
                    .append(k.message)
                    .append(" with id : ");
            v.forEach(id -> sb.append(id).append(" "));
        });
        return sb.toString();
    }
}

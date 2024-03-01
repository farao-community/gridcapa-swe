/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_swe_commons.shift;

import com.farao_community.farao.dichotomy.api.exceptions.ShiftingException;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.IdentifiableType;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.openrao.commons.EICode;
import org.jgrapht.alg.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Before scaling: fetches scalable generators in ES and PT scalables. For generators connected to the grid through a
 * transformer, if the transformer is disconnected, connects the transformer.
 * After scaling: reverts unnecessary changes.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class ScalableGeneratorConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScalableGeneratorConnector.class);
    private Map<String, GeneratorState> changedGeneratorsInitialState;
    private final ZonalData<Scalable> zonalScalable;

    private Set<Generator> changedGeneratorsConnected = new HashSet<>();

    public ScalableGeneratorConnector(ZonalData<Scalable> zonalScalable) {
        this.zonalScalable = zonalScalable;
    }

    public void prepareForScaling(Network network, Set<Country> countriesToProcess) throws ShiftingException {
        changedGeneratorsInitialState = new HashMap<>();
        for (Country c : countriesToProcess) {
            connectGeneratorsToMainComponent(network, c);
        }
    }

    /**
     * Collects the list of generators of a given country, that are present in the scalable list, but not connected
     * to the network's main component
     * For every one of these generators, checks if it has a transformer connecting it to the main component, and
     * connects it
     */
    void connectGeneratorsToMainComponent(Network network, Country country) throws ShiftingException {
        Set<Generator> generators = zonalScalable.getData(new EICode(country).getAreaCode()).filterInjections(network)
                .stream()
                .filter(Generator.class::isInstance)
                .map(Generator.class::cast)
                .filter(gen -> gen.getTerminal().getVoltageLevel().getSubstation().isPresent()
                        && gen.getTerminal().getVoltageLevel().getSubstation().get().getCountry().equals(Optional.of(country))
                        && !getBus(gen.getTerminal()).isInMainConnectedComponent()).collect(Collectors.toSet());
        for (Generator gen : generators) {
            connectGeneratorTwoWindingsTransformer(gen, network);
        }
    }

    /**
     * Checks if a generator has exactly one transformer connecting it to the main network and connects it.
     * (If no transformer exist, nothing is modified)
     * (If more than one transformer exist, TODO)
     * This method also stores information about the initial state of the generator and its transformer, in order to
     * revert modifications after scaling, for generators that were not used
     */
    private void connectGeneratorTwoWindingsTransformer(Generator generator, Network network) throws ShiftingException {
        // Store initial targetP for each generator, in order to revert this change after shift,
        // for the generators that are not connected to main island
        GeneratorState generatorState = new GeneratorState(generator);
        changedGeneratorsInitialState.put(generator.getId(), generatorState);
        // If generator is connected to network through transformers, connect them
        generatorState.twoWindingsTransformerConnection.keySet().forEach(twtId -> {
            TwoWindingsTransformer twt = network.getTwoWindingsTransformer(twtId);
            twt.getTerminal1().connect();
            twt.getTerminal2().connect();
        });
    }

    /**
     * After scaling, this method resets the initial state of every generator and transformer that were modified by
     * "connectGeneratorsToMainComponent", if the generator was not used by the scaling (i.e. if it's still not
     * connected to the main component, or targetP = initTargetP = 0)
     */
    public void revertUnnecessaryChanges(Network network) {
        changedGeneratorsInitialState.forEach((genId, initialState) -> {
            Generator gen = network.getGenerator(genId);
            if (!getBus(gen.getTerminal()).isInMainConnectedComponent()
                    || Math.abs(gen.getTargetP()) < 1e-6 && Math.abs(initialState.targetP) < 1e-6) {
                // Generator is not connected to the main island, even after connecting it and its TWT
                // Or it has 0 production and has not moved
                // Reset it to its state before scaling
                initialState.apply(network);
            }
        });
    }

    private Bus getBus(Terminal terminal) {
        return terminal.getBusBreakerView().getConnectableBus();
    }

    public void connectGeneratorsTransformers(Network network, Set<Country> countriesToProcess) {
        for (Country c : countriesToProcess) {
            Set<Generator> generators = getShiftedGeneratorsDisconnectedFromMainComponent(network, c);
            generators.forEach(generator -> {
                connectTransformersOfGenerator(generator);
                changedGeneratorsConnected.add(generator);
            });

        }
    }

    private Set<Generator> getShiftedGeneratorsDisconnectedFromMainComponent(Network network, Country country) {
        Set<Generator> generators = zonalScalable.getData(new EICode(country).getAreaCode()).filterInjections(network)
                .stream()
                .filter(Generator.class::isInstance)
                .map(Generator.class::cast)
                .filter(gen -> gen.getTerminal().getVoltageLevel().getSubstation().isPresent()
                        && gen.getTerminal().getVoltageLevel().getSubstation().get().getCountry().equals(Optional.of(country))
                        && gen.getTerminal().isConnected()
                        && Math.abs(gen.getTargetP()) > 0.0
                        && !getBus(gen.getTerminal()).isInMainConnectedComponent()).collect(Collectors.toSet());
        return generators;
    }

    /**
     * Stores info about the state of the generator and its eventual transformer in the network
     */
    private class GeneratorState {
        String generatorId;
        double targetP;
        boolean isTerminalConnected;
        Map<String, Pair<Boolean, Boolean>> twoWindingsTransformerConnection;

        GeneratorState(Generator generator) throws ShiftingException {
            Bus genBus = getBus(generator.getTerminal());
            if (genBus == null) {
                throw new ShiftingException(String.format("Unknown terminal bus for generator %s", generator.getId()));
            }
            this.generatorId = generator.getId();
            this.targetP = generator.getTargetP();
            this.isTerminalConnected = generator.getTerminal().isConnected();
            this.twoWindingsTransformerConnection = new HashMap<>();
            generator.getTerminal().getVoltageLevel().getTwoWindingsTransformers().forEach(twt -> {
                if (genBus.equals(getBus(twt.getTerminal1())) || genBus.equals(getBus(twt.getTerminal2()))) {
                    twoWindingsTransformerConnection.put(twt.getId(), Pair.of(twt.getTerminal1().isConnected(), twt.getTerminal2().isConnected()));
                }
            });
        }

        void apply(Network network) {
            Generator generator = network.getGenerator(this.generatorId);
            Objects.requireNonNull(generator);
            generator.setTargetP(this.targetP);
            // TODO: the scaling actually does more stuff when connecting the generator (see ConnectGenerator.apply);
            //  do we have to do the exact opposite when disconnecting?
            connectTerminal(generator.getTerminal(), this.isTerminalConnected);

            twoWindingsTransformerConnection.forEach((twtId, terminalConnection) -> {
                TwoWindingsTransformer twoWindingsTransformer = network.getTwoWindingsTransformer(twtId);
                Objects.requireNonNull(twoWindingsTransformer);
                connectTerminal(twoWindingsTransformer.getTerminal1(), terminalConnection.getFirst());
                connectTerminal(twoWindingsTransformer.getTerminal2(), terminalConnection.getSecond());
            });
        }

        void connectTerminal(Terminal terminal, boolean connect) {
            if (connect) {
                terminal.connect();
            } else {
                terminal.disconnect();
            }
        }
    }

    /**
     * Checks if generator is connected to the network with transformers and connecting them.
     * (If no transformer exist, nothing is modified)
     */
    private static void connectTransformersOfGenerator(Generator generator) {
        ConnectableTopologyVisitor connectableTopologyVisitor = new ConnectableTopologyVisitor();
        generator.getTerminal().getBusBreakerView().getConnectableBus().visitConnectedOrConnectableEquipments(connectableTopologyVisitor);
        //todo use directly getVoltageLevel().getConnectables
        connectableTopologyVisitor.getConnectables().stream().filter(connectable -> connectable.getType().equals(IdentifiableType.TWO_WINDINGS_TRANSFORMER))
                .map(c -> (TwoWindingsTransformer) c)
                .forEach(twt -> {
                    LOGGER.info("Connecting twoWindingsTransformer {} linked to generator {}", twt.getId(), generator.getId());
                    twt.getTerminals().forEach(Terminal::connect);
                });
    }
}

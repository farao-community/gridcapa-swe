/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_swe_commons.shift;

import com.farao_community.farao.dichotomy.api.exceptions.ShiftingException;
import com.farao_community.farao.dichotomy.api.results.ReasonInvalid;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.openrao.commons.EICode;
import org.jgrapht.alg.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
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
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public class ScalableGeneratorConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScalableGeneratorConnector.class);
    private Map<String, GeneratorState> changedGeneratorsInitialState;
    private final ZonalData<Scalable> zonalScalable;

    public ScalableGeneratorConnector(ZonalData<Scalable> zonalScalable) {
        this.zonalScalable = zonalScalable;
    }

    /**
     * This method stores information about the initial state of the generator and its transformer, in order to
     * revert modifications after scaling, for generators that still disconnected to main network
     */
    public void fillGeneratorsInitialState(Network network, Set<Country> countriesToProcess) throws ShiftingException {
        changedGeneratorsInitialState = new HashMap<>();
        for (Country country : countriesToProcess) {
            Set<Generator> generators = getGeneratorsNotMainConnected(network, country);
            for (Generator generator : generators) {
                GeneratorState generatorState = new GeneratorState(generator);
                changedGeneratorsInitialState.put(generator.getId(), generatorState);
            }
        }
    }

    /**
     * Collects the list of generators of a given country, that are present in the scalable list, but not connected
     * to the network's main component
     */
    private Set<Generator> getGeneratorsNotMainConnected(Network network,
                                                         Country country) {
        final String areaCode = new EICode(country).getAreaCode();
        if (zonalScalable.getData(areaCode) != null) {
            return zonalScalable.getData(areaCode).filterInjections(network)
                    .stream()
                    .filter(Generator.class::isInstance)
                    .map(Generator.class::cast)
                    .filter(gen -> gen.getTerminal().getVoltageLevel().getSubstation().isPresent()
                                   && gen.getTerminal().getVoltageLevel().getSubstation().get().getCountry().equals(Optional.of(country))
                                   && !getBus(gen.getTerminal()).isInMainConnectedComponent()).collect(Collectors.toSet());
        } else {
            return Set.of();
        }
    }

    private static Bus getBus(Terminal terminal) {
        return terminal.getBusBreakerView().getConnectableBus();
    }

    /**
     * For countriesToProcess, get the generators used during shift but are not connected to the main network
     * For these generators, try to connect them to the network by connecting the associated transformers
     * If Generator still disconnected from main component, revert to the initial TargetP and connexion for transformers
     */
    public void connectGeneratorsTransformers(Network network, Set<Country> countriesToProcess) {
        for (Country c : countriesToProcess) {
            Set<Generator> generators = getShiftedGeneratorsDisconnectedFromMainComponent(network, c);
            generators.forEach(generator -> connectTransformersOfGenerator(generator, network));
        }
    }

    /**
     * Checks if generator is connected to the network with transformers and connecting them.
     * (If no transformer exist, nothing is modified)
     * If Generator still disconnected from main component, revert to the initial state
     */
    private void connectTransformersOfGenerator(Generator generator, Network network) {
        Bus genBus = getBus(generator.getTerminal());
        Set<TwoWindingsTransformer> transformers = generator.getTerminal().getVoltageLevel().getTwoWindingsTransformerStream()
                .filter(twt -> genBus.equals(getBus(twt.getTerminal1())) || genBus.equals(getBus(twt.getTerminal2())))
                .collect(Collectors.toSet());
        transformers.forEach(twt -> {
            LOGGER.info("Connecting twoWindingsTransformer {} linked to generator {}", twt.getId(), generator.getId());
            twt.getTerminals().forEach(Terminal::connect);
        });
        // Generator is not connected to the main island, even after connecting it and its TWT
        // Reset it to it's initial state before scaling
        if (!genBus.isInMainConnectedComponent()) {
            LOGGER.info("Generator {} still disconnected to the main network, reset to initial state", generator.getId());
            GeneratorState initialState = changedGeneratorsInitialState.get(generator.getId());
            if (initialState != null) {
                initialState.apply(network);
            }
        }
    }

    private Set<Generator> getShiftedGeneratorsDisconnectedFromMainComponent(Network network, Country country) {
        Set<Generator> generators = getGeneratorsNotMainConnected(network, country);
        return generators.stream().filter(this::isShiftedGenerator).collect(Collectors.toSet());
    }

    private boolean isShiftedGenerator(Generator gen) {
        double initialTargetP = changedGeneratorsInitialState.containsKey(gen.getId()) ? changedGeneratorsInitialState.get(gen.getId()).targetP : 0.;
        return gen.getTerminal().isConnected() && Math.abs(gen.getTargetP() - initialTargetP) > 1e-6;
    }

    Map<String, GeneratorState> getChangedGeneratorsInitialState() {
        return changedGeneratorsInitialState;
    }

    /**
     * Stores info about the state of the generator and its eventual transformer in the network
     */
    class GeneratorState {
        String generatorId;
        double targetP;
        boolean isTerminalConnected;
        Map<String, Pair<Boolean, Boolean>> twoWindingsTransformerConnection;

        GeneratorState(Generator generator) throws ShiftingException {
            Bus genBus = getBus(generator.getTerminal());
            if (genBus == null) {
                throw new ShiftingException(String.format("Unknown terminal bus for generator %s", generator.getId()), ReasonInvalid.UNKNOWN_TERMINAL_BUS);
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

}

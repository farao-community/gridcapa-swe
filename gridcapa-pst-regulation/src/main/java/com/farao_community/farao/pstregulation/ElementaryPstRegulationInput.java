/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.pstregulation;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Aggregates the regulation data computed for one PST on one monitored element: the {@link PstRangeAction} to apply,
 * the transformer side that must regulate, and the most constraining current threshold extracted from the associated
 * {@link FlowCnec flow CNECs}.
 *
 * <p>Built by {@link PstRegulation} and consumed by {@link PstRegulator} to configure OpenLoadFlow current-limiter
 * regulation. The {@code limitingSide} selects the {@link TwoSides} terminal used for regulation, and
 * {@code limitingThreshold} is the absolute current limit in amperes retained from the monitored CNECs on that side.
 *
 * @see PstRegulationInput aggregates one elementary input per regulated PST
 * @see PstRegulator applies this input on the network model
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public record ElementaryPstRegulationInput(PstRangeAction pstRangeAction, TwoSides limitingSide, double limitingThreshold) {
    public static ElementaryPstRegulationInput of(PstRangeAction pstRangeAction, String monitoredNetworkElement, State state, Crac crac, Network network) {
        Set<FlowCnec> curativeFlowCnecs = crac.getFlowCnecs(state).stream()
            .filter(flowCnec -> monitoredNetworkElement.equals(flowCnec.getNetworkElement().getId()))
            .collect(Collectors.toSet());
        if (curativeFlowCnecs.isEmpty()) {
            return null;
        }

        // if the PST monitors itself, the most limiting terminal is used for regulation
        if (pstRangeAction.getNetworkElement().getId().equals(monitoredNetworkElement)) {
            double thresholdOne = getMostLimitingThreshold(curativeFlowCnecs, TwoSides.ONE);
            double thresholdTwo = getMostLimitingThreshold(curativeFlowCnecs, TwoSides.TWO);
            return thresholdOne <= thresholdTwo ? new ElementaryPstRegulationInput(pstRangeAction, TwoSides.ONE, thresholdOne) : new ElementaryPstRegulationInput(pstRangeAction, TwoSides.TWO, thresholdTwo);
        }

        // otherwise, the terminal in common with the element in series it monitors is used
        Pair<TwoSides, TwoSides> commonTerminalSides = getSidesOfCommonTerminal(pstRangeAction, monitoredNetworkElement, network);
        return new ElementaryPstRegulationInput(pstRangeAction, commonTerminalSides.getLeft(), getMostLimitingThreshold(curativeFlowCnecs, commonTerminalSides.getRight()));
    }

    private static double getMostLimitingThreshold(Set<FlowCnec> curativeFlowCnecs, TwoSides twoSides) {
        return curativeFlowCnecs.stream().mapToDouble(flowCnec -> getMostLimitingThreshold(flowCnec, twoSides)).min().orElse(Double.MAX_VALUE);
    }

    /**
     * Retrieves the most limiting current threshold of a FlowCnec (in Amperes) on a given side.
     * The returned threshold in always positive since it accounts for a line loading (sign is just for direction).
     */
    private static double getMostLimitingThreshold(FlowCnec flowCnec, TwoSides twoSides) {
        double mostLimitingThreshold = Double.MAX_VALUE;

        Optional<Double> upperBound = flowCnec.getUpperBound(twoSides, Unit.AMPERE);
        // current threshold -> sign is used for current direction so upper bound is expected to be positive
        if (upperBound.isPresent() && upperBound.get() >= 0) {
            mostLimitingThreshold = upperBound.get();
        }

        Optional<Double> lowerBound = flowCnec.getLowerBound(twoSides, Unit.AMPERE);
        // current threshold -> sign is used for current direction so lower bound is expected to be negative
        if (lowerBound.isPresent() && lowerBound.get() <= 0) {
            mostLimitingThreshold = Math.min(mostLimitingThreshold, -lowerBound.get());
        }

        return mostLimitingThreshold;
    }

    /**
     * If the PST monitors a line connected in series, returns a pair that contains the respective side of the terminal
     * they share in common.
     */
    private static Pair<TwoSides, TwoSides> getSidesOfCommonTerminal(PstRangeAction pstRangeAction, String monitoredNetworkElement, Network network) {
        Branch<?> branch = network.getBranch(monitoredNetworkElement);
        if (branch == null) {
            throw new OpenRaoException("No branch with id '%s' found in network.".formatted(monitoredNetworkElement));
        }

        String twoWindingsTransformerId = pstRangeAction.getNetworkElement().getId();
        TwoWindingsTransformer twoWindingsTransformer = network.getTwoWindingsTransformer(twoWindingsTransformerId);
        if (twoWindingsTransformer == null) {
            throw new OpenRaoException("No two-windings transformer with id '%s' found in network.".formatted(twoWindingsTransformerId));
        }

        for (TwoSides twtSide : TwoSides.values()) {
            for (TwoSides branchSide : TwoSides.values()) {
                if (twoWindingsTransformer.getTerminal(twtSide).getVoltageLevel().getId().equals(branch.getTerminal(branchSide).getVoltageLevel().getId())) {
                    return Pair.of(twtSide, branchSide);
                }
            }
        }

        throw new OpenRaoException("Two-windings transformer '%s' and branch '%s' do not share a common terminal so PST regulation cannot be performed.".formatted(twoWindingsTransformerId, monitoredNetworkElement));
    }
}

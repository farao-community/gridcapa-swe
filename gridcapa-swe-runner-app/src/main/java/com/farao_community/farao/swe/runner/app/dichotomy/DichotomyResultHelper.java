/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.farao_community.farao.dichotomy.api.results.LimitingCause;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public final class DichotomyResultHelper {

    private DichotomyResultHelper() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static String limitingCauseToString(LimitingCause limitingCause) {
        // For the event, we don't use dichotomyResult.getLimitingFailureMessage() because it contains the exception message
        switch (limitingCause) {
            case CRITICAL_BRANCH:
                return "Critical Branch";
            case GLSK_LIMITATION:
                return "GSK Limitation";
            case COMPUTATION_FAILURE:
                return "Computation Failure";
            case INDEX_EVALUATION_OR_MAX_ITERATION:
            default:
                return "None";
        }
    }

    public static String getLimitingElement(Crac crac, RaoResult raoResult) {
        double worstMargin = Double.MAX_VALUE;
        Optional<FlowCnec> worstCnec = Optional.empty();
        for (FlowCnec flowCnec : crac.getFlowCnecs()) {
            double margin = computeFlowMargin(raoResult, flowCnec);
            if (margin < worstMargin) {
                worstMargin = margin;
                worstCnec = Optional.of(flowCnec);
            }
        }

        return worstCnec.isPresent() ? worstCnec.get().getName() : "None";
    }

    private static double computeFlowMargin(RaoResult raoResult, FlowCnec flowCnec) {
        return raoResult.getMargin(flowCnec.getState().getInstant(), flowCnec, Unit.AMPERE);
    }

    public static List<String> getActivatedActionInPreventive(Crac crac, RaoResult raoResult) {
        List<String> prasNames = raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState()).stream().map(NetworkAction::getName).collect(Collectors.toList());
        prasNames.addAll(raoResult.getActivatedRangeActionsDuringState(crac.getPreventiveState()).stream().map(RangeAction::getName).collect(Collectors.toList()));
        return prasNames;
    }

    public static List<String> getActivatedActionInCurative(Crac crac, RaoResult raoResult) {
        //CURATIVE && AUTO
        Set<String> crasNames = new HashSet<>();
        crac.getStates().stream().filter(state -> state.getInstant().isCurative() || state.getInstant().isAuto()).forEach(state -> {
            crasNames.addAll(raoResult.getActivatedNetworkActionsDuringState(state).stream().map(NetworkAction::getName).collect(Collectors.toSet()));
            crasNames.addAll(raoResult.getActivatedRangeActionsDuringState(state).stream().map(RangeAction::getName).collect(Collectors.toSet()));
        });
        return new ArrayList<>(crasNames);
    }
}

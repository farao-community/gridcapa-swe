/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.post_processing;

import com.powsybl.computation.ComputationManager;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Identifiable;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.impl.FlowCnecValue;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.util.AbstractNetworkPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Daniel THIRION {@literal <daniel.thirion at rte-france.com>}
 */
public class PstSeries {
    private static final double EPSILON = 0.1;
    private static final Logger LOGGER = LoggerFactory.getLogger(PstSeries.class);
    final LoadFlowParameters loadFlowParameters;
    final Crac crac;
    final Network inputNetwork;
    final RaoResult raoResult;
    private final List<TapInfo> taps = new ArrayList<>();
    private final List<CnecMarginInfo> cnecMarginInfos = new ArrayList<>();
    private static final String ARKALE_PST_NE = "_e071a1d4-fef5-1bd9-5278-d195c5597b6e";
    private static final String PRAGNY_PST_NE = "_f82152ac-578e-500e-97db-84e788c471ee";
    private static final String ARKALE_ARGIA_CNEC_NE = "_1d9c658e-1a01-c0ee-d127-a22e1270a242 + _2e81de07-4c22-5aa1-9683-5e51b054f7f8";
    private static final String PRAGN_BIESCAS_CNEC_NE = "_0a3cbdb0-cd71-52b0-b93d-cb48c9fea3e2 + _6f6b15b3-9bcc-7864-7669-522e9f06e931";

    public PstSeries(final Network network,
                     final RaoResult raoResult,
                     final LoadFlowParameters loadFlowParameters,
                     final Crac crac) {
        this.inputNetwork = network;
        this.crac = crac;
        this.raoResult = raoResult;
        this.loadFlowParameters = loadFlowParameters;

    }

    public void runRegulationLoadFLow() {
        // Apply PRAs
        final State preventiveState = crac.getPreventiveState();
        if (Objects.nonNull(preventiveState)) {
            applyOptimalRemedialActions(preventiveState, inputNetwork, raoResult);
        }

        // Select contingency states for which most limiting element at an optimized instant was ARKALE or BIESCAS.
        extractLimitingElementsFromRaoResult();
        final Set<FlowCnec> limitingCnecstoBeMonitored = cnecMarginInfos.stream().map(CnecMarginInfo::getCnec).collect(Collectors.toSet());
        final Set<State> contingencyStates = limitingCnecstoBeMonitored.stream().map(Cnec::getState).filter(state -> !state.isPreventive()).collect(Collectors.toSet());

        // loop on contingency states
        final int nbThreads = 1; //Math.min(2, contingencyStates.size());
        try (final AbstractNetworkPool networkPool = AbstractNetworkPool.create(inputNetwork, inputNetwork.getVariantManager().getWorkingVariantId(), nbThreads, true)) {
            final List<ForkJoinTask<Object>> tasks = contingencyStates.stream().map(state ->
                    networkPool.submit(() -> {
                        final Network networkClone = networkPool.getAvailableNetwork();
                        final Contingency contingency = state.getContingency().orElseThrow();
                        LOGGER.info(String.format("--- State %s ---", state.getId()));

                        if (!contingency.isValid(networkClone)) {
                            LOGGER.info("Invalid contingency");
                            networkPool.releaseUsedNetwork(networkClone);
                            return null;
                        }
                        contingency.toModification().apply(networkClone, (ComputationManager) null);
                        applyOptimalRemedialActionsOnContingencyState(state, networkClone, crac, raoResult);

                        runInitialLoadFlow(networkClone, state);

                        // Log limiting cnecs' margins
                        final Set<FlowCnec> currentStateToBeMonitoredCnecs = limitingCnecstoBeMonitored.stream().filter(cnec -> cnec.getState().equals(state)).collect(Collectors.toSet());
                        currentStateToBeMonitoredCnecs.forEach(toBeMonitoredCnec -> {
                            final CnecMarginInfo cnecMarginInfo1 = cnecMarginInfos.stream().filter(loggedCnec -> toBeMonitoredCnec.equals(loggedCnec.getCnec())).findFirst().get();
                            cnecMarginInfo1.setMarginBefore(toBeMonitoredCnec.computeMargin(networkClone, Unit.AMPERE));
                            cnecMarginInfo1.setMarginAfter(cnecMarginInfo1.getMarginBefore());
                        });

                        final Set<String> regulatedPsts = setPstToRegulating(state, networkClone, currentStateToBeMonitoredCnecs);

                        if (!regulatedPsts.isEmpty()) {
                            runLoadFlowWithRegulation(networkClone, state);

                            // store new taps + margins
                            if (regulatedPsts.contains(PRAGNY_PST_NE)) {
                                taps.stream().filter(pstInfo -> pstInfo.getNetworkElement().equals(PRAGNY_PST_NE) && pstInfo.getState().equals(state))
                                        .findFirst().ifPresent(value -> value.setTapAfter(networkClone.getTwoWindingsTransformer(PRAGNY_PST_NE).getPhaseTapChanger().getTapPosition()));
                            } else if (regulatedPsts.contains(ARKALE_PST_NE)) {
                                taps.stream().filter(pstInfo -> pstInfo.getNetworkElement().equals(ARKALE_PST_NE) && pstInfo.getState().equals(state))
                                        .findFirst().ifPresent(value -> value.setTapAfter(networkClone.getTwoWindingsTransformer(ARKALE_PST_NE).getPhaseTapChanger().getTapPosition()));
                            }
                            currentStateToBeMonitoredCnecs.forEach(toBeMonitoredCnec ->
                                    cnecMarginInfos.stream().filter(stateCnec -> toBeMonitoredCnec.equals(stateCnec.getCnec())).findFirst().get().setMarginAfter(toBeMonitoredCnec.computeMargin(networkClone, Unit.AMPERE)));

                            computeMarginsForState(state, networkClone);
                            generateRaoResult();
                        }
                        networkPool.releaseUsedNetwork(networkClone);
                        return null;
                    })).toList();

            for (final ForkJoinTask<Object> task : tasks) {
                try {
                    task.get();
                } catch (final ExecutionException e) {
                    throw new OpenRaoException(e);
                }
            }
            networkPool.shutdownAndAwaitTermination(24, TimeUnit.HOURS);
        } catch (final Exception e) {
            Thread.currentThread().interrupt();
        }
        logInfo();
    }

    private void generateRaoResult() {
        // TODO : generate new RaoResult
    }

    private void computeMarginsForState(final State state, final Network network) {
        List<CnecMarginInfo> sortedCnecsOld = new ArrayList<>();
        List<CnecMarginInfo> sortedCnecsNew = new ArrayList<>();

        for (final FlowCnec flowCnec : crac.getFlowCnecs(state)) {
            final CnecMarginInfo flowCnecMarginInfo = new CnecMarginInfo(flowCnec);
            flowCnecMarginInfo.setRaoResultMargin(raoResult.getMargin(state.getInstant(), flowCnec, Unit.AMPERE));
            flowCnecMarginInfo.setMarginAfter(flowCnec.computeMargin(network, Unit.AMPERE));
            sortedCnecsNew.add(flowCnecMarginInfo);
            sortedCnecsOld.add(flowCnecMarginInfo);
        }

        final List<String> summaryNew = new ArrayList<>();
        final List<String> summaryOld = new ArrayList<>();

        Collections.sort(sortedCnecsNew, Comparator.comparing(CnecMarginInfo::getMarginAfter));
        Collections.sort(sortedCnecsOld, Comparator.comparing(CnecMarginInfo::getRaoResultMargin));

        sortedCnecsNew = sortedCnecsNew.subList(0, Math.min(sortedCnecsNew.size(), 5));
        sortedCnecsOld = sortedCnecsOld.subList(0, Math.min(sortedCnecsOld.size(), 5));

        for (int i = 0; i < sortedCnecsNew.size(); i++) {
            final CnecMarginInfo cnecAndMargin = sortedCnecsNew.get(i);
            final FlowCnec cnec = cnecAndMargin.getCnec();
            final double cnecMargin = cnecAndMargin.getMarginAfter();
            summaryNew.add(String.format(Locale.ENGLISH, "Limiting element #%02d: margin = %.2f %s at state %s, CNEC ID = \"%s\"",
                    i + 1,
                    cnecMargin,
                    Unit.AMPERE,
                    state.getId(),
                    cnec.getId()));
        }

        for (int i = 0; i < sortedCnecsOld.size(); i++) {
            final CnecMarginInfo cnecAndMargin = sortedCnecsOld.get(i);
            final FlowCnec cnec = cnecAndMargin.getCnec();
            final double cnecMargin = cnecAndMargin.getRaoResultMargin();
            summaryOld.add(String.format(Locale.ENGLISH, "Limiting element #%02d: margin = %.2f %s at state %s, CNEC ID = \"%s\"",
                    i + 1,
                    cnecMargin,
                    Unit.AMPERE,
                    state.getId(),
                    cnec.getId()));
        }
        LOGGER.info("BEFORE REGULATION:");
        summaryOld.forEach(LOGGER::info);
        LOGGER.info("AFTER REGULATION:");
        summaryNew.forEach(LOGGER::info);
    }

    private Set<String> setPstToRegulating(final State state, final Network networkClone, final Set<FlowCnec> currentStateToBeMonitoredCnecs) {
        final Set<String> regulatedPsts = new HashSet<>();
        currentStateToBeMonitoredCnecs.forEach(flowCnec -> {
            double regulatingValue = 0;
            final Cnec.SecurityStatus ss = flowCnec.computeSecurityStatus(networkClone, Unit.AMPERE);
            final FlowCnecValue flow = (FlowCnecValue) flowCnec.computeValue(networkClone, Unit.AMPERE);
            final Set<TwoSides> monitoredSides = flowCnec.getMonitoredSides();
            if (monitoredSides.size() > 1) {
                LOGGER.info("unhandled for the moment");
            }
            final FlowCnecValue flowMW = (FlowCnecValue) flowCnec.computeValue(networkClone, Unit.MEGAWATT);
            if (monitoredSides.iterator().next() == TwoSides.ONE) {
                LOGGER.info(String.format("%s with flow = %s A has security status %s", flowCnec.getId(), Math.round(100. * flow.side1Value()) / 100., ss.toString()));
                LOGGER.info(String.format("%s with flow = %s MW has security status %s", flowCnec.getId(), Math.round(100. * flowMW.side1Value()) / 100., ss.toString()));
            } else {
                LOGGER.info(String.format("%s with flow = %s A has security status %s", flowCnec.getId(), Math.round(100. * flow.side2Value()) / 100, ss.toString()));
                LOGGER.info(String.format("%s with flow = %s MW has security status %s", flowCnec.getId(), Math.round(100. * flowMW.side2Value()) / 100., ss.toString()));
            }

            if (ss.equals(Cnec.SecurityStatus.SECURE)) {
                LOGGER.info("SECURE status, nothing to be done");
                return;
            }
            // TODO : ifPresent rather than get
            //Switch case instead or if else if
            if (ss.equals(Cnec.SecurityStatus.HIGH_CONSTRAINT)) {
                regulatingValue = flowCnec.getUpperBound(monitoredSides.iterator().next(), Unit.AMPERE).get();
            } else if (ss.equals(Cnec.SecurityStatus.LOW_CONSTRAINT)) {
                regulatingValue = flowCnec.getLowerBound(monitoredSides.iterator().next(), Unit.AMPERE).get();
            } else if (ss.equals(Cnec.SecurityStatus.HIGH_AND_LOW_CONSTRAINTS)) {
                // compute worse bound
                if (monitoredSides.iterator().next() == TwoSides.ONE) {
                    if ((flow.side1Value() - flowCnec.getUpperBound(monitoredSides.iterator().next(), Unit.AMPERE).get()) <
                        (flowCnec.getLowerBound(monitoredSides.iterator().next(), Unit.AMPERE).get() - flow.side1Value())) {
                        regulatingValue = flowCnec.getLowerBound(monitoredSides.iterator().next(), Unit.AMPERE).get();
                    } else {
                        regulatingValue = flowCnec.getUpperBound(monitoredSides.iterator().next(), Unit.AMPERE).get();
                    }
                } else {
                    if ((flow.side2Value() - flowCnec.getUpperBound(monitoredSides.iterator().next(), Unit.AMPERE).get()) <
                        (flowCnec.getLowerBound(monitoredSides.iterator().next(), Unit.AMPERE).get() - flow.side2Value())) {
                        regulatingValue = flowCnec.getLowerBound(monitoredSides.iterator().next(), Unit.AMPERE).get();
                    } else {
                        regulatingValue = flowCnec.getUpperBound(monitoredSides.iterator().next(), Unit.AMPERE).get();
                    }
                }
            }
            if (flowCnec.getNetworkElement().getId().equals(PRAGN_BIESCAS_CNEC_NE)) {
                LOGGER.info(String.format("Setting PRAGN PST regulating value to %s for state %s", regulatingValue, state.getId()));
                setPstInSeriesToRegulating(networkClone, PRAGNY_PST_NE, regulatingValue, state);
                regulatedPsts.add(PRAGNY_PST_NE);
            } else if (flowCnec.getNetworkElement().getId().equals(ARKALE_ARGIA_CNEC_NE)) {
                LOGGER.info(String.format("Setting ARKALE PST regulating value to %s for state %s", regulatingValue, state.getId()));
                setPstInSeriesToRegulating(networkClone, ARKALE_PST_NE, regulatingValue, state);
                regulatedPsts.add(ARKALE_PST_NE);
            } else {
                LOGGER.info("problem");
            }
        });
        return regulatedPsts;
    }

    private void extractLimitingElementsFromRaoResult() {
        final List<Instant> instants = crac.getSortedInstants();
        instants.forEach(instant -> {
            final double minMargin = -raoResult.getFunctionalCost(instant);
            crac.getStates(instant).forEach(state ->
                    crac.getFlowCnecs()
                            .stream()
                            // 1er filtre : Is BP or AA the most limiting CNE?
                            // 2e filtre : Are there available taps in PST linked to CNE?
                            .filter(flowCnec -> flowCnec.getNetworkElement().getId().equals(ARKALE_ARGIA_CNEC_NE) && availableTapsOnPst(ARKALE_PST_NE) ||
                                                flowCnec.getNetworkElement().getId().equals(PRAGN_BIESCAS_CNEC_NE) && availableTapsOnPst(PRAGNY_PST_NE))
                            .filter(cnecInList -> cnecInList.getState().equals(state)).collect(Collectors.toSet())
                            .forEach(cnec -> {
                                if (Math.abs(raoResult.getMargin(instant, cnec, Unit.AMPERE) - minMargin) < EPSILON) {
                                    final CnecMarginInfo cnecMarginInfo0 = new CnecMarginInfo(cnec);
                                    cnecMarginInfo0.setRaoResultMargin(raoResult.getMargin(instant, cnec, Unit.AMPERE));
                                    cnecMarginInfos.add(cnecMarginInfo0);
                                    LOGGER.info(String.format("%s is limiting with margin = %s A", cnec.getId(), cnecMarginInfo0.getRaoResultMargin()));
                                }
                            }));
        });
    }

    // besoin du rao result => sortir la parade et comparer au max et min dans powsybl
    private boolean availableTapsOnPst(final String pstNe) {
        final PhaseTapChanger ptc = this.inputNetwork.getTwoWindingsTransformer(pstNe).getPhaseTapChanger();
        final int currentTapPosition = ptc.getTapPosition();
        return ptc.getLowTapPosition() != currentTapPosition && ptc.getHighTapPosition() != currentTapPosition;
    }

    private void setPstInSeriesToRegulating(final Network network, final String pstNe, final double regulationValue, final State state) {
        final TwoWindingsTransformer twt = network.getTwoWindingsTransformer(pstNe);
        if (taps.stream().noneMatch(pstInfo -> pstInfo.getNetworkElement().equals(pstNe) && pstInfo.getState().equals(state))) {
            taps.add(new TapInfo(state, pstNe, twt.getNameOrId(), twt.getPhaseTapChanger().getTapPosition()));
        }

        if (Objects.nonNull(twt)) {
            twt.getPhaseTapChanger().setRegulating(true);
            twt.getPhaseTapChanger().setRegulationMode(PhaseTapChanger.RegulationMode.CURRENT_LIMITER);
            twt.getPhaseTapChanger().setRegulationValue(regulationValue);
        }
    }

    private boolean runInitialLoadFlow(final Network network, final State state) {
        loadFlowParameters.setPhaseShifterRegulationOn(false);
        final LoadFlowResult loadFlowResult = LoadFlow.find("OpenLoadFlow")
                .run(network, loadFlowParameters);

        if (loadFlowResult.isFailed()) {
            LOGGER.info(String.format("Initial loadflow for state %s is %s because %s", state.getId(), loadFlowResult.getStatus(), loadFlowResult.getComponentResults().get(0).getStatus().toString()));
        } else {
            LOGGER.info(String.format("Initial loadflow for state %s is %s", state.getId(), loadFlowResult.getStatus()));
        }

        return loadFlowResult.isFailed();
    }

    private boolean runLoadFlowWithRegulation(final Network network, final State state) {
        loadFlowParameters.setPhaseShifterRegulationOn(true);
        final LoadFlowResult loadFlowResult = LoadFlow.find("OpenLoadFlow")
                .run(network, loadFlowParameters);

        if (loadFlowResult.isFailed()) {
            LOGGER.info(String.format("LoadFlow WITH regulation for state %s is %s because %s", state.getId(), loadFlowResult.getStatus(), loadFlowResult.getComponentResults().get(0).getStatus().toString()));
        } else {
            LOGGER.info(String.format("LoadFlow WITH regulation for state %s is %s", state.getId(), loadFlowResult.getStatus()));
        }

        return loadFlowResult.isFailed();
    }

    private void logInfo() {
        LOGGER.info("--------------------------------------------");
        taps.stream().forEach(pstInfo ->
                LOGGER.info(String.format("Pst %s for state %s : %s -> %s",
                        pstInfo.getNameOrId(),
                        pstInfo.getState().getId(),
                        pstInfo.getTapBefore(),
                        pstInfo.getTapAfter())));
        cnecMarginInfos.stream().forEach(cnecInfo ->
                LOGGER.info(String.format("%s  : raoResultMargin = %s %n margin before = %s A %n margin after = %s A",
                        cnecInfo.getCnec().getId(),
                        Math.round(100. * cnecInfo.getRaoResultMargin()) / 100.,
                        Math.round(100. * cnecInfo.getMarginBefore()) / 100.,
                        Math.round(100. * cnecInfo.getMarginAfter()) / 100.)));
    }

    private static final class TapInfo {
        State state;
        String nameOrId;
        String networkElement;
        Integer tapBefore;
        Integer tapAfter;

        TapInfo(final State state, final String networkElement, final String nameOrId, final Integer tapBefore) {
            this.state = state;
            this.networkElement = networkElement;
            this.nameOrId = nameOrId;
            this.tapBefore = tapBefore;
            this.tapAfter = tapBefore;
        }

        private void setTapAfter(final Integer tapAfter) {
            this.tapAfter = tapAfter;
        }

        public String getNameOrId() {
            return nameOrId;
        }

        public Integer getTapBefore() {
            return tapBefore;
        }

        public Integer getTapAfter() {
            return tapAfter;
        }

        public String getNetworkElement() {
            return networkElement;
        }

        public State getState() {
            return state;
        }

    }

    private static final class CnecMarginInfo {
        FlowCnec cnec;
        double marginBefore;
        double raoResultMargin;
        double marginAfter;

        public double getRaoResultMargin() {
            return raoResultMargin;
        }

        public double getMarginBefore() {
            return marginBefore;
        }

        public double getMarginAfter() {
            return marginAfter;
        }

        public FlowCnec getCnec() {
            return cnec;
        }

        private CnecMarginInfo(final FlowCnec cnec) {
            this.cnec = cnec;
        }

        public void setRaoResultMargin(final double raoResultMargin) {
            this.raoResultMargin = raoResultMargin;
        }

        public void setMarginBefore(final double marginBefore) {
            this.marginBefore = marginBefore;
        }

        public void setMarginAfter(final double marginAfter) {
            this.marginAfter = marginAfter;
        }
    }

    private void applyOptimalRemedialActions(final State state, final Network network, final RaoResult raoResult) {
        raoResult.getActivatedNetworkActionsDuringState(state)
                .forEach(na -> na.apply(network));
        raoResult.getActivatedRangeActionsDuringState(state)
                .forEach(ra -> ra.apply(network, raoResult.getOptimizedSetPointOnState(state, ra)));

        final Set<String> appliedRas = raoResult.getActivatedNetworkActionsDuringState(state).stream().map(Identifiable::getId).collect(Collectors.toSet());
        appliedRas.addAll(raoResult.getActivatedRangeActionsDuringState(state).stream().map(Identifiable::getId).collect(Collectors.toSet()));
        if (!appliedRas.isEmpty()) {
            final StringBuilder toPrint = new StringBuilder("Applied RAs for state " + state.getId() + ":");
            for (final String ra : appliedRas) {
                toPrint.append(" " + ra);
                LOGGER.info(toPrint.toString());
            }
        }
    }

    private void applyOptimalRemedialActionsOnContingencyState(final State state, final Network network, final Crac crac, final RaoResult raoResult) {
        if (state.getInstant().isCurative()) {
            final Optional<Contingency> contingency = state.getContingency();
            crac.getStates(contingency.orElseThrow()).forEach(contingencyState ->
                    applyOptimalRemedialActions(contingencyState, network, raoResult));
        } else {
            applyOptimalRemedialActions(state, network, raoResult);
        }
    }
}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.pstregulation;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.commons.logs.RaoBusinessLogs;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoPstRegulationParameters;
import com.powsybl.openrao.searchtreerao.castor.algorithm.Castor;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class PstRegulationTest {
    @Test
    void testPstRegulationWithSeveralContingencyScenarios() throws IOException {
        Network network = Network.read("4NodesSeries.uct", getClass().getResourceAsStream("/network/4NodesSeries.uct"));
        network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), "InitialScenario");
        network.getVariantManager().setWorkingVariant("InitialScenario");

        Crac crac = Crac.read("crac-for-regulation.json", getClass().getResourceAsStream("/crac/crac-for-regulation.json"), network);
        RaoResult raoResult = RaoResult.read(getClass().getResourceAsStream("/raoResult/raoResultPreRegulation.json"), crac);
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_ac_3pstsRegulation.json"), ReportNode.NO_OP);

        PstRangeAction pst12 = crac.getPstRangeAction("pstFr12");
        PstRangeAction pst34 = crac.getPstRangeAction("pstFr34");

        ListAppender<ILoggingEvent> listAppender = getBusinessLogs();
        List<ILoggingEvent> logsList = listAppender.list;

        RaoResult raoResultWithPstRegulation = PstRegulation.regulatePsts(network, crac, raoResult, raoParameters, ReportNode.NO_OP);
        List<String> logMessages = logsList.stream().map(ILoggingEvent::getFormattedMessage).sorted().toList();

        assertEquals("2 PST(s) to regulate: pstFr12, pstFr34", logMessages.get(0));
        assertEquals("3 contingency scenario(s) to regulate: Contingency FR 12, Contingency FR 23, Contingency FR 34", logMessages.get(1));

        // PST FR2-FR3 is only preventive so it cannot be regulated
        assertEquals("PST FFR1AA1  FFR2AA1  2 cannot be regulated as no PST range action was defined for it on instant preventive.", logMessages.get(5));

        // Contingency FR1-FR2
        assertEquals(-15, raoResultWithPstRegulation.getOptimizedTapOnState(crac.getState("Contingency FR 12", crac.getLastInstant()), pst12));
        assertEquals(-5, raoResultWithPstRegulation.getOptimizedTapOnState(crac.getState("Contingency FR 12", crac.getLastInstant()), pst34));
        assertEquals(
            "FlowCNEC 'cnecFr34PstCurative - Co12' of contingency scenario 'Contingency FR 12' is overloaded and is the most limiting element, " +
                "PST regulation has been triggered: pstFr12 (-10 -> -15), pstFr34 (0 -> -5)",
            logMessages.get(4)
        );

        // Contingency FR2-FR3
        assertEquals(-5, raoResultWithPstRegulation.getOptimizedTapOnState(crac.getState("Contingency FR 23", crac.getLastInstant()), pst12));
        assertEquals(-5, raoResultWithPstRegulation.getOptimizedTapOnState(crac.getState("Contingency FR 23", crac.getLastInstant()), pst34));
        assertEquals(
            "FlowCNEC 'cnecFr23PstCurative - Co23' of contingency scenario 'Contingency FR 23' is overloaded and is the most limiting element, " +
                "PST regulation has been triggered: pstFr12 (0 -> -5), pstFr34 (0 -> -5)",
            logMessages.get(3)
        );

        // Contingency FR3-FR4
        assertEquals(-5, raoResultWithPstRegulation.getOptimizedTapOnState(crac.getState("Contingency FR 34", crac.getLastInstant()), pst12));
        assertEquals(-15, raoResultWithPstRegulation.getOptimizedTapOnState(crac.getState("Contingency FR 34", crac.getLastInstant()), pst34));
        assertEquals(
            "FlowCNEC 'cnecFr12PstCurative - Co34' of contingency scenario 'Contingency FR 34' is overloaded and is the most limiting element, " +
                "PST regulation has been triggered: pstFr12 (0 -> -5)",
            logMessages.get(2)
        );
    }

    private static ListAppender<ILoggingEvent> getBusinessLogs() {
        Logger logger = (Logger) LoggerFactory.getLogger(RaoBusinessLogs.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        return listAppender;
    }

    @Test
    void testPstRegulationAtTheEndOfRao() throws IOException {
        final Network network = Network.read("2Nodes3ParallelLinesPST.uct", getClass().getResourceAsStream("/network/2Nodes3ParallelLinesPST.uct"));
        final Crac crac = Crac.read("crac-regulation-1-PST.json", getClass().getResourceAsStream("/crac/crac-regulation-1-PST.json"), network);
        final RaoInput raoInput = RaoInput.build(network, crac).build();
        final RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_minMargin_ac.json"), ReportNode.NO_OP);

        final Instant curativeInstant = crac.getInstant(InstantKind.CURATIVE);
        final State curativeState = crac.getState("Contingency BE1 FR1 3", curativeInstant);

        final PstRangeAction pstRangeAction = crac.getPstRangeAction("pstBeFr2");
        final FlowCnec curativeCnecOnLine = crac.getFlowCnec("cnecBeFr1Curative");
        final FlowCnec curativeCnecOnPst = crac.getFlowCnec("cnecBeFr2Curative");

        // first run without regulation: min margin is maximized by setting PST on tap -2 even though PST is overloaded
        // but not seen by the RAO because it has no associated FlowCNEC
        final RaoResult raoResult = new Castor().run(raoInput, raoParameters, null, ReportNode.NO_OP).join();
        assertEquals(690.23, raoResult.getCost(crac.getLastInstant()), 1e-2);
        assertEquals(-2, raoResult.getOptimizedTapOnState(curativeState, pstRangeAction));
        assertEquals(-676.38, raoResult.getMargin(curativeInstant, curativeCnecOnLine, Unit.AMPERE), 1e-2);
        assertEquals(-690.23, raoResult.getMargin(curativeInstant, curativeCnecOnPst, Unit.AMPERE), 1e-2);

        // second run with regulation: regulation shifts PST's tap to position 7 to remove the overload but worsens min margin
        final SearchTreeRaoPstRegulationParameters pstRegulationParameters = new SearchTreeRaoPstRegulationParameters();
        pstRegulationParameters.setPstsToRegulate(Map.of("BBE1AA1  FFR1AA1  2", "BBE1AA1  FFR1AA1  2"));
        raoParameters.getExtension(OpenRaoSearchTreeParameters.class).setPstRegulationParameters(pstRegulationParameters);

        final RaoResult raoResultWithRegulation = PstRegulation.regulatePsts(network, crac, raoResult, raoParameters, ReportNode.NO_OP);
        assertEquals(1382.77, raoResultWithRegulation.getCost(crac.getLastInstant()), 1e-2);
        assertEquals(7, raoResultWithRegulation.getOptimizedTapOnState(curativeState, pstRangeAction));
        assertEquals(-1382.77, raoResultWithRegulation.getMargin(curativeInstant, curativeCnecOnLine, Unit.AMPERE), 1e-2);
        assertEquals(15.49, raoResultWithRegulation.getMargin(curativeInstant, curativeCnecOnPst, Unit.AMPERE), 1e-2);
    }

    @Test
    void testPreventiveRegulationResultsPropagateToCurativeScenarios() throws IOException {
        final Network network = Network.read("2Nodes4ParallelLines2PSTs.uct", getClass().getResourceAsStream("/network/2Nodes4ParallelLines2PSTs.uct"));
        final Crac crac = Crac.read("crac-regulation-preventive-only.json", getClass().getResourceAsStream("/crac/crac-regulation-preventive-only.json"), network);
        final RaoInput raoInput = RaoInput.build(network, crac).build();
        final RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_ac_1pstRegulation.json"), ReportNode.NO_OP);

        Instant preventiveInstant = crac.getPreventiveInstant();
        Instant curativeInstant = crac.getLastInstant();
        State preventiveState = crac.getPreventiveState();
        State curativeState = crac.getState("Contingency BE1 FR1 3", crac.getLastInstant());
        PstRangeAction pstRangeAction = crac.getPstRangeAction("pstBeFr2");
        FlowCnec preventiveCnecOnLine = crac.getFlowCnec("cnecBeFr1Preventive");
        FlowCnec preventiveCnecOnPst = crac.getFlowCnec("cnecBeFr2Preventive");
        FlowCnec curativeCnecOnLine = crac.getFlowCnec("cnecBeFr1Curative");
        FlowCnec curativeCnecOnPst = crac.getFlowCnec("cnecBeFr2Curative");

        // run RAO
        final RaoResult raoResult = new Castor().run(raoInput, raoParameters, null, ReportNode.NO_OP).join();

        // check initial results
        assertEquals(43.08, raoResult.getCost(crac.getLastInstant()), 1e-2);

        assertEquals(1, raoResult.getOptimizedTapOnState(preventiveState, pstRangeAction));
        assertEquals(-40.11, raoResult.getMargin(preventiveInstant, preventiveCnecOnLine, Unit.AMPERE), 1e-2);
        assertEquals(-43.08, raoResult.getMargin(preventiveInstant, preventiveCnecOnPst, Unit.AMPERE), 1e-2);

        assertEquals(366.50, raoResult.getMargin(curativeInstant, curativeCnecOnLine, Unit.AMPERE), 1e-2);
        assertEquals(823.54, raoResult.getMargin(curativeInstant, curativeCnecOnPst, Unit.AMPERE), 1e-2);

        // run PST regulation -> PST will be pushed up to tap position 2
        final RaoResult raoResultWithRegulation = PstRegulation.regulatePsts(network, crac, raoResult, raoParameters, ReportNode.NO_OP);

        assertEquals(79.38, raoResultWithRegulation.getCost(crac.getLastInstant()), 1e-2);

        assertEquals(2, raoResultWithRegulation.getOptimizedTapOnState(preventiveState, pstRangeAction));
        assertEquals(-79.38, raoResultWithRegulation.getMargin(preventiveInstant, preventiveCnecOnLine, Unit.AMPERE), 1e-2);
        assertEquals(74.69, raoResultWithRegulation.getMargin(preventiveInstant, preventiveCnecOnPst, Unit.AMPERE), 1e-2);

        // check that preventive regulated tap is propagated to curative
        assertEquals(2, raoResultWithRegulation.getOptimizedTapOnState(curativeState, pstRangeAction));
        assertEquals(314.16, raoResultWithRegulation.getMargin(curativeInstant, curativeCnecOnLine, Unit.AMPERE), 1e-2);
        assertEquals(928.22, raoResultWithRegulation.getMargin(curativeInstant, curativeCnecOnPst, Unit.AMPERE), 1e-2);
    }

    @Test
    void testPreventiveRegulationTriggersCurativeRegulation() throws IOException {
        final Network network = Network.read("2Nodes4ParallelLines2PSTs.uct", getClass().getResourceAsStream("/network/2Nodes4ParallelLines2PSTs.uct"));
        final Crac crac = Crac.read("crac-regulation-preventive-and-curative.json", getClass().getResourceAsStream("/crac/crac-regulation-preventive-and-curative.json"), network);
        final RaoInput raoInput = RaoInput.build(network, crac).build();
        final RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_ac_2pstRegulation.json"), ReportNode.NO_OP);

        Instant preventiveInstant = crac.getPreventiveInstant();
        Instant curativeInstant = crac.getLastInstant();
        State preventiveState = crac.getPreventiveState();
        State curativeState = crac.getState("Contingency BE1 FR1 3", crac.getLastInstant());
        PstRangeAction pst2RangeAction = crac.getPstRangeAction("pstBeFr2");
        PstRangeAction pst4RangeAction = crac.getPstRangeAction("pstBeFr4");
        FlowCnec preventiveCnecOnLine = crac.getFlowCnec("cnecBeFr1Preventive");
        FlowCnec preventiveCnecOnPst2 = crac.getFlowCnec("cnecBeFr2Preventive");
        FlowCnec curativeCnecOnLine = crac.getFlowCnec("cnecBeFr1Curative");
        FlowCnec curativeCnecOnPst2 = crac.getFlowCnec("cnecBeFr2Curative");
        FlowCnec curativeCnecOnPst4 = crac.getFlowCnec("cnecBeFr4Curative");

        // run RAO
        final RaoResult raoResult = new Castor().run(raoInput, raoParameters, null, ReportNode.NO_OP).join();

        // check initial results
        assertEquals(43.08, raoResult.getCost(crac.getLastInstant()), 1e-2);

        assertEquals(1, raoResult.getOptimizedTapOnState(preventiveState, pst2RangeAction));
        assertEquals(-40.11, raoResult.getMargin(preventiveInstant, preventiveCnecOnLine, Unit.AMPERE), 1e-2);
        assertEquals(-43.08, raoResult.getMargin(preventiveInstant, preventiveCnecOnPst2, Unit.AMPERE), 1e-2);

        assertEquals(0, raoResult.getOptimizedTapOnState(curativeState, pst4RangeAction));
        assertEquals(366.50, raoResult.getMargin(curativeInstant, curativeCnecOnLine, Unit.AMPERE), 1e-2);
        assertEquals(823.54, raoResult.getMargin(curativeInstant, curativeCnecOnPst2, Unit.AMPERE), 1e-2);
        assertEquals(16.51, raoResult.getMargin(curativeInstant, curativeCnecOnPst4, Unit.AMPERE), 1e-2);

        // run PST regulation -> PST will be pushed up to tap position 2 making second PST overloaded in curative
        final RaoResult raoResultWithRegulation = PstRegulation.regulatePsts(network, crac, raoResult, raoParameters, ReportNode.NO_OP);

        assertEquals(79.38, raoResultWithRegulation.getCost(crac.getLastInstant()), 1e-2);

        assertEquals(2, raoResultWithRegulation.getOptimizedTapOnState(preventiveState, pst2RangeAction));
        assertEquals(-79.38, raoResultWithRegulation.getMargin(preventiveInstant, preventiveCnecOnLine, Unit.AMPERE), 1e-2);
        assertEquals(74.69, raoResultWithRegulation.getMargin(preventiveInstant, preventiveCnecOnPst2, Unit.AMPERE), 1e-2);

        // check that the margin on the PST4 FlowCNEC is now negative, which requires curative regulation
        assertEquals(-35.83, raoResultWithRegulation.getMargin(preventiveInstant, curativeCnecOnPst4, Unit.AMPERE), 1e-2);

        // check that preventive regulated tap is propagated to curative and that curative PST is regulated to tap position 1
        assertEquals(2, raoResultWithRegulation.getOptimizedTapOnState(curativeState, pst2RangeAction));
        assertEquals(1, raoResultWithRegulation.getOptimizedTapOnState(curativeState, pst4RangeAction));
        assertEquals(261.82, raoResultWithRegulation.getMargin(curativeInstant, curativeCnecOnLine, Unit.AMPERE), 1e-2);
        assertEquals(875.88, raoResultWithRegulation.getMargin(curativeInstant, curativeCnecOnPst2, Unit.AMPERE), 1e-2);
        assertEquals(68.85, raoResultWithRegulation.getMargin(curativeInstant, curativeCnecOnPst4, Unit.AMPERE), 1e-2);
    }

    // TODO: [note for future test] if PST4's PATL is set to 550 A instead of 500 A, regulation does not change the curative tap -> investigate investigate if deadband problem
}

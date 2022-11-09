/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy.shift;

import com.farao_community.farao.commons.EICode;
import com.farao_community.farao.dichotomy.api.NetworkShifter;
import com.farao_community.farao.dichotomy.api.exceptions.GlskLimitationException;
import com.farao_community.farao.dichotomy.api.exceptions.ShiftingException;
import com.farao_community.farao.dichotomy.shift.ShiftDispatcher;
import com.farao_community.farao.swe.runner.api.exception.SweInvalidDataException;
import com.farao_community.farao.swe.runner.api.resource.ProcessType;
import com.farao_community.farao.swe.runner.app.dichotomy.DichotomyDirection;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public final class SweNetworkShifter implements NetworkShifter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SweNetworkShifter.class);
    private static final double DEFAULT_SHIFT_EPSILON = 1;
    private static final int MAX_NUMBER_ITERATION = 4;
    private final Logger businessLogger;

    private final ProcessType processType;
    private final DichotomyDirection direction;
    private final ZonalData<Scalable> zonalScalable;
    private final ShiftDispatcher shiftDispatcher;
    private final double toleranceEsPt;
    private final double toleranceEsFr;

    public SweNetworkShifter(Logger businessLogger, ProcessType processType, DichotomyDirection direction, ZonalData<Scalable> zonalScalable, ShiftDispatcher shiftDispatcher, double toleranceEsPt, double toleranceEsFr) {
        this.businessLogger = businessLogger;
        this.processType = processType;
        this.direction = direction;
        this.zonalScalable = zonalScalable;
        this.shiftDispatcher = shiftDispatcher;
        this.toleranceEsPt = toleranceEsPt;
        this.toleranceEsFr = toleranceEsFr;
    }

    @Override
    public void shiftNetwork(double stepValue, Network network) throws GlskLimitationException, ShiftingException {
        businessLogger.info(String.format("[%s] : Starting shift on network %s", direction,
                network.getVariantManager().getWorkingVariantId()));

        Map<String, Double> scalingValuesByCountry = shiftDispatcher.dispatch(stepValue);
        businessLogger.info(String.format("[%s] : Target shift on countries %s", direction, scalingValuesByCountry));
        Map<String, Double> targetExchanges = getTargetExchanges(stepValue);
        int iterationCounter = 0;
        boolean shiftSucceed = false;

        String initialVariantId = network.getVariantManager().getWorkingVariantId();
        String workingVariantCopyId = initialVariantId + " COPY";
        network.getVariantManager().cloneVariant(initialVariantId, workingVariantCopyId);
        network.getVariantManager().setWorkingVariant(workingVariantCopyId);

        List<String> limitingCountries = new ArrayList<>();
        Map<String, Double> bordersExchanges;

        do {
            // Step 1: Perform the scaling
            LOGGER.info(String.format("[%s] : Applying shift iteration %s ", direction, iterationCounter));
            for (Map.Entry<String, Double> entry : scalingValuesByCountry.entrySet()) {
                String zoneId = entry.getKey();
                double asked = entry.getValue();
                LOGGER.info(String.format("[%s] : Applying variation on zone %s (target: %.2f)", direction, zoneId, asked));
                double done = zonalScalable.getData(zoneId).scale(network, asked);
                if (Math.abs(done - asked) > DEFAULT_SHIFT_EPSILON) {
                    LOGGER.warn(String.format("[%s] : Incomplete variation on zone %s (target: %.2f, done: %.2f)",
                            direction, zoneId, asked, done));
                    limitingCountries.add(zoneId);
                }
            }
            if (!limitingCountries.isEmpty()) {
                StringJoiner sj = new StringJoiner(", ", "There are Glsk limitation(s) in ", ".");
                limitingCountries.forEach(sj::add);
                LOGGER.error("[{}] : {}", direction, sj);
                throw new GlskLimitationException(sj.toString());
            }

            // Step 2: Compute exchanges mismatch
            LoadFlowResult result = LoadFlow.run(network, workingVariantCopyId, LocalComputationManager.getDefault(), LoadFlowParameters.load());
            if (!result.isOk()) {
                LOGGER.error("Loadflow computation diverged on network '{}'", network.getId());
                throw new ShiftingException(String.format("Loadflow computation diverged on network %s", network.getId()));
            }
            bordersExchanges = CountryBalanceComputation.computeSweBordersExchanges(network);
            double mismatchEsPt = targetExchanges.get("ES_PT") - bordersExchanges.get("ES_PT");
            double mismatchEsFr = targetExchanges.get("ES_FR") -  bordersExchanges.get("ES_FR");

            // Step 3: Checks balance adjustment results
            if (Math.abs(mismatchEsPt) < toleranceEsPt && Math.abs(mismatchEsFr) < toleranceEsFr) {
                LOGGER.info(String.format("[%s] : Shift succeed after %s iteration ", direction, ++iterationCounter));
                businessLogger.info(String.format("[%s] : Shift succeed after %s iteration ", direction, ++iterationCounter));
                network.getVariantManager().cloneVariant(workingVariantCopyId, initialVariantId, true);
                shiftSucceed = true;
            } else {
                // Reset current variant with initial state for each iteration
                network.getVariantManager().cloneVariant(initialVariantId, workingVariantCopyId, true);

                scalingValuesByCountry.put(toEic("PT"), scalingValuesByCountry.get(toEic("PT")) - mismatchEsPt);
                scalingValuesByCountry.put(toEic("FR"), scalingValuesByCountry.get(toEic("FR")) - mismatchEsFr);
                scalingValuesByCountry.put(toEic("ES"), scalingValuesByCountry.get(toEic("ES")) + mismatchEsPt + mismatchEsFr);
                ++iterationCounter;
            }

        } while (iterationCounter < MAX_NUMBER_ITERATION && !shiftSucceed);

        // Step 4 : check after iteration max and out of tolerane
        if (!shiftSucceed) {
            String message = String.format("Balancing adjustment out of tolerances : Exchange ES-PT = %.2f , Exchange ES-FR =  %.2f", bordersExchanges.get("ES_PT"), bordersExchanges.get("ES_FR"));
            throw new ShiftingException(message); //todo check rule  "out of tolerance" with PO
        }

        // Step 5: Reset current variant with initial state
        network.getVariantManager().setWorkingVariant(initialVariantId);
        network.getVariantManager().removeVariant(workingVariantCopyId);
    }

    private Map<String, Double> getTargetExchanges(double stepValue) {
        Map<String, Double> target = new HashMap<>();
        switch (processType) {
            case D2CC:
                if (DichotomyDirection.ES_FR.equals(direction)) {
                    target.put("ES_PT", 0.);
                    target.put("ES_FR", stepValue);
                } else if (DichotomyDirection.FR_ES.equals(direction)) {
                    target.put("ES_PT", 0.);
                    target.put("ES_FR", -stepValue);
                } else if (DichotomyDirection.ES_PT.equals(direction)) {
                    target.put("ES_PT", stepValue);
                    target.put("ES_FR", 0.);
                } else if (DichotomyDirection.PT_ES.equals(direction)) {
                    target.put("ES_PT", -stepValue);
                    target.put("ES_FR", 0.);
                }
                return target;
            case IDCC:
                // todo
                return target;
            default:
                throw new SweInvalidDataException(String.format("Unknown target process for SWE: %s", processType));
        }
    }

    private static String toEic(String country) {
        return new EICode(Country.valueOf(country)).getAreaCode();
    }
}

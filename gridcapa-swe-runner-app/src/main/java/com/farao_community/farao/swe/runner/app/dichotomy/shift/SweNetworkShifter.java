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
import com.farao_community.farao.swe.runner.api.resource.ProcessType;
import com.farao_community.farao.swe.runner.app.configurations.ProcessConfiguration;
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
    public static final String ES_PT = "ES_PT";
    public static final String ES_FR = "ES_FR";
    private final Logger businessLogger;

    private final ProcessType processType;
    private final DichotomyDirection direction;
    private final ZonalData<Scalable> zonalScalable;
    private final ShiftDispatcher shiftDispatcher;
    private final double toleranceEsPt;
    private final double toleranceEsFr;
    private final Map<String, Double> initialNetPositions;
    private final ProcessConfiguration processConfiguration;

    public SweNetworkShifter(Logger businessLogger, ProcessType processType, DichotomyDirection direction, ZonalData<Scalable> zonalScalable, ShiftDispatcher shiftDispatcher, double toleranceEsPt, double toleranceEsFr, Map<String, Double> initialNetPositions, ProcessConfiguration processConfiguration) {
        this.businessLogger = businessLogger;
        this.processType = processType;
        this.direction = direction;
        this.zonalScalable = zonalScalable;
        this.shiftDispatcher = shiftDispatcher;
        this.toleranceEsPt = toleranceEsPt;
        this.toleranceEsFr = toleranceEsFr;
        this.initialNetPositions = initialNetPositions;
        this.processConfiguration = processConfiguration;
    }

    @Override
    public void shiftNetwork(double stepValue, Network network) throws GlskLimitationException, ShiftingException {
        businessLogger.info("Starting shift on network {}", network.getVariantManager().getWorkingVariantId());
        Map<String, Double> scalingValuesByCountry = shiftDispatcher.dispatch(stepValue);
        String logTargetCountriesShift = String.format("Target countries shift [ES = %.2f, FR = %.2f, PT = %.2f]", scalingValuesByCountry.get(toEic("ES")), scalingValuesByCountry.get(toEic("FR")), scalingValuesByCountry.get(toEic("PT")));
        businessLogger.info(logTargetCountriesShift);
        Map<String, Double> targetExchanges = getTargetExchanges(stepValue);
        int iterationCounter = 0;
        boolean shiftSucceed = false;

        String initialVariantId = network.getVariantManager().getWorkingVariantId();
        String workingVariantCopyId = initialVariantId + " COPY";
        network.getVariantManager().cloneVariant(initialVariantId, workingVariantCopyId);
        network.getVariantManager().setWorkingVariant(workingVariantCopyId);

        List<String> limitingCountries = new ArrayList<>();
        Map<String, Double> bordersExchanges;

        int maxIterationNumber = processConfiguration.getShiftMaxIterationNumber();
        do {
            // Step 1: Perform the scaling
            LOGGER.info("[{}] : Applying shift iteration {} ", direction, iterationCounter);
            for (Map.Entry<String, Double> entry : scalingValuesByCountry.entrySet()) {
                String zoneId = entry.getKey();
                double asked = entry.getValue();
                String logApplyingVariationOnZone = String.format("[%s] : Applying variation on zone %s (target: %.2f)", direction, zoneId, asked);
                LOGGER.info(logApplyingVariationOnZone);
                double done = zonalScalable.getData(zoneId).scale(network, asked);
                if (Math.abs(done - asked) > DEFAULT_SHIFT_EPSILON) {
                    String logWarnIncompleteVariation = String.format("[%s] : Incomplete variation on zone %s (target: %.2f, done: %.2f)",
                            direction, zoneId, asked, done);
                    LOGGER.warn(logWarnIncompleteVariation);
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
                throw new ShiftingException("Loadflow computation diverged during balancing adjustment");
            }
            bordersExchanges = CountryBalanceComputation.computeSweBordersExchanges(network);
            double mismatchEsPt = targetExchanges.get(ES_PT) - bordersExchanges.get(ES_PT);
            double mismatchEsFr = targetExchanges.get(ES_FR) - bordersExchanges.get(ES_FR);

            // Step 3: Checks balance adjustment results
            if (Math.abs(mismatchEsPt) < toleranceEsPt && Math.abs(mismatchEsFr) < toleranceEsFr) {
                String logShiftSucceded = String.format("[%s] : Shift succeed after %s iteration ", direction, ++iterationCounter);
                LOGGER.info(logShiftSucceded);
                businessLogger.info("Shift succeed after {} iteration ", ++iterationCounter);
                String msg = String.format("Exchange ES-PT = %.2f , Exchange ES-FR =  %.2f", bordersExchanges.get(ES_PT), bordersExchanges.get(ES_FR));
                businessLogger.info(msg);
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

        } while (iterationCounter < maxIterationNumber && !shiftSucceed);

        // Step 4 : check after iteration max and out of tolerane
        if (!shiftSucceed) {
            String message = String.format("Balancing adjustment out of tolerances : Exchange ES-PT = %.2f , Exchange ES-FR =  %.2f", bordersExchanges.get(ES_PT), bordersExchanges.get(ES_FR));
            businessLogger.error(message);
            throw new ShiftingException(message);
        }

        // Step 5: Reset current variant with initial state
        network.getVariantManager().setWorkingVariant(initialVariantId);
        network.getVariantManager().removeVariant(workingVariantCopyId);
    }

    Map<String, Double> getTargetExchanges(double stepValue) {
        return processType.equals(ProcessType.IDCC) ? getIdccTargetExchanges(stepValue, initialNetPositions) : getD2ccTargetExchanges(stepValue);
    }

    private Map<String, Double> getIdccTargetExchanges(double stepValue, Map<String, Double> initialNetPositions) {
        Map<String, Double> target = new HashMap<>();
        if (DichotomyDirection.ES_FR.equals(direction)) {
            target.put(ES_PT, -initialNetPositions.get(toEic("PT")));
            target.put(ES_FR, stepValue);
        } else if (DichotomyDirection.FR_ES.equals(direction)) {
            target.put(ES_PT, -initialNetPositions.get(toEic("PT")));
            target.put(ES_FR, -stepValue);
        } else if (DichotomyDirection.ES_PT.equals(direction)) {
            target.put(ES_PT, stepValue);
            target.put(ES_FR, initialNetPositions.get(toEic("ES")) + initialNetPositions.get(toEic("PT")));
        } else if (DichotomyDirection.PT_ES.equals(direction)) {
            target.put(ES_PT, -stepValue);
            target.put(ES_FR, initialNetPositions.get(toEic("ES")) + initialNetPositions.get(toEic("PT")));
        }
        return target;
    }

    private Map<String, Double> getD2ccTargetExchanges(double stepValue) {
        Map<String, Double> target = new HashMap<>();
        if (DichotomyDirection.ES_FR.equals(direction)) {
            target.put(ES_PT, 0.);
            target.put(ES_FR, stepValue);
        } else if (DichotomyDirection.FR_ES.equals(direction)) {
            target.put(ES_PT, 0.);
            target.put(ES_FR, -stepValue);
        } else if (DichotomyDirection.ES_PT.equals(direction)) {
            target.put(ES_PT, stepValue);
            target.put(ES_FR, 0.);
        } else if (DichotomyDirection.PT_ES.equals(direction)) {
            target.put(ES_PT, -stepValue);
            target.put(ES_FR, 0.);
        }
        return target;
    }

    private static String toEic(String country) {
        return new EICode(Country.valueOf(country)).getAreaCode();
    }
}

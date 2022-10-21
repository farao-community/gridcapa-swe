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
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;

import java.util.*;

import static com.farao_community.farao.dichotomy.api.logging.DichotomyLoggerProvider.BUSINESS_LOGS;
import static com.farao_community.farao.dichotomy.api.logging.DichotomyLoggerProvider.BUSINESS_WARNS;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public final class SweNetworkShifter implements NetworkShifter {
    private static final double DEFAULT_SHIFT_EPSILON = 1;
    private static final int MAX_NUMBER_ITERATION = 4;

    private final ZonalData<Scalable> zonalScalable;
    private final ShiftDispatcher shiftDispatcher;
    private final double toleranceEsPt;
    private final double toleranceEsFr;

    public SweNetworkShifter(ZonalData<Scalable> zonalScalable, ShiftDispatcher shiftDispatcher, double toleranceEsPt, double toleranceEsFr) {
        this.zonalScalable = zonalScalable;
        this.shiftDispatcher = shiftDispatcher;
        this.toleranceEsPt = toleranceEsPt;
        this.toleranceEsFr = toleranceEsFr;
    }

    @Override
    public void shiftNetwork(double stepValue, Network network) throws GlskLimitationException, ShiftingException {
        BUSINESS_LOGS.info(String.format("Starting shift on network %s with step value %.2f",
                network.getVariantManager().getWorkingVariantId(), stepValue)); //todo add direction to logs
        Map<String, Double> scalingValuesByCountry = shiftDispatcher.dispatch(stepValue);

        int iterationCounter = 0;
        boolean shiftSucceed = false;
        String initialVariantId = network.getVariantManager().getWorkingVariantId();

        String workingStateId = network.getVariantManager().getWorkingVariantId();
        String workingVariantCopyId = workingStateId + " COPY";
        network.getVariantManager().cloneVariant(workingStateId, workingVariantCopyId);
        network.getVariantManager().setWorkingVariant(workingVariantCopyId);

        List<String> limitingCountries = new ArrayList<>();
        Map<String, Double> bordersExchanges;

        do {
            // Step 1: Perform the scaling
            BUSINESS_LOGS.info(String.format("Applying shift iteration %s ", iterationCounter));
            for (Map.Entry<String, Double> entry : scalingValuesByCountry.entrySet()) {
                String zoneId = entry.getKey();
                double asked = entry.getValue();
                BUSINESS_LOGS.info(String.format("Applying variation on zone %s (target: %.2f)", zoneId, asked));
                double done = zonalScalable.getData(zoneId).scale(network, asked);
                if (Math.abs(done - asked) > DEFAULT_SHIFT_EPSILON) {
                    BUSINESS_WARNS.warn(String.format("Incomplete variation on zone %s (target: %.2f, done: %.2f)",
                            zoneId, asked, done));
                    limitingCountries.add(zoneId);
                }
            }
            if (!limitingCountries.isEmpty()) {
                StringJoiner sj = new StringJoiner(", ", "There are Glsk limitation(s) in ", ".");
                limitingCountries.forEach(sj::add);
                throw new GlskLimitationException(sj.toString());
            }

            // Step 2: Compute exchanges mismatch
            bordersExchanges = CountryBalanceComputation.computeSweBordersExchanges(network, workingVariantCopyId);
            Double targetExchangeEsPt = 0.;
            Double targetExchangeEsFr = stepValue;
            double mismatchEsPt = targetExchangeEsPt - bordersExchanges.get("ES_PT");
            double mismatchEsFr = targetExchangeEsFr -  bordersExchanges.get("ES_FR");

            // Step 3: Checks balance adjustment results
            if (Math.abs(mismatchEsPt) < toleranceEsPt && Math.abs(mismatchEsFr) < toleranceEsFr) {
                BUSINESS_LOGS.info(String.format("Shift succeed with %s iteration ", ++iterationCounter));
                network.getVariantManager().cloneVariant(workingVariantCopyId, workingStateId, true);
                shiftSucceed = true;
            } else {
                // Reset current variant with initial state for each iteration
                network.getVariantManager().cloneVariant(workingStateId, workingVariantCopyId, true);

                scalingValuesByCountry.put(toEic("PT"), scalingValuesByCountry.get(toEic("PT")) - mismatchEsPt);
                scalingValuesByCountry.put(toEic("FR"), scalingValuesByCountry.get(toEic("FR")) - mismatchEsFr);
                scalingValuesByCountry.put(toEic("ES"), scalingValuesByCountry.get(toEic("ES")) + mismatchEsPt + mismatchEsFr);
                ++iterationCounter;
            }

        } while (iterationCounter < MAX_NUMBER_ITERATION && !shiftSucceed);

        // Step 4 : check after iteration max and out of tolerane
        if (!shiftSucceed) {
            String message = String.format("Balancing adjustment out of tolerances : Exchange ES-PT = %.2f , Exchange ES-FR =  %.2f", bordersExchanges.get("ES_PT"), bordersExchanges.get("ES_FR"));
            throw new GlskLimitationException(message); //todo check rule  "out of tolerance" with PO
        }

        // Step 5: Reset current variant with initial state
        network.getVariantManager().removeVariant(workingVariantCopyId);
        network.getVariantManager().setWorkingVariant(initialVariantId);
    }

    private static String toEic(String country) {
        return new EICode(Country.valueOf(country)).getAreaCode();
    }
}

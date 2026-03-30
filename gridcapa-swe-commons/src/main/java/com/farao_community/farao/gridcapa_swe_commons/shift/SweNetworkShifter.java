/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_swe_commons.shift;

import com.farao_community.farao.dichotomy.api.NetworkShifter;
import com.farao_community.farao.dichotomy.api.exceptions.GlskLimitationException;
import com.farao_community.farao.dichotomy.api.exceptions.ShiftingException;
import com.farao_community.farao.dichotomy.api.results.ReasonInvalid;
import com.farao_community.farao.dichotomy.shift.ShiftDispatcher;
import com.farao_community.farao.gridcapa_swe_commons.configuration.ProcessConfiguration;
import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import com.farao_community.farao.gridcapa_swe_commons.resource.ProcessType;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.modification.scalable.ScalingParameters;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManager;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.farao_community.farao.gridcapa_swe_commons.loadflow.LoadFlowUtil.runLoadFlowWithMdc;
import static com.farao_community.farao.gridcapa_swe_commons.resource.ProcessType.D2CC;
import static com.farao_community.farao.gridcapa_swe_commons.resource.SweEICode.ES_EIC;
import static com.farao_community.farao.gridcapa_swe_commons.resource.SweEICode.FR_EIC;
import static com.farao_community.farao.gridcapa_swe_commons.resource.SweEICode.PT_EIC;
import static com.farao_community.farao.gridcapa_swe_commons.shift.CountryBalanceComputation.computeSweBordersExchanges;
import static com.powsybl.iidm.modification.scalable.ScalingParameters.Priority.RESPECT_OF_VOLUME_ASKED;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public class SweNetworkShifter implements NetworkShifter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SweNetworkShifter.class);
    private static final double DEFAULT_SHIFT_EPSILON = 1;
    public static final String ES_PT = "ES_PT";
    public static final String ES_FR = "ES_FR";
    private static final Set<Country> PRE_PROCESSING_COUNTRIES = Set.of(Country.ES, Country.PT);
    private final Logger businessLogger;

    private final ProcessType processType;
    private final DichotomyDirection direction;
    private final ZonalData<Scalable> zonalScalable;
    private final ShiftDispatcher shiftDispatcher;
    private final double toleranceEsPt;
    private final double toleranceEsFr;
    private final Map<String, Double> initialNetPositions;
    private final ProcessConfiguration processConfiguration;
    private final LoadFlowParameters loadFlowParameters;
    private final NetworkExporter networkExporter;
    private final boolean runGlskChecksBeforeLoadFlow;

    public SweNetworkShifter(final Logger businessLogger,
                             final ProcessType processType,
                             final DichotomyDirection direction,
                             final ZonalData<Scalable> zonalScalable,
                             final ShiftDispatcher shiftDispatcher,
                             final double toleranceEsPt,
                             final double toleranceEsFr,
                             final Map<String, Double> initialNetPositions,
                             final ProcessConfiguration processConfiguration,
                             final LoadFlowParameters loadFlowParameters,
                             final NetworkExporter networkExporter,
                             final boolean runGlskChecksBeforeLoadFlow) { // NOSONAR
        this.businessLogger = businessLogger;
        this.processType = processType;
        this.direction = direction;
        this.zonalScalable = zonalScalable;
        this.shiftDispatcher = shiftDispatcher;
        this.toleranceEsPt = toleranceEsPt;
        this.toleranceEsFr = toleranceEsFr;
        this.initialNetPositions = initialNetPositions;
        this.processConfiguration = processConfiguration;
        this.loadFlowParameters = loadFlowParameters;
        this.networkExporter = networkExporter;
        this.runGlskChecksBeforeLoadFlow = runGlskChecksBeforeLoadFlow;
    }

    @Override
    public void shiftNetwork(final double stepValue,
                             final Network network) throws GlskLimitationException, ShiftingException {
        final VariantManager variantManager = network.getVariantManager();
        businessLogger.info("Starting shift on network {}", variantManager.getWorkingVariantId());
        final Map<String, Double> scalingValuesByCountry = shiftDispatcher.dispatch(stepValue);
        final ScalableGeneratorConnector scalableGeneratorConnector = new ScalableGeneratorConnector(zonalScalable);
        final GeneratorLimitsHandler generatorLimitsHandler = new GeneratorLimitsHandler(zonalScalable);

        try {
            final String logTargetCountriesShift = String.format("Target countries shift [ES = %.2f, FR = %.2f, PT = %.2f]",
                                                                 scalingValuesByCountry.get(ES_EIC), scalingValuesByCountry.get(FR_EIC), scalingValuesByCountry.get(PT_EIC));
            businessLogger.info(logTargetCountriesShift);
            final Map<String, Double> targetExchanges = getTargetExchanges(stepValue);
            int iterationCounter = 1;
            boolean shiftSucceeded = false;
            final String initialVariantId = variantManager.getWorkingVariantId();
            final String processedVariantId = initialVariantId + " PROCESSED COPY";
            final String workingVariantCopyId = initialVariantId + " WORKING COPY";
            preProcessNetwork(network,
                              scalableGeneratorConnector,
                              generatorLimitsHandler,
                              initialVariantId,
                              processedVariantId,
                              workingVariantCopyId);
            Map<String, Double> bordersExchanges;
            final int maxIterationNumber = processConfiguration.getShiftMaxIterationNumber();
            final ScalingParameters scalingParameters = getScalingParameters();

            do {
                // Step 1: Perform the scaling
                LOGGER.info("[{}] : Applying shift iteration {} ", direction, iterationCounter);
                final Map<String, Double> incompleteShiftCountries = shiftIteration(network,
                                                                                    scalingValuesByCountry,
                                                                                    scalingParameters,
                                                                                    scalableGeneratorConnector,
                                                                                    stepValue);
                // Step 2: Compute exchanges mismatch
                final LoadFlowResult result = runLoadFlowWithMdc(network, workingVariantCopyId, loadFlowParameters);
                if (result.isFailed()) {
                    LOGGER.error("Loadflow computation diverged on network '{}' for direction {}", network.getId(), direction.getDashName());
                    businessLogger.error("Loadflow computation diverged on network during balancing adjustment");
                    if (networkExporter != null) {
                        networkExporter.export(network);
                    }
                    throw new ShiftingException("Loadflow computation diverged during balancing adjustment", ReasonInvalid.BALANCE_LOADFLOW_DIVERGENCE);
                }

                bordersExchanges = computeSweBordersExchanges(network);
                final double mismatchEsPt = targetExchanges.get(ES_PT) - bordersExchanges.get(ES_PT);
                final double mismatchEsFr = targetExchanges.get(ES_FR) - bordersExchanges.get(ES_FR);

                // Step 3: Checks GLSK limitation and balance adjustment results

                if (runGlskChecksBeforeLoadFlow) {
                    checkGlskLimitation(incompleteShiftCountries, mismatchEsPt, mismatchEsFr);
                }
                if (hasShiftSucceeded(mismatchEsPt, mismatchEsFr)) {
                    logShiftSuccess(iterationCounter, bordersExchanges);
                    variantManager.cloneVariant(workingVariantCopyId, initialVariantId, true);
                    shiftSucceeded = true;
                } else {
                    if (!runGlskChecksBeforeLoadFlow) {
                        checkGlskLimitation(incompleteShiftCountries, mismatchEsPt, mismatchEsFr);
                    }
                    // Reset current variant with initial state for each iteration (keeping pre-processing)
                    variantManager.cloneVariant(processedVariantId, workingVariantCopyId, true);
                    updateScalingValuesWithMismatch(scalingValuesByCountry, mismatchEsPt, mismatchEsFr);
                    ++iterationCounter;
                }

            } while (iterationCounter < maxIterationNumber && !shiftSucceeded);

            // Step 4 : check after iteration max and out of tolerance
            if (!shiftSucceeded) {
                String message = String.format("Balancing adjustment out of tolerances : Exchange ES-PT = %.2f , Exchange ES-FR =  %.2f",
                                               bordersExchanges.get(ES_PT), bordersExchanges.get(ES_FR));
                businessLogger.error(message);
                throw new ShiftingException(message);
            }

            // Step 5: Reset current variant with initial state
            variantManager.setWorkingVariant(initialVariantId);
            variantManager.removeVariant(processedVariantId);
            variantManager.removeVariant(workingVariantCopyId);
        } finally {
            // here set working variant generators pmin and pmax values to initial values
            generatorLimitsHandler.resetInitialPminPmax(network);
        }
    }

    private void logShiftSuccess(final int iterationCounter,
                                 final Map<String, Double> bordersExchanges) {
        final String logShiftSucceeded = String.format("[%s] : Shift succeeded after %s iteration ",
                                                       direction, iterationCounter);
        LOGGER.info(logShiftSucceeded);
        businessLogger.info("Shift succeeded after {} iteration ", iterationCounter);
        final String info = String.format("Exchange ES-PT = %.2f , Exchange ES-FR =  %.2f",
                                          bordersExchanges.get(ES_PT), bordersExchanges.get(ES_FR));
        businessLogger.info(info);
    }

    private boolean hasShiftSucceeded(final double mismatchEsPt,
                                      final double mismatchEsFr) {
        return Math.abs(mismatchEsPt) < toleranceEsPt && Math.abs(mismatchEsFr) < toleranceEsFr;
    }

    private Map<String, Double> shiftIteration(final Network network,
                                               final Map<String, Double> scalingValuesByCountry,
                                               final ScalingParameters scalingParameters,
                                               final ScalableGeneratorConnector scalableGeneratorConnector,
                                               final double stepValue) throws GlskLimitationException {
        final Map<String, Double> incompleteShiftCountries = new HashMap<>();
        for (final Map.Entry<String, Double> entry : scalingValuesByCountry.entrySet()) {
            final String zoneId = entry.getKey();
            if (zonalScalable.getData(zoneId) != null) {
                double asked = entry.getValue();
                final String infoMessage = String.format("[%s] : Applying variation on zone %s (target: %.2f)", direction, zoneId, asked);
                LOGGER.info(infoMessage);
                double done = zonalScalable.getData(zoneId).scale(network, asked, scalingParameters);
                if (Math.abs(done - asked) > DEFAULT_SHIFT_EPSILON) {
                    final String warningMessage = String.format("[%s] : Incomplete shift on zone %s (target: %.2f, done: %.2f)",
                                                                      direction, zoneId, asked, done);
                    LOGGER.warn(warningMessage);
                    incompleteShiftCountries.put(zoneId, done - asked);
                }
            }
        }
        // During the shift some generators linked to the main network with a transformers are not connected correctly
        // Waiting for a fix in powsybl-core, we connect the transformers linked to these generators to be correctly connected to the main network component
        scalableGeneratorConnector.connectGeneratorsTransformers(network, PRE_PROCESSING_COUNTRIES);
        return incompleteShiftCountries;
    }

    private void checkGlskLimitation(final Map<String, Double> incompleteShiftCountries,
                                     final double mismatchEsPt,
                                     final double mismatchEsFr) throws GlskLimitationException {

        final Double ptDiff = incompleteShiftCountries.get(PT_EIC);
        final Double frDiff = incompleteShiftCountries.get(FR_EIC);
        final Double esDiff = incompleteShiftCountries.get(ES_EIC);

        if (ptDiff != null) {
            checkGlskLimitationCountry("PT", ptDiff, mismatchEsPt);
        }
        if (frDiff != null) {
            checkGlskLimitationCountry("FR", frDiff, mismatchEsFr);
        }
        if (esDiff != null) {
            checkGlskLimitationCountry("ES", esDiff, -(mismatchEsFr + mismatchEsPt));
        }
    }

    private void checkGlskLimitationCountry(final String country,
                                            final double diffShift,
                                            final double mismatch) throws GlskLimitationException {
        // In case of asked > 0 : (done - asked) will be < 0, we have Glsk limitation if the next asked value increase (mismatch < 0),
        // In case of asked < 0 : (done - asked) will be > 0, we have Glsk limitation if the next asked value decrease (mismatch > 0)
        if (diffShift < 0 && mismatch < 0 || diffShift > 0 && mismatch > 0) {
            final String errorMessage = "GLSK limitation occurred for country " + country;
            businessLogger.error(errorMessage);
            throw new GlskLimitationException(errorMessage);
        }
    }

    private void preProcessNetwork(final Network network,
                                   final ScalableGeneratorConnector scalableGeneratorConnector,
                                   final GeneratorLimitsHandler generatorLimitsHandler,
                                   final String initialVariantId,
                                   final String processedVariantId,
                                   final String workingVariantCopyId) throws ShiftingException {
        final VariantManager variantManager = network.getVariantManager();
        variantManager.cloneVariant(initialVariantId, processedVariantId, true);
        variantManager.setWorkingVariant(processedVariantId);
        scalableGeneratorConnector.fillGeneratorsInitialState(network, PRE_PROCESSING_COUNTRIES);
        // here set working variant generators pmin and pmax values to default values
        // so that glsk generator pmin and pmax values are used
        generatorLimitsHandler.setPminPmaxToDefaultValue(network, PRE_PROCESSING_COUNTRIES);
        variantManager.cloneVariant(processedVariantId, workingVariantCopyId, true);
        variantManager.setWorkingVariant(workingVariantCopyId);
    }

    private static ScalingParameters getScalingParameters() {
        final ScalingParameters scalingParameters = new ScalingParameters();
        // RESPECT_OF_VOLUME_ASKED: this parameter allows to do an iterative shift for proportional Glsk until achieving the asked value
        // if in the first iteration some generators was limited by maximum value, the missing power will be distributed to others generators in the next iteration
        scalingParameters.setPriority(RESPECT_OF_VOLUME_ASKED);
        // allow scaling to reconnect generators that are initially disconnected
        scalingParameters.setReconnect(true);
        return scalingParameters;
    }

    public void updateScalingValuesWithMismatch(final Map<String, Double> scalingValuesByCountry,
                                                final double mismatchEsPt,
                                                final double mismatchEsFr) {
        switch (direction) {
            case ES_FR, FR_ES -> scalingValuesByCountry.put(FR_EIC, scalingValuesByCountry.get(FR_EIC) - mismatchEsFr);
            case ES_PT, PT_ES -> scalingValuesByCountry.put(PT_EIC, scalingValuesByCountry.get(PT_EIC) - mismatchEsPt);
        }

        scalingValuesByCountry.put(ES_EIC, scalingValuesByCountry.get(ES_EIC) + mismatchEsPt + mismatchEsFr);
    }

    public Map<String, Double> getTargetExchanges(final double stepValue) {
        return processType == D2CC ? getDayAheadTargetExchanges(stepValue) : getIdccTargetExchanges(stepValue, initialNetPositions);
    }

    private Map<String, Double> getIdccTargetExchanges(final double stepValue,
                                                       final Map<String, Double> initialNetPositions) {
        return switch (direction) {
            case ES_FR -> Map.of(ES_PT, -initialNetPositions.get(PT_EIC), ES_FR, stepValue);
            case FR_ES -> Map.of(ES_PT, -initialNetPositions.get(PT_EIC), ES_FR, -stepValue);
            case ES_PT ->
                Map.of(ES_PT, stepValue, ES_FR, initialNetPositions.get(ES_EIC) + initialNetPositions.get(PT_EIC));
            case PT_ES ->
                Map.of(ES_PT, -stepValue, ES_FR, initialNetPositions.get(ES_EIC) + initialNetPositions.get(PT_EIC));
        };
    }

    private Map<String, Double> getDayAheadTargetExchanges(final double stepValue) {
        return switch (direction) {
            case ES_FR -> Map.of(ES_PT, 0., ES_FR, stepValue);
            case FR_ES -> Map.of(ES_PT, 0., ES_FR, -stepValue);
            case ES_PT -> Map.of(ES_PT, stepValue, ES_FR, 0.);
            case PT_ES -> Map.of(ES_PT, -stepValue, ES_FR, 0.);
        };
    }
}

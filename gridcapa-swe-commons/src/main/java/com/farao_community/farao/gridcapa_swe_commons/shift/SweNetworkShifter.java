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
import com.farao_community.farao.dichotomy.shift.ShiftDispatcher;
import com.farao_community.farao.gridcapa_swe_commons.configuration.ProcessConfiguration;
import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import com.farao_community.farao.gridcapa_swe_commons.resource.ProcessType;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.modification.scalable.ScalingParameters;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.openrao.commons.EICode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public class SweNetworkShifter implements NetworkShifter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SweNetworkShifter.class);
    private static final double DEFAULT_SHIFT_EPSILON = 1;
    public static final String ES_PT = "ES_PT";
    public static final String ES_FR = "ES_FR";
    private static final double DEFAULT_PMAX = 9999.0;
    private static final double DEFAULT_PMIN = -9999.0;
    private static final Set<Country> CONNECT_GENERATORS_IN_COUNTRIES = Set.of(Country.ES, Country.PT);
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
        // here set working variant generators pmin and pmax values to default values
        // so that glsk generator pmin and pmax values are used
        final Set<String> zoneIds = scalingValuesByCountry.keySet();
        Map<String, InitGenerator> initGenerators = setPminPmaxToDefaultValue(network, zonalScalable, zoneIds);
        ScalableGeneratorConnector scalableGeneratorConnector = new ScalableGeneratorConnector(zonalScalable);

        try {
            String logTargetCountriesShift = String.format("Target countries shift [ES = %.2f, FR = %.2f, PT = %.2f]", scalingValuesByCountry.get(toEic("ES")), scalingValuesByCountry.get(toEic("FR")), scalingValuesByCountry.get(toEic("PT")));
            businessLogger.info(logTargetCountriesShift);
            Map<String, Double> targetExchanges = getTargetExchanges(stepValue);
            int iterationCounter = 0;
            boolean shiftSucceed = false;

            String initialVariantId = network.getVariantManager().getWorkingVariantId();
            String processedVariantId = initialVariantId + " PROCESSED COPY";
            String workingVariantCopyId = initialVariantId + " WORKING COPY";
            preProcessNetwork(network, scalableGeneratorConnector, initialVariantId, processedVariantId, workingVariantCopyId);
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
                    ScalingParameters scalingParameters = new ScalingParameters();
                    scalingParameters.setIterative(true);
                    scalingParameters.setReconnect(true);
                    double done = zonalScalable.getData(zoneId).scale(network, asked, scalingParameters);
                    if (Math.abs(done - asked) > DEFAULT_SHIFT_EPSILON) {
                        String logWarnIncompleteVariation = String.format("[%s] : Incomplete variation on zone %s (target: %.2f, done: %.2f)",
                                direction, zoneId, asked, done);
                        LOGGER.warn(logWarnIncompleteVariation);
                        limitingCountries.add(zoneId);
                    }
                }
                scalableGeneratorConnector.connectGeneratorsTransformers(network, CONNECT_GENERATORS_IN_COUNTRIES);

                if (!limitingCountries.isEmpty()) {
                    StringJoiner sj = new StringJoiner(", ", "There are Glsk limitation(s) in ", ".");
                    limitingCountries.forEach(sj::add);
                    LOGGER.error("[{}] : {}", direction, sj);
                    throw new GlskLimitationException(sj.toString());
                }

                // Step 2: Compute exchanges mismatch
                LoadFlowResult result = LoadFlow.run(network, workingVariantCopyId, LocalComputationManager.getDefault(), LoadFlowParameters.load());
                if (!result.isOk()) {
                    LOGGER.error("Loadflow computation diverged on network '{}' for direction {}", network.getId(), direction.getDashName());
                    businessLogger.error("Loadflow computation diverged on network during balancing adjustment");
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
                    // Reset current variant with initial state for each iteration (keeping pre-processing)
                    network.getVariantManager().cloneVariant(processedVariantId, workingVariantCopyId, true);
                    updateScalingValuesWithMismatch(scalingValuesByCountry, mismatchEsPt, mismatchEsFr);
                    ++iterationCounter;
                }

            } while (iterationCounter < maxIterationNumber && !shiftSucceed);

            // Step 4 : check after iteration max and out of tolerance
            if (!shiftSucceed) {
                String message = String.format("Balancing adjustment out of tolerances : Exchange ES-PT = %.2f , Exchange ES-FR =  %.2f", bordersExchanges.get(ES_PT), bordersExchanges.get(ES_FR));
                businessLogger.error(message);
                throw new ShiftingException(message);
            }

            // Step 5: Reset current variant with initial state
            network.getVariantManager().setWorkingVariant(initialVariantId);
            network.getVariantManager().removeVariant(processedVariantId);
            network.getVariantManager().removeVariant(workingVariantCopyId);
        } finally {
            // here set working variant generators pmin and pmax values to initial values
            resetInitialPminPmax(network, zonalScalable, zoneIds, initGenerators);
        }
    }

    private void preProcessNetwork(Network network, ScalableGeneratorConnector scalableGeneratorConnector, String initialVariantId, String processedVariantId, String workingVariantCopyId) throws ShiftingException {
        network.getVariantManager().cloneVariant(initialVariantId, processedVariantId, true);
        network.getVariantManager().setWorkingVariant(processedVariantId);
        //scalableGeneratorConnector.prepareForScaling(network, CONNECT_GENERATORS_IN_COUNTRIES); todo refactoring preProcess
        network.getVariantManager().cloneVariant(processedVariantId, workingVariantCopyId, true);
        network.getVariantManager().setWorkingVariant(workingVariantCopyId);
    }

    public void updateScalingValuesWithMismatch(Map<String, Double> scalingValuesByCountry, double mismatchEsPt, double mismatchEsFr) {
        switch (direction) {
            case ES_FR:
            case FR_ES:
                scalingValuesByCountry.put(toEic("FR"), scalingValuesByCountry.get(toEic("FR")) - mismatchEsFr);
                break;

            case ES_PT:
            case PT_ES:
                scalingValuesByCountry.put(toEic("PT"), scalingValuesByCountry.get(toEic("PT")) - mismatchEsPt);
                break;
        }

        scalingValuesByCountry.put(toEic("ES"), scalingValuesByCountry.get(toEic("ES")) + mismatchEsPt + mismatchEsFr);
    }

    public Map<String, Double> getTargetExchanges(double stepValue) {
        return processType.equals(ProcessType.D2CC) ? getD2ccTargetExchanges(stepValue) : getIdccTargetExchanges(stepValue, initialNetPositions);
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

    private Map<String, InitGenerator> setPminPmaxToDefaultValue(Network network, ZonalData<Scalable> scalableZonalData, Set<String> zonesIds) {
        Map<String, InitGenerator> initGenerators = new HashMap<>();
        zonesIds.stream()
                //filter out FRANCE because it is always in proportional and not absolute values
                .filter(zoneId -> !zoneId.equals(toEic("FR")))
                .map(scalableZonalData::getData)
                .filter(Objects::nonNull)
                .map(scalable -> scalable.filterInjections(network).stream()
                        .filter(Generator.class::isInstance)
                        .map(Generator.class::cast)
                        .collect(Collectors.toList())).forEach(generators -> generators.forEach(generator -> {
                            if (Double.isNaN(generator.getTargetP())) {
                                generator.setTargetP(0.);
                            }
                            InitGenerator initGenerator = new InitGenerator();
                            initGenerator.setpMin(generator.getMinP());
                            initGenerator.setpMax(generator.getMaxP());
                            String genId = generator.getId();
                            if (!initGenerators.containsKey(genId)) {
                                initGenerators.put(genId, initGenerator);
                            }
                            generator.setMinP(DEFAULT_PMIN);
                            generator.setMaxP(DEFAULT_PMAX);
                        }));
        LOGGER.info("Pmax and Pmin are set to default values for network {}", network.getNameOrId());
        return initGenerators;
    }

    private void resetInitialPminPmax(Network network, ZonalData<Scalable> scalableZonalData, Set<String> zonesIds, Map<String, InitGenerator> initGenerators) {
        zonesIds.forEach(zoneId -> {
            Scalable scalable = scalableZonalData.getData(zoneId);
            //filter out FRANCE because it is always in proportional and not absolute values
            if (scalable != null && !zoneId.equals(toEic("FR"))) {
                List<Generator> generators = scalable.filterInjections(network).stream()
                        .filter(Generator.class::isInstance)
                        .map(Generator.class::cast)
                        .toList();

                generators.forEach(generator -> {
                    generator.setMaxP(Math.max(generator.getTargetP(), initGenerators.get(generator.getId()).getpMax()));
                    generator.setMinP(Math.min(generator.getTargetP(), initGenerators.get(generator.getId()).getpMin()));
                    if (generator.getTargetP() > initGenerators.get(generator.getId()).getpMax()
                            || generator.getTargetP() < initGenerators.get(generator.getId()).getpMin()) {
                        LOGGER.debug("GENERATOR: id=[{}], has targetP: [{}] outside initial min max values", generator.getId(), generator.getTargetP());
                    }
                });
            }
        });
        LOGGER.info("Pmax and Pmin are reset to initial values for network {}", network.getNameOrId());
    }

    private static class InitGenerator {
        double pMin;
        double pMax;

        public double getpMin() {
            return pMin;
        }

        public void setpMin(double pMin) {
            this.pMin = pMin;
        }

        public double getpMax() {
            return pMax;
        }

        public void setpMax(double pMax) {
            this.pMax = pMax;
        }
    }
}

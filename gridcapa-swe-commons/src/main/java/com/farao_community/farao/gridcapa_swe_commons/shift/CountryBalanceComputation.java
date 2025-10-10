/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_swe_commons.shift;

import com.farao_community.farao.gridcapa_swe_commons.exception.SweBaseCaseUnsecureException;
import com.farao_community.farao.gridcapa_swe_commons.resource.SweEICode;
import com.powsybl.balances_adjustment.util.CountryArea;
import com.powsybl.balances_adjustment.util.CountryAreaFactory;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public final class CountryBalanceComputation {
    private static final Logger LOGGER = LoggerFactory.getLogger(CountryBalanceComputation.class);

    private CountryBalanceComputation() {
        // Should not be instantiated
    }

    public static Map<String, Double> computeSweCountriesBalances(Network network, LoadFlowParameters loadFlowParameters) {
        LOGGER.info("Computing initial SWE countries balance");
        Map<String, Double> countriesBalances = new HashMap<>();
        runLoadFlow(network, network.getVariantManager().getWorkingVariantId(), loadFlowParameters);
        Map<String, Double> bordersExchanges = computeSweBordersExchanges(network);
        countriesBalances.put(SweEICode.PT_EIC, -bordersExchanges.get("ES_PT"));
        countriesBalances.put(SweEICode.ES_EIC, bordersExchanges.values().stream().reduce(0., Double::sum));
        countriesBalances.put(SweEICode.FR_EIC, -bordersExchanges.get("ES_FR"));

        return countriesBalances;
    }

    public static Map<String, Double> computeSweBordersExchanges(Network network) {
        Map<String, Double> borderExchanges = new HashMap<>();
        Map<Country, CountryArea> countryAreaPerCountry = Stream.of(Country.FR, Country.ES, Country.PT)
                .collect(Collectors.toMap(Function.identity(), country -> new CountryAreaFactory(country).create(network)));
        borderExchanges.put("ES_FR", getBorderExchange(Country.ES, Country.FR, countryAreaPerCountry));
        borderExchanges.put("ES_PT", getBorderExchange(Country.ES, Country.PT, countryAreaPerCountry));
        return borderExchanges;
    }

    private static void runLoadFlow(Network network, String workingStateId, LoadFlowParameters loadFlowParameters) {
        ComputationManager computationManager;
        try {
            final Map<String, String> mdc = MDC.getCopyOfContextMap();
            computationManager = new LocalComputationManager(command -> {
                MDC.setContextMap(mdc);
                command.run();
            });
        } catch (IOException e) {
            LOGGER.error("Failed to build custom LocalComputationManager", e);
            computationManager = LocalComputationManager.getDefault();
        }

        LoadFlowResult result = LoadFlow.run(network, workingStateId, computationManager, loadFlowParameters);
        if (result.isFailed()) {
            LOGGER.error("Loadflow computation diverged on network '{}'", network.getId());
            throw new SweBaseCaseUnsecureException(String.format("Loadflow computation diverged on network %s", network.getId()));
        }
    }

    private static double getBorderExchange(Country fromCountry, Country toCountry, Map<Country, CountryArea> countryAreaPerCountry) {
        return countryAreaPerCountry.get(fromCountry).getLeavingFlowToCountry(countryAreaPerCountry.get(toCountry));
    }
}

/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_swe_commons.shift;

import com.farao_community.farao.gridcapa_swe_commons.exception.SweBaseCaseUnsecureException;
import com.farao_community.farao.gridcapa_swe_commons.loadflow.LoadFlowUtil;
import com.powsybl.balances_adjustment.util.BorderBasedCountryArea;
import com.powsybl.balances_adjustment.util.CountryAreaFactory;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.farao_community.farao.gridcapa_swe_commons.resource.SweEICode.ES_EIC;
import static com.farao_community.farao.gridcapa_swe_commons.resource.SweEICode.FR_EIC;
import static com.farao_community.farao.gridcapa_swe_commons.resource.SweEICode.PT_EIC;
import static com.powsybl.iidm.network.Country.ES;
import static com.powsybl.iidm.network.Country.FR;
import static com.powsybl.iidm.network.Country.PT;
import static java.util.stream.Collectors.toMap;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public final class CountryBalanceComputation {
    private static final Logger LOGGER = LoggerFactory.getLogger(CountryBalanceComputation.class);

    private CountryBalanceComputation() {
        // Should not be instantiated
    }

    public static Map<String, Double> computeSweCountriesBalances(final Network network, final LoadFlowParameters loadFlowParameters) {
        LOGGER.info("Computing initial SWE countries balance");
        final Map<String, Double> countriesBalances = new HashMap<>();
        runLoadFlow(network, network.getVariantManager().getWorkingVariantId(), loadFlowParameters);
        final Map<String, Double> bordersExchanges = computeSweBordersExchanges(network);
        countriesBalances.put(PT_EIC, -bordersExchanges.get("ES_PT"));
        countriesBalances.put(ES_EIC, bordersExchanges.values().stream().reduce(0., Double::sum));
        countriesBalances.put(FR_EIC, -bordersExchanges.get("ES_FR"));

        return countriesBalances;
    }

    public static Map<String, Double> computeSweBordersExchanges(final Network network) {
        final Map<String, Double> borderExchanges = new HashMap<>();
        final Map<Country, BorderBasedCountryArea> countryAreaPerCountry = Stream.of(FR, ES, PT)
                .collect(toMap(Function.identity(),
                               country -> (BorderBasedCountryArea) new CountryAreaFactory(country).create(network)));
        borderExchanges.put("ES_FR", getBorderExchange(ES, FR, countryAreaPerCountry));
        borderExchanges.put("ES_PT", getBorderExchange(ES, PT, countryAreaPerCountry));
        return borderExchanges;
    }

    private static void runLoadFlow(final Network network, final String workingStateId, final LoadFlowParameters loadFlowParameters) {
        final LoadFlowResult result = LoadFlowUtil.runLoadFlowWithMdc(network, workingStateId, loadFlowParameters);

        if (result.isFailed()) {
            LOGGER.error("Loadflow computation diverged on network '{}'", network.getId());
            throw new SweBaseCaseUnsecureException(String.format("Loadflow computation diverged on network %s", network.getId()));
        }
    }

    private static double getBorderExchange(final Country from, final Country to, final Map<Country, BorderBasedCountryArea> countryAreaByCountry) {
        return countryAreaByCountry.get(from).getLeavingFlowToCountry(countryAreaByCountry.get(to));
    }
}

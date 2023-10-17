/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_swe_commons.shift;

import com.farao_community.farao.commons.EICode;
import com.powsybl.balances_adjustment.util.CountryArea;
import com.powsybl.balances_adjustment.util.CountryAreaFactory;
import com.powsybl.cgmes.extensions.CgmesControlAreas;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final String CONTROL_AREA_FR = "_7917d422-d956-5595-9489-5ad54aeae656";
    private static final String CONTROL_AREA_ES = "_cb354e3c-97fe-4fde-f9a6-b3fe750d4a34";
    private static final String CONTROL_AREA_PT = "_2b6ed858-027e-11e7-afc1-94659cb4e466";

    private CountryBalanceComputation() {
         // Should not be instantiated
    }

    public static Map<String, Double> computeSweCountriesBalances(Network network) {
        LOGGER.info("Computing initial SWE countries balance");
        Map<String, Double> countriesBalances = new HashMap<>();

        if (runLoadFlow(network, network.getVariantManager().getWorkingVariantId())) {
            Map<String, Double> bordersExchanges = computeSweBordersExchanges(network);
            countriesBalances.put(toEic("PT"), -bordersExchanges.get("ES_PT"));
            countriesBalances.put(toEic("ES"), bordersExchanges.values().stream().reduce(0., Double::sum));
            countriesBalances.put(toEic("FR"), -bordersExchanges.get("ES_FR"));
            return countriesBalances;
        }

        // If the loadflow diverges, we use the netinterchange values instead
        countriesBalances.put(toEic("PT"), getNetInterchange(network, CONTROL_AREA_PT));
        countriesBalances.put(toEic("ES"), getNetInterchange(network, CONTROL_AREA_ES));
        countriesBalances.put(toEic("FR"), getNetInterchange(network, CONTROL_AREA_FR));

        return countriesBalances;
    }

    private static double getNetInterchange(Network network, String id) {
        return network.getExtension(CgmesControlAreas.class).getCgmesControlArea(id).getNetInterchange();
    }

    private static boolean runLoadFlow(Network network, String workingStateId) {
        LoadFlowResult result = LoadFlow.run(network, workingStateId, LocalComputationManager.getDefault(), LoadFlowParameters.load());
        return result.isOk();
    }

    public static Map<String, Double> computeSweBordersExchanges(Network network) {
        Map<String, Double> borderExchanges = new HashMap<>();
        Map<Country, CountryArea> countryAreaPerCountry = Stream.of(Country.FR, Country.ES, Country.PT)
                .collect(Collectors.toMap(Function.identity(), country -> new CountryAreaFactory(country).create(network)));
        borderExchanges.put("ES_FR", getBorderExchange(Country.ES, Country.FR, countryAreaPerCountry));
        borderExchanges.put("ES_PT", getBorderExchange(Country.ES, Country.PT, countryAreaPerCountry));
        return borderExchanges;
    }

    private static double getBorderExchange(Country fromCountry, Country toCountry, Map<Country, CountryArea> countryAreaPerCountry) {
        return countryAreaPerCountry.get(fromCountry).getLeavingFlowToCountry(countryAreaPerCountry.get(toCountry));
    }

    private static String toEic(String country) {
        return new EICode(Country.valueOf(country)).getAreaCode();
    }
}

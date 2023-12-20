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
import com.powsybl.cgmes.extensions.CgmesControlArea;
import com.powsybl.cgmes.extensions.CgmesControlAreas;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@Service
public final class CountryBalanceComputation {
    private static final Logger LOGGER = LoggerFactory.getLogger(CountryBalanceComputation.class);

    private final Logger businessLogger;

    public CountryBalanceComputation(Logger businessLogger) {
        this.businessLogger = businessLogger;
    }

    public Map<String, Double> computeSweCountriesBalances(Network network) {
        LOGGER.info("Computing initial SWE countries balance");
        Map<String, Double> countriesBalances = new HashMap<>();

        String portugalEic = toEic("PT");
        String spainEic = toEic("ES");
        String franceEic = toEic("FR");

        if (runLoadFlow(network, network.getVariantManager().getWorkingVariantId())) {
            Map<String, Double> bordersExchanges = computeSweBordersExchanges(network);
            countriesBalances.put(portugalEic, -bordersExchanges.get("ES_PT"));
            countriesBalances.put(spainEic, bordersExchanges.values().stream().reduce(0., Double::sum));
            countriesBalances.put(franceEic, -bordersExchanges.get("ES_FR"));
            businessLogger.info("Base case loadflow is secure");
        } else {
            LOGGER.warn("Base case is not secure: using net interchange values (Loadflow computation diverged on network '{}')", network.getId());
            businessLogger.warn("Base case loadflow is not secure: using net interchange values");
            // If the loadflow diverges, we use the netinterchange values instead
            countriesBalances.put(portugalEic, getNetInterchange(network, portugalEic));
            countriesBalances.put(spainEic, getNetInterchange(network, spainEic));
            countriesBalances.put(franceEic, getNetInterchange(network, franceEic));
        }
        return countriesBalances;
    }

    private static double getNetInterchange(Network network, String eic) {
        return network.getExtension(CgmesControlAreas.class).getCgmesControlAreas().stream()
                .filter(a -> StringUtils.equals(eic, a.getEnergyIdentificationCodeEIC()))
                .map(CgmesControlArea::getNetInterchange).findFirst().orElse(0d);
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

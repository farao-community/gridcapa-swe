/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_swe_commons.shift;

import com.farao_community.farao.commons.EICode;
import com.farao_community.farao.gridcapa_swe_commons.exception.SweInvalidDataException;
import com.powsybl.glsk.api.io.GlskDocumentImporters;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.commons.ZonalDataImpl;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ZonalScalableProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZonalScalableProvider.class);
    private static final Object LOCK_GLSK = new Object();

    public ZonalData<Scalable> get(String glskUrl, Network network, OffsetDateTime timestamp) {
        ZonalData<Scalable> zonalData = importGlsk(glskUrl, network, timestamp.toInstant());
        String eicFR = new EICode(Country.FR).getAreaCode();
        if (zonalData.getData(eicFR) == null) {
            LOGGER.warn("Glsk file does not contains FR scalable for timestamp {}, a Country generators scalable is added", timestamp);
            Scalable scalableFR = getCountryGeneratorsScalableForFR(network);
            zonalData.addAll(new ZonalDataImpl<>(Collections.singletonMap(eicFR, scalableFR)));
        }
        return zonalData;
    }

    public ZonalData<Scalable> importGlsk(String glskUrl, Network network, Instant instant) {
        try (InputStream glskResultStream = new URL(glskUrl).openStream()) {
            synchronized (LOCK_GLSK) {
                LOGGER.info("Importing Glsk file : {}", glskUrl);
                return GlskDocumentImporters.importGlsk(glskResultStream).getZonalScalable(network, instant);
            }
        } catch (IOException e) {
            throw new SweInvalidDataException("Cannot import glsk from url", e);
        }
    }

    private Scalable getCountryGeneratorsScalableForFR(Network network) {
        List<Scalable> scalables = new ArrayList<>();
        List<Double> percentages = new ArrayList<>();
        List<Generator> generators;
        generators = network.getGeneratorStream()
                .filter(generator -> Country.FR.equals(generator.getTerminal().getVoltageLevel().getSubstation().map(Substation::getNullableCountry).orElse(null)))
                .filter(NetworkUtil::isCorrect)
                .collect(Collectors.toList());
        //calculate sum P of country's generators
        double totalCountryP = generators.stream().mapToDouble(NetworkUtil::pseudoTargetP).sum();
        //calculate factor of each generator
        generators.forEach(generator -> {
            double generatorPercentage =  100 * NetworkUtil.pseudoTargetP(generator) / totalCountryP;
            percentages.add(generatorPercentage);
            scalables.add(Scalable.onGenerator(generator.getId()));
        });

        return Scalable.proportional(percentages, scalables);
    }
}

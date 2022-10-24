/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy;

import com.farao_community.farao.commons.EICode;
import com.farao_community.farao.swe.runner.app.services.FileImporter;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.commons.ZonalDataImpl;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@Service
public class ZonalScalableProvider {
    private final FileImporter fileImporter;

    public ZonalScalableProvider(FileImporter fileImporter) {
        this.fileImporter = fileImporter;
    }

    public ZonalData<Scalable> get(String glskUrl, Network network, OffsetDateTime timestamp) {
        ZonalData<Scalable> zonalScalable = fileImporter.importGlsk(glskUrl, network, timestamp.toInstant());
        String eicFR = new EICode(Country.FR).getAreaCode();
        if (zonalScalable.getData(eicFR) == null) {
            Scalable scalableFR = getCountryGeneratorsScalablefoFR(network);
            zonalScalable.addAll(new ZonalDataImpl<>(Collections.singletonMap(eicFR, scalableFR)));
        }
        return zonalScalable;
    }

    private Scalable getCountryGeneratorsScalablefoFR(Network network) {
        List<Scalable> scalables = new ArrayList<>();
        List<Float> percentages = new ArrayList<>();
        List<Generator> generators;
        generators = network.getGeneratorStream()
                .filter(generator -> Country.FR.equals(generator.getTerminal().getVoltageLevel().getSubstation().map(Substation::getNullableCountry).orElse(null)))
                .filter(NetworkUtil::isCorrect)
                .collect(Collectors.toList());
        //calculate sum P of country's generators
        double totalCountryP = generators.stream().mapToDouble(NetworkUtil::pseudoTargetP).sum();
        //calculate factor of each generator
        generators.forEach(generator -> {
            float generatorPercentage = (float) (100 * NetworkUtil.pseudoTargetP(generator) / totalCountryP);
            percentages.add(generatorPercentage);
            scalables.add(Scalable.onGenerator(generator.getId()));
        });

        return Scalable.proportional(percentages, scalables, true);
    }
}


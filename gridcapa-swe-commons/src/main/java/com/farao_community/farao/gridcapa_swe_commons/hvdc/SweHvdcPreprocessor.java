/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_swe_commons.hvdc;

import com.farao_community.farao.gridcapa_swe_commons.hvdc.parameters.HvdcCreationParameters;
import com.farao_community.farao.gridcapa_swe_commons.hvdc.parameters.SwePreprocessorParameters;
import com.farao_community.farao.gridcapa_swe_commons.hvdc.parameters.json.JsonSwePreprocessorImporter;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
public class SweHvdcPreprocessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(SweHvdcPreprocessor.class);

    public void applyParametersToNetwork(InputStream parameters, Network network) {
        SwePreprocessorParameters params = JsonSwePreprocessorImporter.read(parameters);
        List<String> hvdcIds = params.getHvdcCreationParametersSet().stream().map(HvdcCreationParameters::getId).toList();
        HvdcLinkProcessor.replaceEquivalentModelByHvdc(network, params.getHvdcCreationParametersSet());
        LOGGER.info("HVDC {} added to network", hvdcIds);
    }

}

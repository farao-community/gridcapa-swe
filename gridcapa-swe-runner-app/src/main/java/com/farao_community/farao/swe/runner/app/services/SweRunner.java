/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.swe.runner.api.resource.SweRequest;
import com.farao_community.farao.swe.runner.api.resource.SweResponse;
import com.farao_community.farao.swe.runner.app.utils.Threadable;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Service
public class SweRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(SweRunner.class);
    private final NetworkService networkImporter;

    public SweRunner(NetworkService networkImporter) {
        this.networkImporter = networkImporter;
    }

    @Threadable
    public SweResponse run(SweRequest sweRequest) {
        LOGGER.info("Request received for timestamp {}", sweRequest.getTargetProcessDateTime());
        Network network = networkImporter.importNetwork(sweRequest);
        return new SweResponse(sweRequest.getId());
    }

}

/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.swe.runner.api.resource.SweRequest;
import com.farao_community.farao.swe.runner.api.resource.SweResponse;
import com.farao_community.farao.swe.runner.app.utils.Threadable;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Service
public class SweRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(SweRunner.class);
    private final NetworkService networkImporter;
    private final FileImporter fileImporter;

    public SweRunner(NetworkService networkImporter, FileImporter fileImporter) {
        this.networkImporter = networkImporter;
        this.fileImporter = fileImporter;
    }

    @Threadable
    public SweResponse run(SweRequest sweRequest) {
        final OffsetDateTime processDateTime = sweRequest.getTargetProcessDateTime();
        LOGGER.info("Request received for timestamp {}", processDateTime);
        Network network = networkImporter.importNetwork(sweRequest);
        Crac crac = fileImporter.importCimCracFromUrlWithNetwork(sweRequest.getCrac().getUrl(), processDateTime, network);
        //to be continued!
        return new SweResponse(sweRequest.getId());
    }

}

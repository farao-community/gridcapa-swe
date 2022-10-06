/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.cim.CimCrac;
import com.farao_community.farao.swe.runner.api.resource.ProcessType;
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

    public static final String CRAC_CIM_CRAC_CREATION_PARAMETERS_PT_ES_JSON = "/crac/CimCracCreationParameters_PT-ES.json";
    public static final String CRAC_CIM_CRAC_CREATION_PARAMETERS_FR_ES_JSON = "/crac/CimCracCreationParameters_FR-ES.json";

    private final NetworkService networkImporter;
    private final FileImporter fileImporter;
    private final FileExporter fileExporter;

    public SweRunner(NetworkService networkImporter, FileImporter fileImporter, FileExporter fileExporter) {
        this.networkImporter = networkImporter;
        this.fileImporter = fileImporter;
        this.fileExporter = fileExporter;
    }

    @Threadable
    public SweResponse run(SweRequest sweRequest) {
        LOGGER.info("Request received for timestamp {}", sweRequest.getTargetProcessDateTime());
        Network network = networkImporter.importNetwork(sweRequest);
        CimCrac cimCrac = fileImporter.importCimCrac(sweRequest);
        OffsetDateTime targetProcessDateTime = sweRequest.getTargetProcessDateTime();
        Crac cracEsPt = fileImporter.importCracFromCimCracAndNetwork(cimCrac, targetProcessDateTime, network, CRAC_CIM_CRAC_CREATION_PARAMETERS_PT_ES_JSON);
        Crac cracFrEs = fileImporter.importCracFromCimCracAndNetwork(cimCrac, targetProcessDateTime, network, CRAC_CIM_CRAC_CREATION_PARAMETERS_FR_ES_JSON);
        String jsonPathEsPt = fileExporter.saveCracInJsonFormat(cracEsPt, "cracEsPt.json", targetProcessDateTime, ProcessType.D2CC);
        String jsonPathFrEs = fileExporter.saveCracInJsonFormat(cracFrEs, "cracFrEs.json", targetProcessDateTime, ProcessType.D2CC);
        //to be continued!
        return new SweResponse(sweRequest.getId());
    }

}

/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
import com.farao_community.farao.swe.runner.api.resource.SweRequest;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.powsybl.iidm.network.Network;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Service
public class FilesService {

    public static final String CRAC_CIM_CRAC_CREATION_PARAMETERS_PT_ES_JSON = "/crac/CimCracCreationParameters_PT-ES.json";
    public static final String CRAC_CIM_CRAC_CREATION_PARAMETERS_FR_ES_JSON = "/crac/CimCracCreationParameters_FR-ES.json";

    private final NetworkService networkImporter;
    private final FileImporter fileImporter;
    private final FileExporter fileExporter;

    public FilesService(NetworkService networkImporter, FileImporter fileImporter, FileExporter fileExporter) {
        this.networkImporter = networkImporter;
        this.fileImporter = fileImporter;
        this.fileExporter = fileExporter;
    }

    public SweData importFiles(SweRequest sweRequest) {
        Network network = networkImporter.importNetwork(sweRequest);
        OffsetDateTime targetProcessDateTime = sweRequest.getTargetProcessDateTime();
        CimCracCreationContext cracCreationContextFrEs = fileImporter.importCracFromCimCracAndNetwork(fileImporter.importCimCrac(sweRequest), targetProcessDateTime, network, CRAC_CIM_CRAC_CREATION_PARAMETERS_FR_ES_JSON);
        CimCracCreationContext cracCreationContextEsPt = fileImporter.importCracFromCimCracAndNetwork(fileImporter.importCimCrac(sweRequest), targetProcessDateTime, network, CRAC_CIM_CRAC_CREATION_PARAMETERS_PT_ES_JSON);
        Crac cracFrEs = cracCreationContextFrEs.getCrac();
        Crac cracEsPt = cracCreationContextEsPt.getCrac();
        String jsonCracPathFrEs = fileExporter.saveCracInJsonFormat(cracFrEs, "cracFrEs.json", targetProcessDateTime, sweRequest.getProcessType());
        String jsonCracPathEsPt = fileExporter.saveCracInJsonFormat(cracEsPt, "cracEsPt.json", targetProcessDateTime, sweRequest.getProcessType());
        return new SweData(sweRequest.getId(), sweRequest.getTargetProcessDateTime(), sweRequest.getProcessType(), network, cracCreationContextFrEs, cracCreationContextEsPt, sweRequest.getGlsk().getUrl(), jsonCracPathEsPt, jsonCracPathFrEs);
    }
}

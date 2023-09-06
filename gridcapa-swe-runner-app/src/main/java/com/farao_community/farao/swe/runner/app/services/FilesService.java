/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
import com.farao_community.farao.swe.runner.api.resource.ProcessType;
import com.farao_community.farao.swe.runner.api.resource.SweRequest;
import com.farao_community.farao.swe.runner.app.dichotomy.DichotomyDirection;
import com.farao_community.farao.swe.runner.app.domain.MergingViewData;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.powsybl.iidm.network.Network;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Service
public class FilesService {

    public static final String CRAC_CIM_CRAC_CREATION_PARAMETERS_PT_ES_IDCC_JSON = "/crac/CimCracCreationParameters_PT-ES_IDCC.json";
    public static final String CRAC_CIM_CRAC_CREATION_PARAMETERS_PT_ES_D2CC_JSON = "/crac/CimCracCreationParameters_PT-ES_D2CC.json";
    public static final String CRAC_CIM_CRAC_CREATION_PARAMETERS_FR_ES_IDCC_JSON = "/crac/CimCracCreationParameters_FR-ES_IDCC.json";
    public static final String CRAC_CIM_CRAC_CREATION_PARAMETERS_FR_ES_D2CC_JSON = "/crac/CimCracCreationParameters_FR-ES_D2CC.json";

    private final NetworkService networkService;
    private final MergingViewService mergingViewService;
    private final FileImporter fileImporter;
    private final FileExporter fileExporter;

    public FilesService(NetworkService networkImporter, MergingViewService mergingViewService, FileImporter fileImporter, FileExporter fileExporter) {
        this.networkService = networkImporter;
        this.mergingViewService = mergingViewService;
        this.fileImporter = fileImporter;
        this.fileExporter = fileExporter;
    }

    public SweData importFiles(SweRequest sweRequest) {
        MergingViewData mergingViewData = mergingViewService.importMergingView(sweRequest);
        Network networkEsFr = networkService.importNetwork(sweRequest);
        OffsetDateTime targetProcessDateTime = sweRequest.getTargetProcessDateTime();
        Network networkFrEs = networkService.loadNetworkFromMinio(targetProcessDateTime);
        Network networkEsPt = networkService.loadNetworkFromMinio(targetProcessDateTime);
        Network networkPtEs = networkService.loadNetworkFromMinio(targetProcessDateTime);
        String cracCreationParamFrEs = sweRequest.getProcessType().equals(ProcessType.D2CC) ? CRAC_CIM_CRAC_CREATION_PARAMETERS_FR_ES_D2CC_JSON : CRAC_CIM_CRAC_CREATION_PARAMETERS_FR_ES_IDCC_JSON;
        String cracCreationParamEsPt = sweRequest.getProcessType().equals(ProcessType.D2CC) ? CRAC_CIM_CRAC_CREATION_PARAMETERS_PT_ES_D2CC_JSON : CRAC_CIM_CRAC_CREATION_PARAMETERS_PT_ES_IDCC_JSON;
        CimCracCreationContext cracCreationContextFrEs = fileImporter.importCracFromCimCracAndNetwork(fileImporter.importCimCrac(sweRequest), targetProcessDateTime, networkEsFr, cracCreationParamFrEs);
        CimCracCreationContext cracCreationContextEsPt = fileImporter.importCracFromCimCracAndNetwork(fileImporter.importCimCrac(sweRequest), targetProcessDateTime, networkEsPt, cracCreationParamEsPt);
        Crac cracFrEs = cracCreationContextFrEs.getCrac();
        Crac cracEsPt = cracCreationContextEsPt.getCrac();
        String jsonCracPathFrEs = fileExporter.saveCracInJsonFormat(cracFrEs, "cracFrEs.json", targetProcessDateTime, sweRequest.getProcessType());
        String jsonCracPathEsPt = fileExporter.saveCracInJsonFormat(cracEsPt, "cracEsPt.json", targetProcessDateTime, sweRequest.getProcessType());
        String raoParametersEsFrUrl = fileExporter.saveRaoParameters(targetProcessDateTime, sweRequest.getProcessType(), DichotomyDirection.ES_FR);
        String raoParametersEsPtUrl = fileExporter.saveRaoParameters(targetProcessDateTime, sweRequest.getProcessType(), DichotomyDirection.ES_PT);
        return new SweData(sweRequest.getId(), sweRequest.getTargetProcessDateTime(), sweRequest.getProcessType(), networkEsFr, networkFrEs, networkEsPt, networkPtEs, mergingViewData, cracCreationContextFrEs, cracCreationContextEsPt, sweRequest.getGlsk().getUrl(), jsonCracPathEsPt, jsonCracPathFrEs, raoParametersEsFrUrl, raoParametersEsPtUrl);
    }
}

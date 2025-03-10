/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import com.farao_community.farao.gridcapa_swe_commons.resource.ProcessType;
import com.farao_community.farao.swe.runner.api.resource.SweFileResource;
import com.farao_community.farao.swe.runner.api.resource.SweRequest;
import com.farao_community.farao.swe.runner.app.domain.CgmesFileType;
import com.farao_community.farao.gridcapa_swe_commons.hvdc.HvdcInformation;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweTaskParameters;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.io.cim.craccreator.CimCracCreationContext;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

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
    private final RemoveRemoteVoltageRegulationInFranceService removeRemoteVoltageRegulationInFranceService;

    private final FileImporter fileImporter;
    private final FileExporter fileExporter;

    public FilesService(NetworkService networkImporter, RemoveRemoteVoltageRegulationInFranceService removeRemoteVoltageRegulationInFranceService, FileImporter fileImporter, FileExporter fileExporter) {
        this.networkService = networkImporter;
        this.removeRemoteVoltageRegulationInFranceService = removeRemoteVoltageRegulationInFranceService;
        this.fileImporter = fileImporter;
        this.fileExporter = fileExporter;
    }

    public SweData importFiles(SweRequest sweRequest, SweTaskParameters sweTaskParameters) {
        OffsetDateTime targetProcessDateTime = sweRequest.getTargetProcessDateTime();
        Network mergedNetwork = networkService.importMergedNetwork(sweRequest);
        List<HvdcInformation> hvdcInformationList = networkService.getHvdcInformationFromNetwork(mergedNetwork);
        networkService.addHvdcAndPstToNetwork(mergedNetwork);
        Map<String, RemoveRemoteVoltageRegulationInFranceService.ReplacedVoltageRegulation> replacedVoltageRegulations = removeRemoteVoltageRegulationInFranceService.removeRemoteVoltageRegulationInFrance(mergedNetwork);
        fileExporter.saveMergedNetworkWithHvdc(mergedNetwork, targetProcessDateTime);

        Network networkEsFr = networkService.loadNetworkFromMinio(targetProcessDateTime);
        Network networkFrEs = networkService.loadNetworkFromMinio(targetProcessDateTime);
        Network networkEsPt = networkService.loadNetworkFromMinio(targetProcessDateTime);
        Network networkPtEs = networkService.loadNetworkFromMinio(targetProcessDateTime);
        String cracCreationParamFrEs = sweRequest.getProcessType().equals(ProcessType.D2CC) ? CRAC_CIM_CRAC_CREATION_PARAMETERS_FR_ES_D2CC_JSON : CRAC_CIM_CRAC_CREATION_PARAMETERS_FR_ES_IDCC_JSON;
        String cracCreationParamEsPt = sweRequest.getProcessType().equals(ProcessType.D2CC) ? CRAC_CIM_CRAC_CREATION_PARAMETERS_PT_ES_D2CC_JSON : CRAC_CIM_CRAC_CREATION_PARAMETERS_PT_ES_IDCC_JSON;
        CimCracCreationContext cracCreationContextFrEs = fileImporter.importCracFromCimCracAndNetwork(sweRequest.getCrac(), targetProcessDateTime, networkEsFr, cracCreationParamFrEs, sweTaskParameters);
        CimCracCreationContext cracCreationContextEsPt = fileImporter.importCracFromCimCracAndNetwork(sweRequest.getCrac(), targetProcessDateTime, networkEsPt, cracCreationParamEsPt, sweTaskParameters);
        Crac cracFrEs = cracCreationContextFrEs.getCrac();
        Crac cracEsPt = cracCreationContextEsPt.getCrac();
        String jsonCracPathFrEs = fileExporter.saveCracInJsonFormat(cracFrEs, "cracFrEs.json", targetProcessDateTime, sweRequest.getProcessType());
        String jsonCracPathEsPt = fileExporter.saveCracInJsonFormat(cracEsPt, "cracEsPt.json", targetProcessDateTime, sweRequest.getProcessType());
        String raoParametersEsFrUrl = fileExporter.saveRaoParameters(targetProcessDateTime, sweRequest.getProcessType(), sweTaskParameters, DichotomyDirection.ES_FR);
        String raoParametersEsPtUrl = fileExporter.saveRaoParameters(targetProcessDateTime, sweRequest.getProcessType(), sweTaskParameters, DichotomyDirection.ES_PT);
        EnumMap<CgmesFileType, SweFileResource> mapCgmesInputFiles = fillMapCgmesInputFiles(sweRequest);
        return new SweData(sweRequest.getId(), sweRequest.getCurrentRunId(), sweRequest.getTargetProcessDateTime(), sweRequest.getProcessType(), networkEsFr, networkFrEs, networkEsPt, networkPtEs, cracCreationContextFrEs, cracCreationContextEsPt, sweRequest.getGlsk().getUrl(), jsonCracPathEsPt, jsonCracPathFrEs, raoParametersEsFrUrl, raoParametersEsPtUrl, hvdcInformationList, mapCgmesInputFiles, replacedVoltageRegulations);
    }

    private EnumMap<CgmesFileType, SweFileResource> fillMapCgmesInputFiles(SweRequest sweRequest) {
        EnumMap<CgmesFileType, SweFileResource> mapCgmesInputFiles = new EnumMap<>(CgmesFileType.class);
        mapCgmesInputFiles.put(CgmesFileType.CORESO_SV, sweRequest.getCoresoSv());
        mapCgmesInputFiles.put(CgmesFileType.RTE_SSH, sweRequest.getRteSsh());
        mapCgmesInputFiles.put(CgmesFileType.RTE_EQ, sweRequest.getRteEq());
        mapCgmesInputFiles.put(CgmesFileType.RTE_TP, sweRequest.getRteTp());
        mapCgmesInputFiles.put(CgmesFileType.REE_SSH, sweRequest.getReeSsh());
        mapCgmesInputFiles.put(CgmesFileType.REE_EQ, sweRequest.getReeEq());
        mapCgmesInputFiles.put(CgmesFileType.REE_TP, sweRequest.getReeTp());
        mapCgmesInputFiles.put(CgmesFileType.REN_SSH, sweRequest.getRenSsh());
        mapCgmesInputFiles.put(CgmesFileType.REN_EQ, sweRequest.getRenEq());
        mapCgmesInputFiles.put(CgmesFileType.REN_TP, sweRequest.getRenTp());
        return mapCgmesInputFiles;
    }
}

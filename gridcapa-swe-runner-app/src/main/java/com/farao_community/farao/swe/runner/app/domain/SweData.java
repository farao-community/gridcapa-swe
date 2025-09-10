/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.domain;

import com.farao_community.farao.gridcapa_swe_commons.hvdc.HvdcInformation;
import com.farao_community.farao.gridcapa_swe_commons.resource.ProcessType;
import com.farao_community.farao.swe.runner.api.resource.SweFileResource;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.io.cim.craccreator.CimCracCreationContext;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
public class SweData {

    private final String id;
    private final String runId;
    private final OffsetDateTime timestamp;
    private final ProcessType processType;
    private final Network networkEsFr;
    private final Network networkFrEs;
    private final Network networkEsPt;
    private final Network networkPtEs;
    private final CimCracCreationContext cracEsPt;
    private final CimCracCreationContext cracFrEs;
    private final String glskUrl;
    private final String jsonCracPathEsPt;
    private final String jsonCracPathFrEs;
    private final String raoParametersEsFrUrl;
    private final String raoParametersEsPtUrl;
    private final List<HvdcInformation> hvdcInformationList;
    private final Map<CgmesFileType, SweFileResource> mapCgmesInputFiles;

    public SweData(String id, String runId, OffsetDateTime timestamp, ProcessType processType, Network networkEsFr, Network networkFrEs, Network networkEsPt, Network networkPtEs, CimCracCreationContext cracFrEs, CimCracCreationContext cracEsPt, String glskUrl, String jsonCracPathEsPt, String jsonCracPathFrEs, String raoParametersEsFrUrl, String raoParametersEsPtUrl, List<HvdcInformation> hvdcInformationList, Map<CgmesFileType, SweFileResource> mapCgmesInputFiles) {
        this.id = id;
        this.runId = runId;
        this.timestamp = timestamp;
        this.processType = processType;
        this.networkEsFr = networkEsFr;
        this.networkFrEs = networkFrEs;
        this.networkEsPt = networkEsPt;
        this.networkPtEs = networkPtEs;
        this.cracEsPt = cracEsPt;
        this.cracFrEs = cracFrEs;
        this.glskUrl = glskUrl;
        this.jsonCracPathEsPt = jsonCracPathEsPt;
        this.jsonCracPathFrEs = jsonCracPathFrEs;
        this.raoParametersEsFrUrl = raoParametersEsFrUrl;
        this.raoParametersEsPtUrl = raoParametersEsPtUrl;
        this.hvdcInformationList = hvdcInformationList;
        this.mapCgmesInputFiles = mapCgmesInputFiles;
    }

    public String getId() {
        return id;
    }

    public String getRunId() {
        return runId;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public ProcessType getProcessType() {
        return processType;
    }

    public Network getNetworkEsFr() {
        return networkEsFr;
    }

    public Network getNetworkFrEs() {
        return networkFrEs;
    }

    public Network getNetworkEsPt() {
        return networkEsPt;
    }

    public Network getNetworkPtEs() {
        return networkPtEs;
    }

    public CimCracCreationContext getCracEsPt() {
        return cracEsPt;
    }

    public CimCracCreationContext getCracFrEs() {
        return cracFrEs;
    }

    public String getGlskUrl() {
        return glskUrl;
    }

    public String getJsonCracPathEsPt() {
        return jsonCracPathEsPt;
    }

    public String getJsonCracPathFrEs() {
        return jsonCracPathFrEs;
    }

    public String getRaoParametersEsFrUrl() {
        return raoParametersEsFrUrl;
    }

    public String getRaoParametersEsPtUrl() {
        return raoParametersEsPtUrl;
    }

    public List<HvdcInformation> getHvdcInformationList() {
        return hvdcInformationList;
    }

    public Map<CgmesFileType, SweFileResource> getMapCgmesInputFiles() {
        return mapCgmesInputFiles;
    }

}

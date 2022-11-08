/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.domain;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.swe.runner.api.resource.ProcessType;
import com.powsybl.iidm.network.Network;

import java.time.OffsetDateTime;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
public class SweData {

    private final String id;
    private final OffsetDateTime timestamp;
    private final ProcessType processType;
    private final Network network;
    private final MergingViewData mergingViewData;
    private final Crac cracEsPt;
    private final Crac cracFrEs;
    private final String glskUrl;
    private final String jsonCracPathEsPt;
    private final String jsonCracPathFrEs;

    public SweData(String id, OffsetDateTime timestamp, ProcessType processType, Network network, MergingViewData mergingViewData, Crac cracEsPt, Crac cracFrEs, String glskUrl, String jsonCracPathEsPt, String jsonCracPathFrEs) {
        this.id = id;
        this.timestamp = timestamp;
        this.processType = processType;
        this.network = network;
        this.mergingViewData = mergingViewData;
        this.cracEsPt = cracEsPt;
        this.cracFrEs = cracFrEs;
        this.glskUrl = glskUrl;
        this.jsonCracPathEsPt = jsonCracPathEsPt;
        this.jsonCracPathFrEs = jsonCracPathFrEs;
    }

    public String getId() {
        return id;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public ProcessType getProcessType() {
        return processType;
    }

    public Network getNetwork() {
        return network;
    }

    public MergingViewData getMergingViewData() {
        return mergingViewData;
    }

    public Crac getCracEsPt() {
        return cracEsPt;
    }

    public Crac getCracFrEs() {
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
}

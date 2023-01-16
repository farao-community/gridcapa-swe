/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.api.resource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Type("swe-response")
public class SweResponse {
    @Id
    private final String id;
    private final String ttcDocUrl;

    @JsonCreator
    public SweResponse(@JsonProperty("id") String id,
                       @JsonProperty("ttcDocUrl") String ttcDocUrl) {
        this.id = id;
        this.ttcDocUrl = ttcDocUrl;
    }

    public String getId() {
        return id;
    }

    public String getTtcDocUrl() {
        return ttcDocUrl;
    }
}

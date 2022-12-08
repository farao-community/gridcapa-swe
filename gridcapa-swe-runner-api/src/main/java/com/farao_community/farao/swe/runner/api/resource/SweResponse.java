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
    private final String esFrHighestValidStepUrl;
    private final String esFrLowestInvalidStepUrl;
    private final String esFrCgmesZipUrl;

    @JsonCreator
    public SweResponse(@JsonProperty("id") String id,
                       @JsonProperty("ttcDocUrl") String ttcDocUrl,
                       @JsonProperty("esFrHighestValidStepUrl") String esFrHighestValidStepUrl,
                       @JsonProperty("esFrLowestInvalidStepUrl") String esFrLowestInvalidStepUrl,
                       @JsonProperty("esFrCgmesZipUrl") String esFrCgmesZipUrl) {
        this.id = id;
        this.ttcDocUrl = ttcDocUrl;
        this.esFrHighestValidStepUrl = esFrHighestValidStepUrl;
        this.esFrLowestInvalidStepUrl = esFrLowestInvalidStepUrl;
        this.esFrCgmesZipUrl = esFrCgmesZipUrl;
    }

    public String getId() {
        return id;
    }

    public String getTtcDocUrl() {
        return ttcDocUrl;
    }

    public String getEsFrCgmesZipUrl() {
        return esFrCgmesZipUrl;
    }

    public String getEsFrHighestValidStepUrl() {
        return esFrHighestValidStepUrl;
    }

    public String getEsFrLowestInvalidStepUrl() {
        return esFrLowestInvalidStepUrl;
    }
}

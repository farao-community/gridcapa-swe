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

import java.time.OffsetDateTime;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Type("swe-request")
public class SweRequest {
    @Id
    private final String id;
    private final ProcessType processType;
    private final OffsetDateTime targetProcessDateTime;
    private final SweFileResource coresoSv;
    private final SweFileResource reeEq;
    private final SweFileResource reeSsh;
    private final SweFileResource reeTp;
    private final SweFileResource renEq;
    private final SweFileResource renSsh;
    private final SweFileResource renTp;
    private final SweFileResource rteEq;
    private final SweFileResource rteSsh;
    private final SweFileResource rteTp;
    private final SweFileResource crac;
    private final SweFileResource boundaryEq;
    private final SweFileResource boundaryTp;
    private final SweFileResource glsk;

    @JsonCreator
    public SweRequest(@JsonProperty("id") String id,
                      @JsonProperty("processType") ProcessType processType,
                      @JsonProperty("targetProcessDateTime") OffsetDateTime targetProcessDateTime,
                      @JsonProperty("coresoSv") SweFileResource coresoSv,
                      @JsonProperty("reeEq") SweFileResource reeEq,
                      @JsonProperty("reeSsh") SweFileResource reeSsh,
                      @JsonProperty("reeTp") SweFileResource reeTp,
                      @JsonProperty("renEq") SweFileResource renEq,
                      @JsonProperty("renSsh") SweFileResource renSsh,
                      @JsonProperty("renTp") SweFileResource renTp,
                      @JsonProperty("rteEq") SweFileResource rteEq,
                      @JsonProperty("rteSsh") SweFileResource rteSsh,
                      @JsonProperty("rteTp") SweFileResource rteTp,
                      @JsonProperty("crac") SweFileResource crac,
                      @JsonProperty("boundaryEq") SweFileResource boundaryEq,
                      @JsonProperty("boundaryTp") SweFileResource boundaryTp,
                      @JsonProperty("glsk") SweFileResource glsk) {
        this.id = id;
        this.processType = processType;
        this.targetProcessDateTime = targetProcessDateTime;
        this.coresoSv = coresoSv;
        this.reeEq = reeEq;
        this.reeSsh = reeSsh;
        this.reeTp = reeTp;
        this.renEq = renEq;
        this.renSsh = renSsh;
        this.renTp = renTp;
        this.rteEq = rteEq;
        this.rteSsh = rteSsh;
        this.rteTp = rteTp;
        this.crac = crac;
        this.boundaryEq = boundaryEq;
        this.boundaryTp = boundaryTp;
        this.glsk = glsk;
    }

    public String getId() {
        return id;
    }

    public ProcessType getProcessType() {
        return processType;
    }

    public OffsetDateTime getTargetProcessDateTime() {
        return targetProcessDateTime;
    }

    public SweFileResource getCoresoSv() {
        return coresoSv;
    }

    public SweFileResource getReeEq() {
        return reeEq;
    }

    public SweFileResource getReeSsh() {
        return reeSsh;
    }

    public SweFileResource getReeTp() {
        return reeTp;
    }

    public SweFileResource getRenEq() {
        return renEq;
    }

    public SweFileResource getRenSsh() {
        return renSsh;
    }

    public SweFileResource getRenTp() {
        return renTp;
    }

    public SweFileResource getRteEq() {
        return rteEq;
    }

    public SweFileResource getRteSsh() {
        return rteSsh;
    }

    public SweFileResource getRteTp() {
        return rteTp;
    }

    public SweFileResource getCrac() {
        return crac;
    }

    public SweFileResource getBoundaryEq() {
        return boundaryEq;
    }

    public SweFileResource getBoundaryTp() {
        return boundaryTp;
    }

    public SweFileResource getGlsk() {
        return glsk;
    }
}

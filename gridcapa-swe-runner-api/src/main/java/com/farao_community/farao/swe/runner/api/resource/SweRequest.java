/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.api.resource;

import com.farao_community.farao.gridcapa.task_manager.api.TaskParameterDto;
import com.farao_community.farao.gridcapa_swe_commons.resource.ProcessType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * JSON:API request sent to the SWE runner over the message broker to describe one capacity-calculation execution.
 * It bundles the run identity, target process timestamp, all required input file references and task-manager parameters consumed by the runner application.
 *
 * <p>The main collaborators are {@link SweFileResource}, used for every referenced input artifact,
 * {@link TaskParameterDto}, which carries task-level overrides later interpreted by the runner, and
 * {@link com.farao_community.farao.gridcapa_swe_commons.resource.ProcessType}, which selects the process family handled by the run.
 *
 * <p>{@code currentRunId} identifies the task-manager run checked for interruption before launching the computation.
 * {@code processType} drives process-specific behavior such as the CRAC creation parameter set to use.
 * {@code targetProcessDateTime} is the business timestamp of the network being computed.
 * {@code boundaryEq}/{@code boundaryTp} are the boundary CGMES files merged with the national files, and {@code taskParameterList} contains non-default calculation parameters.
 *
 * @see SweResponse broker response produced for this request
 * @see com.farao_community.farao.swe.runner.app.services.RequestService service deserializing and dispatching this request
 * @see com.farao_community.farao.swe.runner.app.services.FilesService service consuming the referenced input files
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Type("swe-request")
public class SweRequest {
    @Id
    private final String id;
    private final String currentRunId;
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
    private final List<TaskParameterDto> taskParameterList;

    @JsonCreator
    public SweRequest(@JsonProperty("id") String id,
                      @JsonProperty("currentRunId") String currentRunId,
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
                      @JsonProperty("glsk") SweFileResource glsk,
                      @JsonProperty("taskParameterList") List<TaskParameterDto> taskParameterList) {
        this.id = id;
        this.currentRunId = currentRunId;
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
        this.taskParameterList = taskParameterList;
    }

    public String getId() {
        return id;
    }

    public String getCurrentRunId() {
        return currentRunId;
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

    public List<TaskParameterDto> getTaskParameterList() {
        return taskParameterList;
    }
}

/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.swe.runner.app.domain;

/**
 * @author Théo Pascoli {@literal <theo.pascoli at rte-france.com>}
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */

public enum CgmesFileType {
    CORESO_SV("CORESO", "SV"),
    RTE_SSH("RTE", "SSH"),
    RTE_EQ("RTE", "EQ"),
    RTE_TP("RTE", "TP"),
    REE_SSH("REE", "SSH"),
    REE_EQ("REE", "EQ"),
    REE_TP("REE", "TP"),
    REN_SSH("REN", "SSH"),
    REN_EQ("REN", "EQ"),
    REN_TP("REN", "TP");

    private final String tso;
    private final String fileType;

    CgmesFileType(String tso, String fileType) {
        this.tso = tso;
        this.fileType = fileType;
    }

    public String getTso() {
        return tso;
    }

    public String getFileType() {
        return fileType;
    }
}

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
    RTE_SSH(Tso.RTE, "SSH"),
    RTE_EQ(Tso.RTE, "EQ"),
    RTE_TP(Tso.RTE, "TP"),
    REE_SSH(Tso.REE, "SSH"),
    REE_EQ(Tso.REE, "EQ"),
    REE_TP(Tso.REE, "TP"),
    REN_SSH(Tso.REN, "SSH"),
    REN_EQ(Tso.REN, "EQ"),
    REN_TP(Tso.REN, "TP");

    private static class Tso {
        private static final String RTE = "RTEFRANCE";
        private static final String REE = "REE";
        private static final String REN = "REN";
    }

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

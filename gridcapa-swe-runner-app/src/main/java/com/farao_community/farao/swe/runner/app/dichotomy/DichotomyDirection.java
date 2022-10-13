/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public enum DichotomyDirection {
    ES_FR("ES-FR"),
    FR_ES("FR-ES"),
    ES_PT("ES-PT"),
    PT_ES("PT-ES");

    private final String direction;

    DichotomyDirection(String direction) {
        this.direction = direction;
    }

    public String getDirection() {
        return direction;
    }
}

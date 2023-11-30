/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_swe_commons.resource;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */
public enum ProcessType {
    D2CC("2D"),
    IDCC("ID"),
    IDCC_IDCF("IDCF");

    private final String code;

    ProcessType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

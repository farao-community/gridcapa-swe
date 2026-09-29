/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_swe_commons.resource;

/**
 * Identifies which SWE capacity-calculation flavour is being processed so downstream components can pick the proper dispatch and target-exchange logic.
 * {@link com.farao_community.farao.gridcapa_swe_commons.shift.SweNetworkShifter} branches on it to compute border targets, and runner-side providers use it to choose between {@link com.farao_community.farao.gridcapa_swe_commons.shift.SweD2ccShiftDispatcher} and {@link com.farao_community.farao.gridcapa_swe_commons.shift.SweIdccShiftDispatcher}.
 * {@code code} is the external short label written in requests and exports ({@code 2D}, {@code 1D}, {@code IDCF}, {@code BTCC}).
 *
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */
public enum ProcessType {
    D2CC("2D"),
    IDCC("1D"),
    IDCC_IDCF("IDCF"),
    BTCC("BTCC");

    private final String code;

    ProcessType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

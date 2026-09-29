/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_swe_commons.dichotomy;

/**
 * Enumerates the four directed border exchanges solved by the SWE dichotomy, distinguishing the ES-FR and ES-PT capacities in both directions.
 * {@link com.farao_community.farao.gridcapa_swe_commons.shift.SweD2ccShiftDispatcher} and {@link com.farao_community.farao.gridcapa_swe_commons.shift.SweIdccShiftDispatcher} translate each value into country scaling signs, and reporting code reuses {@code dashName} and {@code shortName} to label outputs consistently.
 *
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public enum DichotomyDirection {
    ES_FR("ES-FR", "ESFR"),
    FR_ES("FR-ES", "FRES"),
    ES_PT("ES-PT", "ESPT"),
    PT_ES("PT-ES", "PTES");

    private final String dashName;
    private final String shortName;

    DichotomyDirection(String dashName, String shortName) {
        this.dashName = dashName;
        this.shortName = shortName;
    }

    public String getDashName() {
        return dashName;
    }

    public String getShortName() {
        return shortName;
    }
}

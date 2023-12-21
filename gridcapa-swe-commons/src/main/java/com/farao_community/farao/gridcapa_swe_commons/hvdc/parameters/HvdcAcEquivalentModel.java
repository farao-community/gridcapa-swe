/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_swe_commons.hvdc.parameters;

import com.powsybl.iidm.network.TwoSides;

import java.util.Map;
import java.util.Objects;

/**
 * Parameters of an HVDC equivalent AC model
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class HvdcAcEquivalentModel {
    private Map<TwoSides, String> generatorIds;
    private Map<TwoSides, String> loadIds;
    private String acLineId;

    public HvdcAcEquivalentModel(Map<TwoSides, String> generatorIds, Map<TwoSides, String> loadIds, String acLineId) {
        Objects.requireNonNull(generatorIds);
        Objects.requireNonNull(generatorIds.get(TwoSides.ONE));
        Objects.requireNonNull(generatorIds.get(TwoSides.TWO));
        Objects.requireNonNull(loadIds);
        Objects.requireNonNull(loadIds.get(TwoSides.ONE));
        Objects.requireNonNull(loadIds.get(TwoSides.TWO));
        Objects.requireNonNull(acLineId);

        this.generatorIds = generatorIds;
        this.loadIds = loadIds;
        this.acLineId = acLineId;
    }

    public String getGeneratorId(TwoSides side) {
        return generatorIds.get(side);
    }

    public String getLoadId(TwoSides side) {
        return loadIds.get(side);
    }

    public String getAcLineId() {
        return acLineId;
    }
}

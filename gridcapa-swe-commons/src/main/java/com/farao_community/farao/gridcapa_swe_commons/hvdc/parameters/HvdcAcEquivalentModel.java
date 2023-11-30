/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_swe_commons.hvdc.parameters;

import com.powsybl.iidm.network.HvdcLine;

import java.util.Map;
import java.util.Objects;

/**
 * Parameters of an HVDC equivalent AC model
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class HvdcAcEquivalentModel {
    private Map<HvdcLine.Side, String> generatorIds;
    private Map<HvdcLine.Side, String> loadIds;
    private String acLineId;

    public HvdcAcEquivalentModel(Map<HvdcLine.Side, String> generatorIds, Map<HvdcLine.Side, String> loadIds, String acLineId) {
        Objects.requireNonNull(generatorIds);
        Objects.requireNonNull(generatorIds.get(HvdcLine.Side.ONE));
        Objects.requireNonNull(generatorIds.get(HvdcLine.Side.TWO));
        Objects.requireNonNull(loadIds);
        Objects.requireNonNull(loadIds.get(HvdcLine.Side.ONE));
        Objects.requireNonNull(loadIds.get(HvdcLine.Side.TWO));
        Objects.requireNonNull(acLineId);

        this.generatorIds = generatorIds;
        this.loadIds = loadIds;
        this.acLineId = acLineId;
    }

    public String getGeneratorId(HvdcLine.Side side) {
        return generatorIds.get(side);
    }

    public String getLoadId(HvdcLine.Side side) {
        return loadIds.get(side);
    }

    public String getAcLineId() {
        return acLineId;
    }
}

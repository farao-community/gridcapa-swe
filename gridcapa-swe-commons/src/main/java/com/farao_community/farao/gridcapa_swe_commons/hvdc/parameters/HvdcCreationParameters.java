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
 * Parameters for creating an HVDC line and replacing an existing equivalent AC model
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class HvdcCreationParameters {
    private String id;
    private double maxP;
    private double r;
    private double nominalV;
    private Map<TwoSides, VscStationCreationParameters> vscCreationParameters;
    private AngleDroopActivePowerControlParameters angleDroopActivePowerControlParameters;
    private HvdcAcEquivalentModel hvdcAcEquivalentModel;

    public HvdcCreationParameters(String id, Double maxP, Double r, Double nominalV, Map<TwoSides, VscStationCreationParameters> vscCreationParameters, AngleDroopActivePowerControlParameters angleDroopActivePowerControlParameters, HvdcAcEquivalentModel hvdcAcEquivalentModel) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(maxP);
        Objects.requireNonNull(r);
        Objects.requireNonNull(nominalV);
        Objects.requireNonNull(vscCreationParameters);
        Objects.requireNonNull(vscCreationParameters.get(TwoSides.ONE));
        Objects.requireNonNull(vscCreationParameters.get(TwoSides.TWO));
        Objects.requireNonNull(hvdcAcEquivalentModel);

        this.id = id;
        this.maxP = maxP;
        this.r = r;
        this.nominalV = nominalV;
        this.vscCreationParameters = vscCreationParameters;
        this.angleDroopActivePowerControlParameters = angleDroopActivePowerControlParameters;
        this.hvdcAcEquivalentModel = hvdcAcEquivalentModel;
    }

    public String getEquivalentGeneratorId(TwoSides side) {
        return hvdcAcEquivalentModel.getGeneratorId(side);
    }

    public String getEquivalentLoadId(TwoSides side) {
        return hvdcAcEquivalentModel.getLoadId(side);
    }

    public String getEquivalentAcLineId() {
        return hvdcAcEquivalentModel.getAcLineId();
    }

    public VscStationCreationParameters getVscCreationParameters(TwoSides side) {
        return vscCreationParameters.get(side);
    }

    public String getId() {
        return id;
    }

    public double getMaxP() {
        return maxP;
    }

    public double getR() {
        return r;
    }

    public double getNominalV() {
        return nominalV;
    }

    public AngleDroopActivePowerControlParameters getAngleDroopActivePowerControlParameters() {
        return angleDroopActivePowerControlParameters;
    }
}

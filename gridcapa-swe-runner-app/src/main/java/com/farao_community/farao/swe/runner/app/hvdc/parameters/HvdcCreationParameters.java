/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.swe.runner.app.hvdc.parameters;

import com.powsybl.iidm.network.HvdcLine;

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
    private Map<HvdcLine.Side, VscStationCreationParameters> vscCreationParameters;
    private AngleDroopActivePowerControlParameters angleDroopActivePowerControlParameters;
    private HvdcAcEquivalentModel hvdcAcEquivalentModel;

    public HvdcCreationParameters(String id, Double maxP, Double r, Double nominalV, Map<HvdcLine.Side, VscStationCreationParameters> vscCreationParameters, AngleDroopActivePowerControlParameters angleDroopActivePowerControlParameters, HvdcAcEquivalentModel hvdcAcEquivalentModel) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(maxP);
        Objects.requireNonNull(r);
        Objects.requireNonNull(nominalV);
        Objects.requireNonNull(vscCreationParameters);
        Objects.requireNonNull(vscCreationParameters.get(HvdcLine.Side.ONE));
        Objects.requireNonNull(vscCreationParameters.get(HvdcLine.Side.TWO));
        Objects.requireNonNull(hvdcAcEquivalentModel);

        this.id = id;
        this.maxP = maxP;
        this.r = r;
        this.nominalV = nominalV;
        this.vscCreationParameters = vscCreationParameters;
        this.angleDroopActivePowerControlParameters = angleDroopActivePowerControlParameters;
        this.hvdcAcEquivalentModel = hvdcAcEquivalentModel;
    }

    public String getEquivalentGeneratorId(HvdcLine.Side side) {
        return hvdcAcEquivalentModel.getGeneratorId(side);
    }

    public String getEquivalentLoadId(HvdcLine.Side side) {
        return hvdcAcEquivalentModel.getLoadId(side);
    }

    public String getEquivalentAcLineId() {
        return hvdcAcEquivalentModel.getAcLineId();
    }

    public VscStationCreationParameters getVscCreationParameters(HvdcLine.Side side) {
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

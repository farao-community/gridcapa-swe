/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_swe_commons.hvdc.parameters;

import java.util.Objects;

/**
 * Parameters for creating a VSC converter station
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class VscStationCreationParameters {
    private String id;
    private double reactivePowerSetpoint;
    private float lossFactor;
    private boolean voltageRegulatorOn;
    private double defaultVoltageSetpoint;

    public VscStationCreationParameters(String id, Double reactivePowerSetpoint, Float lossFactor, Boolean voltageRegulatorOn, Double defaultVoltageSetpoint) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(reactivePowerSetpoint);
        Objects.requireNonNull(lossFactor);
        Objects.requireNonNull(voltageRegulatorOn);
        Objects.requireNonNull(defaultVoltageSetpoint);

        this.id = id;
        this.reactivePowerSetpoint = reactivePowerSetpoint;
        this.lossFactor = lossFactor;
        this.voltageRegulatorOn = voltageRegulatorOn;
        this.defaultVoltageSetpoint = defaultVoltageSetpoint;
    }

    public String getId() {
        return id;
    }

    public double getReactivePowerSetpoint() {
        return reactivePowerSetpoint;
    }

    public float getLossFactor() {
        return lossFactor;
    }

    public boolean isVoltageRegulatorOn() {
        return voltageRegulatorOn;
    }

    public double getDefaultVoltageSetpoint() {
        return defaultVoltageSetpoint;
    }
}

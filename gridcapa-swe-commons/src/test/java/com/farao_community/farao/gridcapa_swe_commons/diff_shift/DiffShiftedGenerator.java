/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_swe_commons.diff_shift;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public class DiffShiftedGenerator {

    private final ScalableInformation scalableInformation;
    private final GeneratorInformation initialInformation;

    private final GeneratorInformation shiftedInformation;

    public DiffShiftedGenerator(ScalableInformation scalableInformation, GeneratorInformation initialInformation, GeneratorInformation shiftedInformation) {
        this.scalableInformation = scalableInformation;
        this.initialInformation = initialInformation;
        this.shiftedInformation = shiftedInformation;
    }

    public boolean hasDifferentTargetP() {
        return initialInformation.getTargetP() != shiftedInformation.getTargetP();
    }

    public boolean hasDifferentConnectionStatus() {
        return initialInformation.hasDifferentStatus(shiftedInformation);
    }

    public String displayDiffGenerator() {
        return scalableInformation.toString() + ";" + initialInformation.displayWithoutTwt() + ";" + shiftedInformation.displayWithoutTwt();
    }

    public String displayDiffGeneratorWithTwt() {
        return scalableInformation.toString() + ";" + initialInformation.displayWithoutTwt() + ";" + shiftedInformation.displayWithoutTwt() + ";" + shiftedInformation.listTwtIds();
    }
}

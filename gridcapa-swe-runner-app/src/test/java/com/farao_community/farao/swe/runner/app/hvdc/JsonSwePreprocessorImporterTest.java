/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.swe.runner.app.hvdc;

import com.farao_community.farao.swe.runner.app.hvdc.parameters.AngleDroopActivePowerControlParameters;
import com.farao_community.farao.swe.runner.app.hvdc.parameters.HvdcCreationParameters;
import com.farao_community.farao.swe.runner.app.hvdc.parameters.SwePreprocessorParameters;
import com.farao_community.farao.swe.runner.app.hvdc.parameters.VscStationCreationParameters;
import com.farao_community.farao.swe.runner.app.hvdc.parameters.json.JsonSwePreprocessorImporter;
import com.powsybl.iidm.network.HvdcLine;
import org.junit.jupiter.api.Test;

import java.io.UncheckedIOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri@rte-france.com>}
 */
class JsonSwePreprocessorImporterTest {
    private static final double DOUBLE_TOLERANCE = 1e-6;

    @Test
    void testImportNok() {
        assertThrows(UncheckedIOException.class, () -> JsonSwePreprocessorImporter.read(getClass().getResourceAsStream("/hvdc/SwePreprocessorParameters_error0.json")));
        assertThrows(UncheckedIOException.class, () -> JsonSwePreprocessorImporter.read(getClass().getResourceAsStream("/hvdc/SwePreprocessorParameters_error1.json")));
        assertThrows(UncheckedIOException.class, () -> JsonSwePreprocessorImporter.read(getClass().getResourceAsStream("/hvdc/SwePreprocessorParameters_error2.json")));
        assertThrows(UncheckedIOException.class, () -> JsonSwePreprocessorImporter.read(getClass().getResourceAsStream("/hvdc/SwePreprocessorParameters_error3.json")));
        assertThrows(UncheckedIOException.class, () -> JsonSwePreprocessorImporter.read(getClass().getResourceAsStream("/hvdc/SwePreprocessorParameters_error4.json")));
    }

    @Test
    void testImport() {
        SwePreprocessorParameters params = JsonSwePreprocessorImporter.read(getClass().getResourceAsStream("/hvdc/SwePreprocessorParameters.json"));
        assertNotNull(params);
        assertNotNull(params.getHvdcCreationParametersSet());
        assertEquals(2, params.getHvdcCreationParametersSet().size());

        HvdcCreationParameters hvdc1 = params.getHvdcCreationParametersSet().stream().filter(cp -> cp.getId().equals("HVDC1")).findAny().orElseThrow();
        assertEquals(1000, hvdc1.getMaxP(), DOUBLE_TOLERANCE);
        assertEquals(.5, hvdc1.getR(), DOUBLE_TOLERANCE);
        assertEquals(400, hvdc1.getNominalV(), DOUBLE_TOLERANCE);
        inspectVscStationCreationParameters(hvdc1.getVscCreationParameters(HvdcLine.Side.ONE), "HVDC1_VSC1", 0., 1f, false, 400.);
        inspectVscStationCreationParameters(hvdc1.getVscCreationParameters(HvdcLine.Side.TWO), "HVDC1_VSC2", 20., 10f, true, 380.);
        inspectAngleDroopActivePowerControlParameters(hvdc1.getAngleDroopActivePowerControlParameters(), 0f, 180f);
        inspectHvdcAcEquivalentModel(
            hvdc1,
            Map.of(HvdcLine.Side.ONE, "HVDC1_GEN1", HvdcLine.Side.TWO, "HVDC1_GEN2"),
            Map.of(HvdcLine.Side.ONE, "HVDC1_LOAD1", HvdcLine.Side.TWO, "HVDC1_LOAD2"),
            "HVDC1_ACLINE"
        );

        HvdcCreationParameters hvdc2 = params.getHvdcCreationParametersSet().stream().filter(cp -> cp.getId().equals("HVDC2")).findAny().orElseThrow();
        assertEquals(200, hvdc2.getMaxP(), DOUBLE_TOLERANCE);
        assertEquals(.75, hvdc2.getR(), DOUBLE_TOLERANCE);
        assertEquals(220, hvdc2.getNominalV(), DOUBLE_TOLERANCE);
        inspectVscStationCreationParameters(hvdc2.getVscCreationParameters(HvdcLine.Side.ONE), "HVDC2_VSC1", 5., 2f, false, 100.);
        inspectVscStationCreationParameters(hvdc2.getVscCreationParameters(HvdcLine.Side.TWO), "HVDC2_VSC2", -10., -1f, true, 440.);
        inspectAngleDroopActivePowerControlParameters(hvdc2.getAngleDroopActivePowerControlParameters(), 10f, 270f);
        inspectHvdcAcEquivalentModel(
            hvdc2,
            Map.of(HvdcLine.Side.ONE, "HVDC2_GEN1", HvdcLine.Side.TWO, "HVDC2_GEN2"),
            Map.of(HvdcLine.Side.ONE, "HVDC2_LOAD1", HvdcLine.Side.TWO, "HVDC2_LOAD2"),
            "HVDC2_ACLINE"
        );
    }

    private void inspectVscStationCreationParameters(VscStationCreationParameters vscStationCreationParameters, String id, Double reactivePowerSetpoint, Float lossFactor, Boolean voltageRegulatorOn, Double defaultVoltageSetpoint) {
        assertEquals(id, vscStationCreationParameters.getId());
        assertEquals(reactivePowerSetpoint, vscStationCreationParameters.getReactivePowerSetpoint(), DOUBLE_TOLERANCE);
        assertEquals(lossFactor, vscStationCreationParameters.getLossFactor(), DOUBLE_TOLERANCE);
        assertEquals(voltageRegulatorOn, vscStationCreationParameters.isVoltageRegulatorOn());
        assertEquals(defaultVoltageSetpoint, vscStationCreationParameters.getDefaultVoltageSetpoint(), DOUBLE_TOLERANCE);
    }

    private void inspectAngleDroopActivePowerControlParameters(AngleDroopActivePowerControlParameters angleDroopActivePowerControlParameters, Float p0, Float droop) {
        assertEquals(p0, angleDroopActivePowerControlParameters.getP0(), DOUBLE_TOLERANCE);
        assertEquals(droop, angleDroopActivePowerControlParameters.getDroop(), DOUBLE_TOLERANCE);
    }

    private void inspectHvdcAcEquivalentModel(HvdcCreationParameters hvdcCreationParameters, Map<HvdcLine.Side, String> generatorIds, Map<HvdcLine.Side, String> loadIds, String acLineId) {
        assertEquals(generatorIds.get(HvdcLine.Side.ONE), hvdcCreationParameters.getEquivalentGeneratorId(HvdcLine.Side.ONE));
        assertEquals(generatorIds.get(HvdcLine.Side.TWO), hvdcCreationParameters.getEquivalentGeneratorId(HvdcLine.Side.TWO));
        assertEquals(loadIds.get(HvdcLine.Side.ONE), hvdcCreationParameters.getEquivalentLoadId(HvdcLine.Side.ONE));
        assertEquals(loadIds.get(HvdcLine.Side.TWO), hvdcCreationParameters.getEquivalentLoadId(HvdcLine.Side.TWO));
        assertEquals(acLineId, hvdcCreationParameters.getEquivalentAcLineId());
    }
}

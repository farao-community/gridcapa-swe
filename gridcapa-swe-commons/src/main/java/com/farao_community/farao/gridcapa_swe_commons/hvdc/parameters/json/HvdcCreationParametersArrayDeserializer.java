/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_swe_commons.hvdc.parameters.json;

import com.farao_community.farao.gridcapa_swe_commons.hvdc.parameters.AngleDroopActivePowerControlParameters;
import com.farao_community.farao.gridcapa_swe_commons.hvdc.parameters.HvdcAcEquivalentModel;
import com.farao_community.farao.gridcapa_swe_commons.hvdc.parameters.HvdcCreationParameters;
import com.farao_community.farao.gridcapa_swe_commons.hvdc.parameters.VscStationCreationParameters;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.powsybl.iidm.network.TwoSides;
import org.apache.commons.math3.util.Pair;

import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class HvdcCreationParametersArrayDeserializer {

    private static final String ID = "id";
    private static final String MAX_P = "maxP";
    private static final String R = "r";
    private static final String NOMINAL_V = "nominalV";
    private static final String VSC_PARAMS = "vscCreationParameters";
    private static final String ANGLE_DROOP_CONTROL_PARAMS = "angleDroopActivePowerControlParameters";
    private static final String HVDC_EQUIVALENT_MODEL = "hvdcAcEquivalentModel";

    private HvdcCreationParametersArrayDeserializer() {
        // should not be instantiated
    }

    static Set<HvdcCreationParameters> deserialize(JsonParser jsonParser) throws IOException, NoSuchFieldException {
        Set<HvdcCreationParameters> hvdcCreationParametersSet = new HashSet<>();

        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            String id = null;
            Double maxP = null;
            Double r = null;
            Double nominalV = null;
            Map<TwoSides, VscStationCreationParameters> vscCreationParameters = new EnumMap<>(TwoSides.class);
            AngleDroopActivePowerControlParameters angleDroopActivePowerControlParameters = null;
            HvdcAcEquivalentModel hvdcAcEquivalentModel = null;

            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case ID:
                        id = jsonParser.nextTextValue();
                        break;
                    case MAX_P:
                        jsonParser.nextToken();
                        maxP = jsonParser.getDoubleValue();
                        break;
                    case R:
                        jsonParser.nextToken();
                        r = jsonParser.getDoubleValue();
                        break;
                    case NOMINAL_V:
                        jsonParser.nextToken();
                        nominalV = jsonParser.getDoubleValue();
                        break;
                    case VSC_PARAMS:
                        jsonParser.nextToken();
                        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
                            Pair<TwoSides, VscStationCreationParameters> result = VscStationCreationParametersDeserializer.deserialize(jsonParser);
                            vscCreationParameters.put(result.getKey(), result.getValue());
                        }
                        break;
                    case ANGLE_DROOP_CONTROL_PARAMS:
                        jsonParser.nextToken();
                        angleDroopActivePowerControlParameters = AngleDroopActivePowerControlParametersDeserializer.deserialize(jsonParser);
                        break;
                    case HVDC_EQUIVALENT_MODEL:
                        jsonParser.nextToken();
                        hvdcAcEquivalentModel = HvdcAcEquivalentModelDeserializer.deserialize(jsonParser);
                        break;
                    default:
                        throw new NoSuchFieldException("Unexpected field in HvdcCreationParameters: " + jsonParser.getCurrentName());
                }
            }

            hvdcCreationParametersSet.add(
                    new HvdcCreationParameters(id, maxP, r, nominalV, vscCreationParameters, angleDroopActivePowerControlParameters, hvdcAcEquivalentModel)
            );
        }

        return hvdcCreationParametersSet;
    }

    private static class VscStationCreationParametersDeserializer {
        private static final String SIDE = "side";
        private static final String ID = "id";
        private static final String REACTIVE_P_SETPOINT = "reactivePowerSetpoint";
        private static final String LOSS_FACTOR = "lossFactor";
        private static final String VOLTAGE_REGULATOR_ON = "voltageRegulatorOn";
        private static final String DEFAULT_VOLTAGE_SETPOINT = "defaultVoltageSetpoint";

        static Pair<TwoSides, VscStationCreationParameters> deserialize(JsonParser jsonParser) throws IOException, NoSuchFieldException {
            TwoSides side = null;
            String id = null;
            Double reactivePowerSetpoint = null;
            Float lossFactor = null;
            Boolean voltageRegulatorOn = null;
            Double defaultVoltageSetpoint = null;

            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case SIDE:
                        int sideInt = jsonParser.nextIntValue(0);
                        if (sideInt == 1) {
                            side = TwoSides.ONE;
                        } else if (sideInt == 2) {
                            side = TwoSides.TWO;
                        } else {
                            throw new IllegalArgumentException("VscStationCreationParameters Side must be 1 or 2");
                        }
                        break;
                    case ID:
                        id = jsonParser.nextTextValue();
                        break;
                    case REACTIVE_P_SETPOINT:
                        jsonParser.nextToken();
                        reactivePowerSetpoint = jsonParser.getValueAsDouble();
                        break;
                    case LOSS_FACTOR:
                        jsonParser.nextToken();
                        lossFactor = jsonParser.getFloatValue();
                        break;
                    case VOLTAGE_REGULATOR_ON:
                        voltageRegulatorOn = jsonParser.nextBooleanValue();
                        break;
                    case DEFAULT_VOLTAGE_SETPOINT:
                        jsonParser.nextToken();
                        defaultVoltageSetpoint = jsonParser.getDoubleValue();
                        break;
                    default:
                        throw new NoSuchFieldException("Unexpected field in VscStationCreationParameters: " + jsonParser.getCurrentName());
                }
            }
            return Pair.create(side, new VscStationCreationParameters(id, reactivePowerSetpoint, lossFactor, voltageRegulatorOn, defaultVoltageSetpoint));
        }
    }

    private static class AngleDroopActivePowerControlParametersDeserializer {
        private static final String P0 = "p0";
        private static final String DROOP = "droop";

        static AngleDroopActivePowerControlParameters deserialize(JsonParser jsonParser) throws IOException, NoSuchFieldException {
            Float p0 = null;
            Float droop = null;
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case P0:
                        jsonParser.nextToken();
                        p0 = jsonParser.getFloatValue();
                        break;
                    case DROOP:
                        jsonParser.nextToken();
                        droop = jsonParser.getFloatValue();
                        break;
                    default:
                        throw new NoSuchFieldException("Unexpected field in AngleDroopActivePowerControlParameters: " + jsonParser.getCurrentName());
                }
            }
            return new AngleDroopActivePowerControlParameters(p0, droop);
        }
    }

    /*
     following a coreso request temporarily the side 1 of the load can have 2 ids
     if the first id SIDE_1_LOAD_ID does not exist in the network
     we look for the second id SIDE_1_LOAD_ID_OPTION_2 in the network
     */
    private static class HvdcAcEquivalentModelDeserializer {
        private static final String SIDE_1_GEN_ID = "side1GeneratorId";
        private static final String SIDE_2_GEN_ID = "side2GeneratorId";
        private static final String SIDE_1_LOAD_ID = "side1LoadID";
        private static final String SIDE_1_LOAD_ID_OPTION_2 = "side1LoadIDOption2";
        private static final String SIDE_2_LOAD_ID = "side2LoadId";
        private static final String AC_LINE_ID = "acLineId";

        static HvdcAcEquivalentModel deserialize(JsonParser jsonParser) throws IOException, NoSuchFieldException {
            Map<TwoSides, String> generatorIds = new EnumMap<>(TwoSides.class);
            Map<TwoSides, Map<Integer, String>> loadIds = new EnumMap<>(TwoSides.class);
            Map<Integer, String> idsByPriority = new HashMap<>();
            String acLineId = null;
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case SIDE_1_GEN_ID:
                        generatorIds.put(TwoSides.ONE, jsonParser.nextTextValue());
                        break;
                    case SIDE_2_GEN_ID:
                        generatorIds.put(TwoSides.TWO, jsonParser.nextTextValue());
                        break;
                    case SIDE_1_LOAD_ID:
                        idsByPriority.put(1, jsonParser.nextTextValue());
                        break;
                    case SIDE_1_LOAD_ID_OPTION_2:
                        idsByPriority.put(2, jsonParser.nextTextValue());
                        break;
                    case SIDE_2_LOAD_ID:
                        loadIds.put(TwoSides.TWO, Map.of(1, jsonParser.nextTextValue()));
                        break;
                    case AC_LINE_ID:
                        acLineId = jsonParser.nextTextValue();
                        break;
                    default:
                        throw new NoSuchFieldException("Unexpected field in HvdcAcEquivalentModel: " + jsonParser.getCurrentName());
                }
            }
            loadIds.put(TwoSides.ONE, idsByPriority);
            return new HvdcAcEquivalentModel(generatorIds, loadIds, acLineId);
        }
    }

}

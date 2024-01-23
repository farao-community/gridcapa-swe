/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.domain;

import com.farao_community.farao.gridcapa.task_manager.api.TaskParameterDto;
import com.farao_community.farao.gridcapa_swe_commons.exception.SweInvalidDataException;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class SweTaskParameters {

    private static final String IS_RUN_ES_FR = "RUN_ES-FR";
    private static final String IS_RUN_FR_ES = "RUN_FR-ES";
    private static final String IS_RUN_ES_PT = "RUN_ES-PT";
    private static final String IS_RUN_PT_ES = "RUN_PT-ES";
    private static final String STARTING_POINT_ES_FR = "STARTING_POINT_ES-FR";
    private static final String STARTING_POINT_FR_ES = "STARTING_POINT_FR-ES";
    private static final String STARTING_POINT_ES_PT = "STARTING_POINT_ES-PT";
    private static final String STARTING_POINT_PT_ES = "STARTING_POINT_PT-ES";
    private static final String MIN_POINT_ES_FR = "MIN_POINT_ES-FR";
    private static final String MIN_POINT_FR_ES = "MIN_POINT_FR-ES";
    private static final String MIN_POINT_ES_PT = "MIN_POINT_ES-PT";
    private static final String MIN_POINT_PT_ES = "MIN_POINT_PT-ES";
    private static final String SENSITIVITY_ES_FR = "SENSITIVITY_ES-FR";
    private static final String SENSITIVITY_FR_ES = "SENSITIVITY_FR-ES";
    private static final String SENSITIVITY_ES_PT = "SENSITIVITY_ES-PT";
    private static final String SENSITIVITY_PT_ES = "SENSITIVITY_PT-ES";
    private static final String RUN_ANGLE_CHECK = "RUN_ANGLE_CHECK";
    private static final String RUN_VOLTAGE_CHECK = "RUN_VOLTAGE_CHECK";
    private static final String MAX_CRA = "MAX_CRA";

    private boolean runDirectionEsFr;
    private boolean runDirectionFrEs;
    private boolean runDirectionEsPt;
    private boolean runDirectionPtEs;
    private int startingPointEsFr;
    private int startingPointFrEs;
    private int startingPointEsPt;
    private int startingPointPtEs;
    private int minPointEsFr;
    private int minPointFrEs;
    private int minPointEsPt;
    private int minPointPtEs;
    private int sensitivityEsFr;
    private int sensitivityFrEs;
    private int sensitivityEsPt;
    private int sensitivityPtEs;
    private boolean runAngleCheck;
    private boolean runVoltageCheck;
    private int maxCra;

    public SweTaskParameters(List<TaskParameterDto> parameters) {
        for (TaskParameterDto parameter : parameters) {
            switch (parameter.getId()) {
                case IS_RUN_ES_FR -> runDirectionEsFr = validateIsBooleanAndGet(parameter);
                case IS_RUN_FR_ES -> runDirectionFrEs = validateIsBooleanAndGet(parameter);
                case IS_RUN_ES_PT -> runDirectionEsPt = validateIsBooleanAndGet(parameter);
                case IS_RUN_PT_ES -> runDirectionPtEs = validateIsBooleanAndGet(parameter);
                case STARTING_POINT_ES_FR -> startingPointEsFr = validateIsIntegerAndGet(parameter);
                case STARTING_POINT_FR_ES -> startingPointFrEs = validateIsIntegerAndGet(parameter);
                case STARTING_POINT_ES_PT -> startingPointEsPt = validateIsIntegerAndGet(parameter);
                case STARTING_POINT_PT_ES -> startingPointPtEs = validateIsIntegerAndGet(parameter);
                case MIN_POINT_ES_FR -> minPointEsFr = validateIsIntegerAndGet(parameter);
                case MIN_POINT_FR_ES -> minPointFrEs = validateIsIntegerAndGet(parameter);
                case MIN_POINT_ES_PT -> minPointEsPt = validateIsIntegerAndGet(parameter);
                case MIN_POINT_PT_ES -> minPointPtEs = validateIsIntegerAndGet(parameter);
                case SENSITIVITY_ES_FR -> sensitivityEsFr = validateIsIntegerAndGet(parameter);
                case SENSITIVITY_FR_ES -> sensitivityFrEs = validateIsIntegerAndGet(parameter);
                case SENSITIVITY_ES_PT -> sensitivityEsPt = validateIsIntegerAndGet(parameter);
                case SENSITIVITY_PT_ES -> sensitivityPtEs = validateIsIntegerAndGet(parameter);
                case RUN_ANGLE_CHECK -> runAngleCheck = validateIsBooleanAndGet(parameter);
                case RUN_VOLTAGE_CHECK -> runVoltageCheck = validateIsBooleanAndGet(parameter);
                case MAX_CRA -> maxCra = validateIsIntegerAndGet(parameter);
                default -> {
                    //do nothing
                }
            }
        }
    }

    private boolean validateIsBooleanAndGet(TaskParameterDto parameter) {
        if (StringUtils.equals("BOOLEAN", parameter.getParameterType())) {
            return Boolean.parseBoolean(parameter.getValue());
        } else {
            throw new SweInvalidDataException(String.format("Invalid boolean parameter with id %s and value %s", parameter.getId(), parameter.getValue()));
        }
    }

    private int validateIsIntegerAndGet(TaskParameterDto parameter) {
        if (StringUtils.equals("INT", parameter.getParameterType())) {
            return Integer.parseInt(parameter.getValue());
        } else {
            throw new SweInvalidDataException(String.format("Invalid integer parameter with id %s and value %s", parameter.getId(), parameter.getValue()));
        }
    }

    public boolean isRunDirectionEsFr() {
        return runDirectionEsFr;
    }

    public boolean isRunDirectionFrEs() {
        return runDirectionFrEs;
    }

    public boolean isRunDirectionEsPt() {
        return runDirectionEsPt;
    }

    public boolean isRunDirectionPtEs() {
        return runDirectionPtEs;
    }

    public int getStartingPointEsFr() {
        return startingPointEsFr;
    }

    public int getStartingPointFrEs() {
        return startingPointFrEs;
    }

    public int getStartingPointEsPt() {
        return startingPointEsPt;
    }

    public int getStartingPointPtEs() {
        return startingPointPtEs;
    }

    public int getMinPointEsFr() {
        return minPointEsFr;
    }

    public int getMinPointFrEs() {
        return minPointFrEs;
    }

    public int getMinPointEsPt() {
        return minPointEsPt;
    }

    public int getMinPointPtEs() {
        return minPointPtEs;
    }

    public int getSensitivityEsFr() {
        return sensitivityEsFr;
    }

    public int getSensitivityFrEs() {
        return sensitivityFrEs;
    }

    public int getSensitivityEsPt() {
        return sensitivityEsPt;
    }

    public int getSensitivityPtEs() {
        return sensitivityPtEs;
    }

    public boolean isRunAngleCheck() {
        return runAngleCheck;
    }

    public boolean isRunVoltageCheck() {
        return runVoltageCheck;
    }

    public int getMaxCra() {
        return maxCra;
    }
}

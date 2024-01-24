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
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SweTaskParameters {
    private static final Logger LOGGER = LoggerFactory.getLogger(SweTaskParameters.class);

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
    private int maxNewtonRaphsonIterations;

    public SweTaskParameters(List<TaskParameterDto> parameters) {
        for (TaskParameterDto parameter : parameters) {
            switch (parameter.getId()) {
                case "RUN_ES-FR" -> runDirectionEsFr = validateIsBooleanAndGet(parameter);
                case "RUN_FR-ES" -> runDirectionFrEs = validateIsBooleanAndGet(parameter);
                case "RUN_ES-PT" -> runDirectionEsPt = validateIsBooleanAndGet(parameter);
                case "RUN_PT-ES" -> runDirectionPtEs = validateIsBooleanAndGet(parameter);
                case "STARTING_POINT_ES-FR" -> startingPointEsFr = validateIsIntegerAndGet(parameter);
                case "STARTING_POINT_FR-ES" -> startingPointFrEs = validateIsIntegerAndGet(parameter);
                case "STARTING_POINT_ES-PT" -> startingPointEsPt = validateIsIntegerAndGet(parameter);
                case "STARTING_POINT_PT-ES" -> startingPointPtEs = validateIsIntegerAndGet(parameter);
                case "MIN_POINT_ES-FR" -> minPointEsFr = validateIsIntegerAndGet(parameter);
                case "MIN_POINT_FR-ES" -> minPointFrEs = validateIsIntegerAndGet(parameter);
                case "MIN_POINT_ES-PT" -> minPointEsPt = validateIsIntegerAndGet(parameter);
                case "MIN_POINT_PT-ES" -> minPointPtEs = validateIsIntegerAndGet(parameter);
                case "SENSITIVITY_ES-FR" -> sensitivityEsFr = validateIsIntegerAndGet(parameter);
                case "SENSITIVITY_FR-ES" -> sensitivityFrEs = validateIsIntegerAndGet(parameter);
                case "SENSITIVITY_ES-PT" -> sensitivityEsPt = validateIsIntegerAndGet(parameter);
                case "SENSITIVITY_PT-ES" -> sensitivityPtEs = validateIsIntegerAndGet(parameter);
                case "RUN_ANGLE_CHECK" -> runAngleCheck = validateIsBooleanAndGet(parameter);
                case "RUN_VOLTAGE_CHECK" -> runVoltageCheck = validateIsBooleanAndGet(parameter);
                case "MAX_CRA" -> maxCra = validateIsIntegerAndGet(parameter);
                case "MAX_NEWTON_RAPHSON_ITERATIONS" -> maxNewtonRaphsonIterations = validateIsIntegerAndGet(parameter);
                default -> LOGGER.warn("Unknown parameter {} (value {}) will be ignored", parameter.getId(), parameter.getValue());
            }
        }
    }

    private boolean validateIsBooleanAndGet(TaskParameterDto parameter) {
        if (StringUtils.equals("BOOLEAN", parameter.getParameterType())) {
            String value = parameter.getValue() != null ? parameter.getValue() : parameter.getDefaultValue();
            return Boolean.parseBoolean(value);
        } else {
            throw new SweInvalidDataException(String.format("Invalid boolean parameter with id %s and value %s", parameter.getId(), parameter.getValue()));
        }
    }

    private int validateIsIntegerAndGet(TaskParameterDto parameter) {
        if (StringUtils.equals("INT", parameter.getParameterType())) {
            String value = parameter.getValue() != null ? parameter.getValue() : parameter.getDefaultValue();
            return Integer.parseInt(value);
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

    public int getMaxNewtonRaphsonIterations() {
        return maxNewtonRaphsonIterations;
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    public String toJsonString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }
}

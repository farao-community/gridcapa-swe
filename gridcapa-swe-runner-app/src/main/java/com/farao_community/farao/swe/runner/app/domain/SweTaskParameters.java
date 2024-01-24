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

import java.util.ArrayList;
import java.util.List;

public class SweTaskParameters {
    private static final Logger LOGGER = LoggerFactory.getLogger(SweTaskParameters.class);
    private static final String KEY_VALUE_FORMAT = "%n\t\"%s\": %s";

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
    private static final String MAX_NEWTON_RAPHSON_ITERATIONS = "MAX_NEWTON_RAPHSON_ITERATIONS";

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
        List<String> errors = new ArrayList<>();
        for (TaskParameterDto parameter : parameters) {
            switch (parameter.getId()) {
                case IS_RUN_ES_FR -> runDirectionEsFr = validateIsBooleanAndGet(parameter, errors);
                case IS_RUN_FR_ES -> runDirectionFrEs = validateIsBooleanAndGet(parameter, errors);
                case IS_RUN_ES_PT -> runDirectionEsPt = validateIsBooleanAndGet(parameter, errors);
                case IS_RUN_PT_ES -> runDirectionPtEs = validateIsBooleanAndGet(parameter, errors);
                case STARTING_POINT_ES_FR -> startingPointEsFr = validateIsPositiveIntegerAndGet(parameter, errors);
                case STARTING_POINT_FR_ES -> startingPointFrEs = validateIsPositiveIntegerAndGet(parameter, errors);
                case STARTING_POINT_ES_PT -> startingPointEsPt = validateIsPositiveIntegerAndGet(parameter, errors);
                case STARTING_POINT_PT_ES -> startingPointPtEs = validateIsPositiveIntegerAndGet(parameter, errors);
                case MIN_POINT_ES_FR -> minPointEsFr = validateIsPositiveIntegerAndGet(parameter, errors);
                case MIN_POINT_FR_ES -> minPointFrEs = validateIsPositiveIntegerAndGet(parameter, errors);
                case MIN_POINT_ES_PT -> minPointEsPt = validateIsPositiveIntegerAndGet(parameter, errors);
                case MIN_POINT_PT_ES -> minPointPtEs = validateIsPositiveIntegerAndGet(parameter, errors);
                case SENSITIVITY_ES_FR -> sensitivityEsFr = validateIsPositiveIntegerAndGet(parameter, errors);
                case SENSITIVITY_FR_ES -> sensitivityFrEs = validateIsPositiveIntegerAndGet(parameter, errors);
                case SENSITIVITY_ES_PT -> sensitivityEsPt = validateIsPositiveIntegerAndGet(parameter, errors);
                case SENSITIVITY_PT_ES -> sensitivityPtEs = validateIsPositiveIntegerAndGet(parameter, errors);
                case RUN_ANGLE_CHECK -> runAngleCheck = validateIsBooleanAndGet(parameter, errors);
                case RUN_VOLTAGE_CHECK -> runVoltageCheck = validateIsBooleanAndGet(parameter, errors);
                case MAX_CRA -> maxCra = validateIsPositiveIntegerAndGet(parameter, errors);
                case MAX_NEWTON_RAPHSON_ITERATIONS -> maxNewtonRaphsonIterations = validateIsPositiveIntegerAndGet(parameter, errors);
                default -> LOGGER.warn("Unknown parameter {} (value: {}) will be ignored", parameter.getId(), parameter.getValue());
            }
        }

        crossValidateParameters(errors);

        if (!errors.isEmpty()) {
            String message = String.format("Validation of parameters failed. Failure reasons are: [\"%s\"].", String.join("\" ; \"", errors));
            throw new SweInvalidDataException(message);
        }
    }

    private boolean validateIsBooleanAndGet(TaskParameterDto parameter, List<String> errors) {
        if (StringUtils.equals("BOOLEAN", parameter.getParameterType())) {
            String value = parameter.getValue() != null ? parameter.getValue() : parameter.getDefaultValue();
            return Boolean.parseBoolean(value);
        } else {
            errors.add(String.format("Parameter %s was expected to be of type BOOLEAN, got %s", parameter.getId(), parameter.getParameterType()));
            return false; // default return value, won't be used as this return can be reached only in case of validation error
        }
    }

    private int validateIsIntegerAndGet(TaskParameterDto parameter, List<String> errors) {
        if (StringUtils.equals("INT", parameter.getParameterType())) {
            String value = parameter.getValue() != null ? parameter.getValue() : parameter.getDefaultValue();
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                errors.add(String.format("Parameter %s could not be parsed as integer (value: %s)", parameter.getId(), parameter.getValue()));
            }
        } else {
            errors.add(String.format("Parameter %s was expected to be of type INT, got %s", parameter.getId(), parameter.getParameterType()));
        }
        return 0; // default return value, won't be used as this return can be reached only in case of validation error
    }

    private int validateIsPositiveIntegerAndGet(TaskParameterDto parameter, List<String> errors) {
        int value = validateIsIntegerAndGet(parameter, errors);
        if (value < 0) {
            errors.add(String.format("Parameter %s should be positive (value: %s)", parameter.getId(), parameter.getValue()));
            return 0; // default return value, won't be used as this return can be reached only in case of validation error
        }
        return value;
    }

    private void crossValidateParameters(List<String> errors) {
        if (runDirectionEsFr) {
            validateStartingPointAndMinPoint("ES-FR", startingPointEsFr, minPointEsFr, errors);
        }
        if (runDirectionFrEs) {
            validateStartingPointAndMinPoint("FR-ES", startingPointFrEs, minPointFrEs, errors);
        }
        if (runDirectionEsPt) {
            validateStartingPointAndMinPoint("ES-PT", startingPointEsPt, minPointEsPt, errors);
        }
        if (runDirectionPtEs) {
            validateStartingPointAndMinPoint("PT-ES", startingPointPtEs, minPointPtEs, errors);
        }
    }

    private void validateStartingPointAndMinPoint(String direction, int startingPointValue, int minPointValue, List<String> errors) {
        if (startingPointValue < minPointValue) {
            errors.add(String.format("[%s] Starting point (value: %d) should be greater than minimum point (value: %d)", direction, startingPointValue, minPointValue));
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
        List<String> appender = new ArrayList<>();
        appender.add(String.format(KEY_VALUE_FORMAT, IS_RUN_ES_FR, runDirectionEsFr));
        appender.add(String.format(KEY_VALUE_FORMAT, IS_RUN_FR_ES, runDirectionFrEs));
        appender.add(String.format(KEY_VALUE_FORMAT, IS_RUN_ES_PT, runDirectionEsPt));
        appender.add(String.format(KEY_VALUE_FORMAT, IS_RUN_PT_ES, runDirectionPtEs));
        appender.add(String.format(KEY_VALUE_FORMAT, STARTING_POINT_ES_FR, startingPointEsFr));
        appender.add(String.format(KEY_VALUE_FORMAT, STARTING_POINT_FR_ES, startingPointFrEs));
        appender.add(String.format(KEY_VALUE_FORMAT, STARTING_POINT_ES_PT, startingPointEsPt));
        appender.add(String.format(KEY_VALUE_FORMAT, STARTING_POINT_PT_ES, startingPointPtEs));
        appender.add(String.format(KEY_VALUE_FORMAT, MIN_POINT_ES_FR, minPointEsFr));
        appender.add(String.format(KEY_VALUE_FORMAT, MIN_POINT_FR_ES, minPointFrEs));
        appender.add(String.format(KEY_VALUE_FORMAT, MIN_POINT_ES_PT, minPointEsPt));
        appender.add(String.format(KEY_VALUE_FORMAT, MIN_POINT_PT_ES, minPointPtEs));
        appender.add(String.format(KEY_VALUE_FORMAT, SENSITIVITY_ES_FR, sensitivityEsFr));
        appender.add(String.format(KEY_VALUE_FORMAT, SENSITIVITY_FR_ES, sensitivityFrEs));
        appender.add(String.format(KEY_VALUE_FORMAT, SENSITIVITY_ES_PT, sensitivityEsPt));
        appender.add(String.format(KEY_VALUE_FORMAT, SENSITIVITY_PT_ES, sensitivityPtEs));
        appender.add(String.format(KEY_VALUE_FORMAT, RUN_ANGLE_CHECK, runAngleCheck));
        appender.add(String.format(KEY_VALUE_FORMAT, RUN_VOLTAGE_CHECK, runVoltageCheck));
        appender.add(String.format(KEY_VALUE_FORMAT, MAX_CRA, maxCra));
        appender.add(String.format(KEY_VALUE_FORMAT, MAX_NEWTON_RAPHSON_ITERATIONS, maxNewtonRaphsonIterations));

        return String.format("{%s%n}", String.join(", ", appender));
    }
}

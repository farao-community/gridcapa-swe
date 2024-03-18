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

    private static final String RUN_ES_FR = "RUN_ES-FR";
    private static final String RUN_FR_ES = "RUN_FR-ES";
    private static final String RUN_ES_PT = "RUN_ES-PT";
    private static final String RUN_PT_ES = "RUN_PT-ES";
    private static final String MAX_TTC_ES_FR = "MAX_TTC_ES-FR";
    private static final String MAX_TTC_FR_ES = "MAX_TTC_FR-ES";
    private static final String MAX_TTC_ES_PT = "MAX_TTC_ES-PT";
    private static final String MAX_TTC_PT_ES = "MAX_TTC_PT-ES";
    private static final String MIN_TTC_ES_FR = "MIN_TTC_ES-FR";
    private static final String MIN_TTC_FR_ES = "MIN_TTC_FR-ES";
    private static final String MIN_TTC_ES_PT = "MIN_TTC_ES-PT";
    private static final String MIN_TTC_PT_ES = "MIN_TTC_PT-ES";
    private static final String DICHOTOMY_PRECISION_ES_FR = "DICHOTOMY_PRECISION_ES-FR";
    private static final String DICHOTOMY_PRECISION_FR_ES = "DICHOTOMY_PRECISION_FR-ES";
    private static final String DICHOTOMY_PRECISION_ES_PT = "DICHOTOMY_PRECISION_ES-PT";
    private static final String DICHOTOMY_PRECISION_PT_ES = "DICHOTOMY_PRECISION_PT-ES";
    private static final String RUN_ANGLE_CHECK = "RUN_ANGLE_CHECK";
    private static final String RUN_VOLTAGE_CHECK = "RUN_VOLTAGE_CHECK";
    private static final String MAX_CRA = "MAX_CRA";
    private static final String MAX_NEWTON_RAPHSON_ITERATIONS = "MAX_NEWTON_RAPHSON_ITERATIONS";
    private static final String DISABLE_SECOND_PREVENTIVE_RAO = "DISABLE_SECOND_PREVENTIVE_RAO";

    private boolean runDirectionEsFr;
    private boolean runDirectionFrEs;
    private boolean runDirectionEsPt;
    private boolean runDirectionPtEs;
    private int maxTtcEsFr;
    private int maxTtcFrEs;
    private int maxTtcEsPt;
    private int maxTtcPtEs;
    private int minTtcEsFr;
    private int minTtcFrEs;
    private int minTtcEsPt;
    private int minTtcPtEs;
    private int dichotomyPrecisionEsFr;
    private int dichotomyPrecisionFrEs;
    private int dichotomyPrecisionEsPt;
    private int dichotomyPrecisionPtEs;
    private boolean runAngleCheck;
    private boolean runVoltageCheck;
    private int maxCra;
    private int maxNewtonRaphsonIterations;
    private boolean secondPreventiveRaoDisabled;

    public SweTaskParameters(List<TaskParameterDto> parameters) {
        List<String> errors = new ArrayList<>();
        for (TaskParameterDto parameter : parameters) {
            switch (parameter.getId()) {
                case RUN_ES_FR -> runDirectionEsFr = validateIsBooleanAndGet(parameter, errors);
                case RUN_FR_ES -> runDirectionFrEs = validateIsBooleanAndGet(parameter, errors);
                case RUN_ES_PT -> runDirectionEsPt = validateIsBooleanAndGet(parameter, errors);
                case RUN_PT_ES -> runDirectionPtEs = validateIsBooleanAndGet(parameter, errors);
                case MAX_TTC_ES_FR -> maxTtcEsFr = validateIsPositiveIntegerAndGet(parameter, errors);
                case MAX_TTC_FR_ES -> maxTtcFrEs = validateIsPositiveIntegerAndGet(parameter, errors);
                case MAX_TTC_ES_PT -> maxTtcEsPt = validateIsPositiveIntegerAndGet(parameter, errors);
                case MAX_TTC_PT_ES -> maxTtcPtEs = validateIsPositiveIntegerAndGet(parameter, errors);
                case MIN_TTC_ES_FR -> minTtcEsFr = validateIsPositiveIntegerAndGet(parameter, errors);
                case MIN_TTC_FR_ES -> minTtcFrEs = validateIsPositiveIntegerAndGet(parameter, errors);
                case MIN_TTC_ES_PT -> minTtcEsPt = validateIsPositiveIntegerAndGet(parameter, errors);
                case MIN_TTC_PT_ES -> minTtcPtEs = validateIsPositiveIntegerAndGet(parameter, errors);
                case DICHOTOMY_PRECISION_ES_FR -> dichotomyPrecisionEsFr = validateIsPositiveIntegerAndGet(parameter, errors);
                case DICHOTOMY_PRECISION_FR_ES -> dichotomyPrecisionFrEs = validateIsPositiveIntegerAndGet(parameter, errors);
                case DICHOTOMY_PRECISION_ES_PT -> dichotomyPrecisionEsPt = validateIsPositiveIntegerAndGet(parameter, errors);
                case DICHOTOMY_PRECISION_PT_ES -> dichotomyPrecisionPtEs = validateIsPositiveIntegerAndGet(parameter, errors);
                case RUN_ANGLE_CHECK -> runAngleCheck = validateIsBooleanAndGet(parameter, errors);
                case RUN_VOLTAGE_CHECK -> runVoltageCheck = validateIsBooleanAndGet(parameter, errors);
                case MAX_CRA -> maxCra = validateIsPositiveIntegerAndGet(parameter, errors);
                case MAX_NEWTON_RAPHSON_ITERATIONS -> maxNewtonRaphsonIterations = validateIsPositiveIntegerAndGet(parameter, errors);
                case DISABLE_SECOND_PREVENTIVE_RAO -> secondPreventiveRaoDisabled = validateIsBooleanAndGet(parameter, errors);
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
            validateStartingPointAndMinPoint("ES-FR", maxTtcEsFr, minTtcEsFr, errors);
        }
        if (runDirectionFrEs) {
            validateStartingPointAndMinPoint("FR-ES", maxTtcFrEs, minTtcFrEs, errors);
        }
        if (runDirectionEsPt) {
            validateStartingPointAndMinPoint("ES-PT", maxTtcEsPt, minTtcEsPt, errors);
        }
        if (runDirectionPtEs) {
            validateStartingPointAndMinPoint("PT-ES", maxTtcPtEs, minTtcPtEs, errors);
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

    public int getMaxTtcEsFr() {
        return maxTtcEsFr;
    }

    public int getMaxTtcFrEs() {
        return maxTtcFrEs;
    }

    public int getMaxTtcEsPt() {
        return maxTtcEsPt;
    }

    public int getMaxTtcPtEs() {
        return maxTtcPtEs;
    }

    public int getMinTtcEsFr() {
        return minTtcEsFr;
    }

    public int getMinTtcFrEs() {
        return minTtcFrEs;
    }

    public int getMinTtcEsPt() {
        return minTtcEsPt;
    }

    public int getMinTtcPtEs() {
        return minTtcPtEs;
    }

    public int getDichotomyPrecisionEsFr() {
        return dichotomyPrecisionEsFr;
    }

    public int getDichotomyPrecisionFrEs() {
        return dichotomyPrecisionFrEs;
    }

    public int getDichotomyPrecisionEsPt() {
        return dichotomyPrecisionEsPt;
    }

    public int getDichotomyPrecisionPtEs() {
        return dichotomyPrecisionPtEs;
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

    public boolean isSecondPreventiveRaoDisabled() {
        return secondPreventiveRaoDisabled;
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    public String toJsonString() {
        List<String> appender = new ArrayList<>();
        appender.add(String.format(KEY_VALUE_FORMAT, RUN_ES_FR, runDirectionEsFr));
        appender.add(String.format(KEY_VALUE_FORMAT, RUN_FR_ES, runDirectionFrEs));
        appender.add(String.format(KEY_VALUE_FORMAT, RUN_ES_PT, runDirectionEsPt));
        appender.add(String.format(KEY_VALUE_FORMAT, RUN_PT_ES, runDirectionPtEs));
        appender.add(String.format(KEY_VALUE_FORMAT, MAX_TTC_ES_FR, maxTtcEsFr));
        appender.add(String.format(KEY_VALUE_FORMAT, MAX_TTC_FR_ES, maxTtcFrEs));
        appender.add(String.format(KEY_VALUE_FORMAT, MAX_TTC_ES_PT, maxTtcEsPt));
        appender.add(String.format(KEY_VALUE_FORMAT, MAX_TTC_PT_ES, maxTtcPtEs));
        appender.add(String.format(KEY_VALUE_FORMAT, MIN_TTC_ES_FR, minTtcEsFr));
        appender.add(String.format(KEY_VALUE_FORMAT, MIN_TTC_FR_ES, minTtcFrEs));
        appender.add(String.format(KEY_VALUE_FORMAT, MIN_TTC_ES_PT, minTtcEsPt));
        appender.add(String.format(KEY_VALUE_FORMAT, MIN_TTC_PT_ES, minTtcPtEs));
        appender.add(String.format(KEY_VALUE_FORMAT, DICHOTOMY_PRECISION_ES_FR, dichotomyPrecisionEsFr));
        appender.add(String.format(KEY_VALUE_FORMAT, DICHOTOMY_PRECISION_FR_ES, dichotomyPrecisionFrEs));
        appender.add(String.format(KEY_VALUE_FORMAT, DICHOTOMY_PRECISION_ES_PT, dichotomyPrecisionEsPt));
        appender.add(String.format(KEY_VALUE_FORMAT, DICHOTOMY_PRECISION_PT_ES, dichotomyPrecisionPtEs));
        appender.add(String.format(KEY_VALUE_FORMAT, RUN_ANGLE_CHECK, runAngleCheck));
        appender.add(String.format(KEY_VALUE_FORMAT, RUN_VOLTAGE_CHECK, runVoltageCheck));
        appender.add(String.format(KEY_VALUE_FORMAT, MAX_CRA, maxCra));
        appender.add(String.format(KEY_VALUE_FORMAT, MAX_NEWTON_RAPHSON_ITERATIONS, maxNewtonRaphsonIterations));
        appender.add(String.format(KEY_VALUE_FORMAT, DISABLE_SECOND_PREVENTIVE_RAO, secondPreventiveRaoDisabled));

        return String.format("{%s%n}", String.join(", ", appender));
    }
}

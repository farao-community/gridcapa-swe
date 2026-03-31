/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.domain;

import com.farao_community.farao.gridcapa.task_manager.api.TaskParameterDto;
import com.farao_community.farao.gridcapa_swe_commons.exception.SweInvalidDataException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SweTaskParametersTest {

    @Test
    void nominalTest() {
        List<TaskParameterDto> parameters = List.of(
            new TaskParameterDto("RUN_ES-FR", "BOOLEAN", "true", "true"),
            new TaskParameterDto("RUN_FR-ES", "BOOLEAN", "false", "true"),
            new TaskParameterDto("RUN_ES-PT", "BOOLEAN", "true", "true"),
            new TaskParameterDto("RUN_PT-ES", "BOOLEAN", "false", "true"),
            new TaskParameterDto("MAX_TTC_ES-FR", "INT", "42", "1515"),
            new TaskParameterDto("MAX_TTC_FR-ES", "INT", "43", "1515"),
            new TaskParameterDto("MAX_TTC_ES-PT", "INT", "44", "1515"),
            new TaskParameterDto("MAX_TTC_PT-ES", "INT", "45", "1515"),
            new TaskParameterDto("MIN_TTC_ES-FR", "INT", "26", "1515"),
            new TaskParameterDto("MIN_TTC_FR-ES", "INT", "27", "1515"),
            new TaskParameterDto("MIN_TTC_ES-PT", "INT", "28", "1515"),
            new TaskParameterDto("MIN_TTC_PT-ES", "INT", "29", "1515"),
            new TaskParameterDto("DICHOTOMY_PRECISION_ES-FR", "INT", "10", "1515"),
            new TaskParameterDto("DICHOTOMY_PRECISION_FR-ES", "INT", "11", "1515"),
            new TaskParameterDto("DICHOTOMY_PRECISION_ES-PT", "INT", "12", "1515"),
            new TaskParameterDto("DICHOTOMY_PRECISION_PT-ES", "INT", "13", "1515"),
            new TaskParameterDto("RUN_ANGLE_CHECK", "BOOLEAN", "true", "true"),
            new TaskParameterDto("RUN_VOLTAGE_CHECK", "BOOLEAN", "true", "true"),
            new TaskParameterDto("MAX_CRA", "INT", "72", "25"),
            new TaskParameterDto("MAX_NEWTON_RAPHSON_ITERATIONS", "INT", "38", "63"),
            new TaskParameterDto("DISABLE_SECOND_PREVENTIVE_RAO", "BOOLEAN", "true", "false"),
            new TaskParameterDto("EXPORT_FIRST_UNSECURE_SHIFTED_CGM", "BOOLEAN", "true", "false"),
            new TaskParameterDto("RUN_GLSK_CHECKS_BEFORE_LOADFLOW", "BOOLEAN", "true", "true")
        );

        final SweTaskParameters params = new SweTaskParameters(parameters);

        assertThat(params.isRunDirectionEsFr()).isTrue();
        assertThat(params.isRunDirectionFrEs()).isFalse();
        assertThat(params.isRunDirectionEsPt()).isTrue();
        assertThat(params.isRunDirectionPtEs()).isFalse();
        assertThat(params.getMaxTtcEsFr()).isEqualTo(42);
        assertThat(params.getMaxTtcFrEs()).isEqualTo(43);
        assertThat(params.getMaxTtcEsPt()).isEqualTo(44);
        assertThat(params.getMaxTtcPtEs()).isEqualTo(45);
        assertThat(params.getMinTtcEsFr()).isEqualTo(26);
        assertThat(params.getMinTtcFrEs()).isEqualTo(27);
        assertThat(params.getMinTtcEsPt()).isEqualTo(28);
        assertThat(params.getMinTtcPtEs()).isEqualTo(29);
        assertThat(params.getDichotomyPrecisionEsFr()).isEqualTo(10);
        assertThat(params.getDichotomyPrecisionFrEs()).isEqualTo(11);
        assertThat(params.getDichotomyPrecisionEsPt()).isEqualTo(12);
        assertThat(params.getDichotomyPrecisionPtEs()).isEqualTo(13);
        assertThat(params.isRunAngleCheck()).isTrue();
        assertThat(params.isRunVoltageCheck()).isTrue();
        assertThat(params.getMaxCra()).isEqualTo(72);
        assertThat(params.getMaxNewtonRaphsonIterations()).isEqualTo(38);
        assertThat(params.isSecondPreventiveRaoDisabled()).isTrue();
        assertThat(params.isExportFirstUnsecureShiftedCGM()).isTrue();
        assertThat(params.isRunGlskChecksBeforeLoadFlow()).isTrue();
    }

    @Test
    void unknownParameterTest() {
        List<TaskParameterDto> parameters = List.of(
            new TaskParameterDto("UNKNOWN_PARAMETER", "BOOLEAN", "true", "true")
        );

        // expected: object is created and no exception is thrown
        assertThat(new SweTaskParameters(parameters)).isInstanceOf(SweTaskParameters.class);
    }

    @Test
    void absentParametersTest() {
        final SweTaskParameters params = new SweTaskParameters(List.of());

        assertThat(params.isRunDirectionEsFr()).isFalse();
        assertThat(params.isRunDirectionFrEs()).isFalse();
        assertThat(params.isRunDirectionEsPt()).isFalse();
        assertThat(params.isRunDirectionPtEs()).isFalse();
        assertThat(params.getMaxTtcEsFr()).isZero();
        assertThat(params.getMaxTtcFrEs()).isZero();
        assertThat(params.getMaxTtcEsPt()).isZero();
        assertThat(params.getMaxTtcPtEs()).isZero();
        assertThat(params.getMinTtcEsFr()).isZero();
        assertThat(params.getMinTtcFrEs()).isZero();
        assertThat(params.getMinTtcEsPt()).isZero();
        assertThat(params.getMinTtcPtEs()).isZero();
        assertThat(params.getDichotomyPrecisionEsFr()).isZero();
        assertThat(params.getDichotomyPrecisionFrEs()).isZero();
        assertThat(params.getDichotomyPrecisionEsPt()).isZero();
        assertThat(params.getDichotomyPrecisionPtEs()).isZero();
        assertThat(params.isRunAngleCheck()).isFalse();
        assertThat(params.isRunVoltageCheck()).isFalse();
        assertThat(params.getMaxCra()).isZero();
        assertThat(params.getMaxNewtonRaphsonIterations()).isZero();
        assertThat(params.isSecondPreventiveRaoDisabled()).isFalse();
        assertThat(params.isExportFirstUnsecureShiftedCGM()).isFalse();
        assertThat(params.isRunGlskChecksBeforeLoadFlow()).isFalse();
    }

    @Test
    void badTypeParameterBooleanTest() {
        List<TaskParameterDto> parameters = List.of(
            new TaskParameterDto("RUN_FR-ES", "STRING", "test", "default")
        );

        Assertions.assertThatExceptionOfType(SweInvalidDataException.class)
            .isThrownBy(() -> new SweTaskParameters(parameters))
            .withMessage("Validation of parameters failed. Failure reasons are: [\"Parameter RUN_FR-ES was expected to be of type BOOLEAN, got STRING\"].");
    }

    @Test
    void badTypeParameterIntTest() {
        List<TaskParameterDto> parameters = List.of(
            new TaskParameterDto("DICHOTOMY_PRECISION_FR-ES", "STRING", "test", "default")
        );

        Assertions.assertThatExceptionOfType(SweInvalidDataException.class)
            .isThrownBy(() -> new SweTaskParameters(parameters))
            .withMessage("Validation of parameters failed. Failure reasons are: [\"Parameter DICHOTOMY_PRECISION_FR-ES was expected to be of type INT, got STRING\"].");
    }

    @Test
    void validationFailureIntParameterNotParseableTest() {
        List<TaskParameterDto> parameters = List.of(
            new TaskParameterDto("MIN_TTC_ES-FR", "INT", "3.14", "25")
        );

        Assertions.assertThatExceptionOfType(SweInvalidDataException.class)
            .isThrownBy(() -> new SweTaskParameters(parameters))
            .withMessage("Validation of parameters failed. Failure reasons are: [\"Parameter MIN_TTC_ES-FR could not be parsed as integer (value: 3.14)\"].");
    }

    @Test
    void validationFailurePositiveIntParameterTest() {
        List<TaskParameterDto> parameters = List.of(
            new TaskParameterDto("MAX_CRA", "INT", "-2", "10"),
            new TaskParameterDto("MAX_NEWTON_RAPHSON_ITERATIONS", "INT", "-5", "15")
        );

        Assertions.assertThatExceptionOfType(SweInvalidDataException.class)
            .isThrownBy(() -> new SweTaskParameters(parameters))
            .withMessage("Validation of parameters failed. Failure reasons are: [" +
                "\"Parameter MAX_CRA should be positive (value: -2)\" ; " +
                "\"Parameter MAX_NEWTON_RAPHSON_ITERATIONS should be positive (value: -5)\"" +
                "].");
    }

    @Test
    void crossValidationFailureTest() {
        List<TaskParameterDto> parameters = List.of(
            new TaskParameterDto("RUN_ES-FR", "BOOLEAN", "true", "true"),
            new TaskParameterDto("RUN_FR-ES", "BOOLEAN", "true", "true"),
            new TaskParameterDto("RUN_ES-PT", "BOOLEAN", "true", "true"),
            new TaskParameterDto("RUN_PT-ES", "BOOLEAN", "true", "true"),
            new TaskParameterDto("MAX_TTC_ES-FR", "INT", "15", "6400"),
            new TaskParameterDto("MAX_TTC_FR-ES", "INT", "16", "6400"),
            new TaskParameterDto("MAX_TTC_ES-PT", "INT", "17", "6400"),
            new TaskParameterDto("MAX_TTC_PT-ES", "INT", "18", "6400"),
            new TaskParameterDto("MIN_TTC_ES-FR", "INT", "100", "0"),
            new TaskParameterDto("MIN_TTC_FR-ES", "INT", "101", "0"),
            new TaskParameterDto("MIN_TTC_ES-PT", "INT", "102", "0"),
            new TaskParameterDto("MIN_TTC_PT-ES", "INT", "103", "0")
        );

        Assertions.assertThatExceptionOfType(SweInvalidDataException.class)
            .isThrownBy(() -> new SweTaskParameters(parameters))
            .withMessage("Validation of parameters failed. Failure reasons are: [" +
                "\"[ES-FR] Starting point (value: 15) should be greater than minimum point (value: 100)\" ; " +
                "\"[FR-ES] Starting point (value: 16) should be greater than minimum point (value: 101)\" ; " +
                "\"[ES-PT] Starting point (value: 17) should be greater than minimum point (value: 102)\" ; " +
                "\"[PT-ES] Starting point (value: 18) should be greater than minimum point (value: 103)\"" +
                "].");
    }
}

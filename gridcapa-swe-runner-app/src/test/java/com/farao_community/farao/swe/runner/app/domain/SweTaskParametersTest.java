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

class SweTaskParametersTest {

    @Test
    void nominalTest() {
        List<TaskParameterDto> parameters = List.of(
            new TaskParameterDto("RUN_ES-FR", "BOOLEAN", "true", "true"),
            new TaskParameterDto("RUN_FR-ES", "BOOLEAN", "false", "true"),
            new TaskParameterDto("RUN_ES-PT", "BOOLEAN", "true", "true"),
            new TaskParameterDto("RUN_PT-ES", "BOOLEAN", "false", "true"),
            new TaskParameterDto("STARTING_POINT_ES-FR", "INT", "42", "1515"),
            new TaskParameterDto("STARTING_POINT_FR-ES", "INT", "43", "1515"),
            new TaskParameterDto("STARTING_POINT_ES-PT", "INT", "44", "1515"),
            new TaskParameterDto("STARTING_POINT_PT-ES", "INT", "45", "1515"),
            new TaskParameterDto("MIN_POINT_ES-FR", "INT", "26", "1515"),
            new TaskParameterDto("MIN_POINT_FR-ES", "INT", "27", "1515"),
            new TaskParameterDto("MIN_POINT_ES-PT", "INT", "28", "1515"),
            new TaskParameterDto("MIN_POINT_PT-ES", "INT", "29", "1515"),
            new TaskParameterDto("SENSITIVITY_ES-FR", "INT", "10", "1515"),
            new TaskParameterDto("SENSITIVITY_FR-ES", "INT", "11", "1515"),
            new TaskParameterDto("SENSITIVITY_ES-PT", "INT", "12", "1515"),
            new TaskParameterDto("SENSITIVITY_PT-ES", "INT", "13", "1515"),
            new TaskParameterDto("RUN_ANGLE_CHECK", "BOOLEAN", "true", "true"),
            new TaskParameterDto("RUN_VOLTAGE_CHECK", "BOOLEAN", "true", "true"),
            new TaskParameterDto("MAX_CRA", "INT", "72", "25"),
            new TaskParameterDto("MAX_NEWTON_RAPHSON_ITERATIONS", "INT", "38", "63")
        );

        SweTaskParameters sweTaskParameters = new SweTaskParameters(parameters);

        Assertions.assertThat(sweTaskParameters.isRunDirectionEsFr()).isTrue();
        Assertions.assertThat(sweTaskParameters.isRunDirectionFrEs()).isFalse();
        Assertions.assertThat(sweTaskParameters.isRunDirectionEsPt()).isTrue();
        Assertions.assertThat(sweTaskParameters.isRunDirectionPtEs()).isFalse();
        Assertions.assertThat(sweTaskParameters.getStartingPointEsFr()).isEqualTo(42);
        Assertions.assertThat(sweTaskParameters.getStartingPointFrEs()).isEqualTo(43);
        Assertions.assertThat(sweTaskParameters.getStartingPointEsPt()).isEqualTo(44);
        Assertions.assertThat(sweTaskParameters.getStartingPointPtEs()).isEqualTo(45);
        Assertions.assertThat(sweTaskParameters.getMinPointEsFr()).isEqualTo(26);
        Assertions.assertThat(sweTaskParameters.getMinPointFrEs()).isEqualTo(27);
        Assertions.assertThat(sweTaskParameters.getMinPointEsPt()).isEqualTo(28);
        Assertions.assertThat(sweTaskParameters.getMinPointPtEs()).isEqualTo(29);
        Assertions.assertThat(sweTaskParameters.getSensitivityEsFr()).isEqualTo(10);
        Assertions.assertThat(sweTaskParameters.getSensitivityFrEs()).isEqualTo(11);
        Assertions.assertThat(sweTaskParameters.getSensitivityEsPt()).isEqualTo(12);
        Assertions.assertThat(sweTaskParameters.getSensitivityPtEs()).isEqualTo(13);
        Assertions.assertThat(sweTaskParameters.isRunAngleCheck()).isTrue();
        Assertions.assertThat(sweTaskParameters.isRunVoltageCheck()).isTrue();
        Assertions.assertThat(sweTaskParameters.getMaxCra()).isEqualTo(72);
        Assertions.assertThat(sweTaskParameters.getMaxNewtonRaphsonIterations()).isEqualTo(38);
    }

    @Test
    void unknownParameterTest() {
        List<TaskParameterDto> parameters = List.of(
            new TaskParameterDto("UNKNOWN_PARAMETER", "BOOLEAN", "true", "true")
        );

        // expected: object is created and no exception is thrown
        Assertions.assertThat(new SweTaskParameters(parameters)).isInstanceOf(SweTaskParameters.class);
    }

    @Test
    void absentParametersTest() {
        SweTaskParameters sweTaskParameters = new SweTaskParameters(List.of());

        Assertions.assertThat(sweTaskParameters.isRunDirectionEsFr()).isFalse();
        Assertions.assertThat(sweTaskParameters.isRunDirectionFrEs()).isFalse();
        Assertions.assertThat(sweTaskParameters.isRunDirectionEsPt()).isFalse();
        Assertions.assertThat(sweTaskParameters.isRunDirectionPtEs()).isFalse();
        Assertions.assertThat(sweTaskParameters.getStartingPointEsFr()).isZero();
        Assertions.assertThat(sweTaskParameters.getStartingPointFrEs()).isZero();
        Assertions.assertThat(sweTaskParameters.getStartingPointEsPt()).isZero();
        Assertions.assertThat(sweTaskParameters.getStartingPointPtEs()).isZero();
        Assertions.assertThat(sweTaskParameters.getMinPointEsFr()).isZero();
        Assertions.assertThat(sweTaskParameters.getMinPointFrEs()).isZero();
        Assertions.assertThat(sweTaskParameters.getMinPointEsPt()).isZero();
        Assertions.assertThat(sweTaskParameters.getMinPointPtEs()).isZero();
        Assertions.assertThat(sweTaskParameters.getSensitivityEsFr()).isZero();
        Assertions.assertThat(sweTaskParameters.getSensitivityFrEs()).isZero();
        Assertions.assertThat(sweTaskParameters.getSensitivityEsPt()).isZero();
        Assertions.assertThat(sweTaskParameters.getSensitivityPtEs()).isZero();
        Assertions.assertThat(sweTaskParameters.isRunAngleCheck()).isFalse();
        Assertions.assertThat(sweTaskParameters.isRunVoltageCheck()).isFalse();
        Assertions.assertThat(sweTaskParameters.getMaxCra()).isZero();
        Assertions.assertThat(sweTaskParameters.getMaxNewtonRaphsonIterations()).isZero();
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
            new TaskParameterDto("SENSITIVITY_FR-ES", "STRING", "test", "default")
        );

        Assertions.assertThatExceptionOfType(SweInvalidDataException.class)
            .isThrownBy(() -> new SweTaskParameters(parameters))
            .withMessage("Validation of parameters failed. Failure reasons are: [\"Parameter SENSITIVITY_FR-ES was expected to be of type INT, got STRING\"].");
    }

    @Test
    void validationFailureIntParameterNotParseableTest() {
        List<TaskParameterDto> parameters = List.of(
            new TaskParameterDto("MIN_POINT_ES-FR", "INT", "3.14", "25")
        );

        Assertions.assertThatExceptionOfType(SweInvalidDataException.class)
            .isThrownBy(() -> new SweTaskParameters(parameters))
            .withMessage("Validation of parameters failed. Failure reasons are: [\"Parameter MIN_POINT_ES-FR could not be parsed as integer (value: 3.14)\"].");
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
            new TaskParameterDto("STARTING_POINT_ES-FR", "INT", "15", "6400"),
            new TaskParameterDto("STARTING_POINT_FR-ES", "INT", "16", "6400"),
            new TaskParameterDto("STARTING_POINT_ES-PT", "INT", "17", "6400"),
            new TaskParameterDto("STARTING_POINT_PT-ES", "INT", "18", "6400"),
            new TaskParameterDto("MIN_POINT_ES-FR", "INT", "100", "0"),
            new TaskParameterDto("MIN_POINT_FR-ES", "INT", "101", "0"),
            new TaskParameterDto("MIN_POINT_ES-PT", "INT", "102", "0"),
            new TaskParameterDto("MIN_POINT_PT-ES", "INT", "103", "0")
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

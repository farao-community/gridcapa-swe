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
            new TaskParameterDto("MIN_POINT_ES-FR", "INT", "46", "1515"),
            new TaskParameterDto("MIN_POINT_FR-ES", "INT", "47", "1515"),
            new TaskParameterDto("MIN_POINT_ES-PT", "INT", "48", "1515"),
            new TaskParameterDto("MIN_POINT_PT-ES", "INT", "49", "1515"),
            new TaskParameterDto("SENSITIVITY_ES-FR", "INT", "50", "1515"),
            new TaskParameterDto("SENSITIVITY_FR-ES", "INT", "51", "1515"),
            new TaskParameterDto("SENSITIVITY_ES-PT", "INT", "52", "1515"),
            new TaskParameterDto("SENSITIVITY_PT-ES", "INT", "53", "1515"),
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
        Assertions.assertThat(sweTaskParameters.getMinPointEsFr()).isEqualTo(46);
        Assertions.assertThat(sweTaskParameters.getMinPointFrEs()).isEqualTo(47);
        Assertions.assertThat(sweTaskParameters.getMinPointEsPt()).isEqualTo(48);
        Assertions.assertThat(sweTaskParameters.getMinPointPtEs()).isEqualTo(49);
        Assertions.assertThat(sweTaskParameters.getSensitivityEsFr()).isEqualTo(50);
        Assertions.assertThat(sweTaskParameters.getSensitivityFrEs()).isEqualTo(51);
        Assertions.assertThat(sweTaskParameters.getSensitivityEsPt()).isEqualTo(52);
        Assertions.assertThat(sweTaskParameters.getSensitivityPtEs()).isEqualTo(53);
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

        // expected: object is created and no exception is thrown
        Assertions.assertThatExceptionOfType(SweInvalidDataException.class)
            .isThrownBy(() -> new SweTaskParameters(parameters))
            .withMessage("Invalid boolean parameter with id RUN_FR-ES and value test");
    }

    @Test
    void badTypeParameterIntTest() {
        List<TaskParameterDto> parameters = List.of(
            new TaskParameterDto("SENSITIVITY_FR-ES", "STRING", "test", "default")
        );

        // expected: object is created and no exception is thrown
        Assertions.assertThatExceptionOfType(SweInvalidDataException.class)
            .isThrownBy(() -> new SweTaskParameters(parameters))
            .withMessage("Invalid integer parameter with id SENSITIVITY_FR-ES and value test");
    }
}

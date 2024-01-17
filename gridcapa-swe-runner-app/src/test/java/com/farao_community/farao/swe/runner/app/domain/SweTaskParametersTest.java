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
            new TaskParameterDto("RUN_PT-ES", "BOOLEAN", "false", "true")
        );

        SweTaskParameters sweTaskParameters = new SweTaskParameters(parameters);

        Assertions.assertThat(sweTaskParameters.isRunDirectionEsToFr()).isTrue();
        Assertions.assertThat(sweTaskParameters.isRunDirectionFrToEs()).isFalse();
        Assertions.assertThat(sweTaskParameters.isRunDirectionEsToPt()).isTrue();
        Assertions.assertThat(sweTaskParameters.isRunDirectionPtToEs()).isFalse();
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

        Assertions.assertThat(sweTaskParameters.isRunDirectionEsToFr()).isFalse();
        Assertions.assertThat(sweTaskParameters.isRunDirectionFrToEs()).isFalse();
        Assertions.assertThat(sweTaskParameters.isRunDirectionEsToPt()).isFalse();
        Assertions.assertThat(sweTaskParameters.isRunDirectionPtToEs()).isFalse();
    }

    @Test
    void badTypeParameterTest() {
        List<TaskParameterDto> parameters = List.of(
            new TaskParameterDto("RUN_FR-ES", "STRING", "test", "default")
        );

        // expected: object is created and no exception is thrown
        Assertions.assertThatExceptionOfType(SweInvalidDataException.class)
            .isThrownBy(() -> new SweTaskParameters(parameters))
            .withMessage("Invalid boolean parameter with id RUN_FR-ES and value test");
    }
}

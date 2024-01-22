/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app;

import com.farao_community.farao.gridcapa.task_manager.api.TaskParameterDto;
import com.farao_community.farao.swe.runner.app.domain.SweTaskParameters;

import java.util.ArrayList;
import java.util.List;

public class SweTaskParametersTestUtil {
    public static SweTaskParameters getSweTaskParameters() {
        List<TaskParameterDto> parameters = new ArrayList<>();
        parameters.add(new TaskParameterDto("RUN_ES-FR", "BOOLEAN", "true", "true"));
        parameters.add(new TaskParameterDto("RUN_FR-ES", "BOOLEAN", "true", "true"));
        parameters.add(new TaskParameterDto("RUN_ES-PT", "BOOLEAN", "true", "true"));
        parameters.add(new TaskParameterDto("RUN_PT-ES", "BOOLEAN", "true", "true"));
        parameters.add(new TaskParameterDto("STARTING_POINT_ES-FR", "INT", "42", "1515"));
        parameters.add(new TaskParameterDto("STARTING_POINT_FR-ES", "INT", "43", "1515"));
        parameters.add(new TaskParameterDto("STARTING_POINT_ES-PT", "INT", "44", "1515"));
        parameters.add(new TaskParameterDto("STARTING_POINT_PT-ES", "INT", "45", "1515"));
        parameters.add(new TaskParameterDto("MIN_POINT_ES-FR", "INT", "82", "1515"));
        parameters.add(new TaskParameterDto("MIN_POINT_FR-ES", "INT", "83", "1515"));
        parameters.add(new TaskParameterDto("MIN_POINT_ES-PT", "INT", "84", "1515"));
        parameters.add(new TaskParameterDto("MIN_POINT_PT-ES", "INT", "85", "1515"));
        parameters.add(new TaskParameterDto("SENSITIVITY_ES-FR", "INT", "12", "1515"));
        parameters.add(new TaskParameterDto("SENSITIVITY_FR-ES", "INT", "13", "1515"));
        parameters.add(new TaskParameterDto("SENSITIVITY_ES-PT", "INT", "14", "1515"));
        parameters.add(new TaskParameterDto("SENSITIVITY_PT-ES", "INT", "15", "1515"));
        parameters.add(new TaskParameterDto("RUN_ANGLE_CHECK", "BOOLEAN", "true", "true"));
        parameters.add(new TaskParameterDto("RUN_VOLTAGE_CHECK", "BOOLEAN", "true", "true"));
        return new SweTaskParameters(parameters);
    }

}

/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.utils;

import com.farao_community.farao.swe.runner.app.domain.SweTaskParameters;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openloadflow.OpenLoadFlowParameters;

public final class OpenLoadFlowParametersUtil {
    private OpenLoadFlowParametersUtil() {
        // private constructor because util class should not be instanced
    }

    public static LoadFlowParameters getLoadFlowParameters(SweTaskParameters sweTaskParameters) {
        LoadFlowParameters loadFlowParameters = LoadFlowParameters.load();

        OpenLoadFlowParameters openLoadFlowParameters = loadFlowParameters.getExtension(OpenLoadFlowParameters.class);
        if (openLoadFlowParameters == null) {
            openLoadFlowParameters = new OpenLoadFlowParameters();
            loadFlowParameters.addExtension(OpenLoadFlowParameters.class, openLoadFlowParameters);
        }
        openLoadFlowParameters.setMaxNewtonRaphsonIterations(sweTaskParameters.getMaxNewtonRaphsonIterations());

        return loadFlowParameters;
    }
}

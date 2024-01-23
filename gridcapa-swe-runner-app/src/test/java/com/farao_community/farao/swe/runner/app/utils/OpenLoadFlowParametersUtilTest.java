/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.utils;

import com.farao_community.farao.swe.runner.app.SweTaskParametersTestUtil;
import com.farao_community.farao.swe.runner.app.domain.SweTaskParameters;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openloadflow.OpenLoadFlowParameters;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenLoadFlowParametersUtilTest {

    @Test
    void getLoadFlowParameters() {
        SweTaskParameters sweTaskParameters = SweTaskParametersTestUtil.getSweTaskParameters();
        LoadFlowParameters loadFlowParameters = OpenLoadFlowParametersUtil.getLoadFlowParameters(sweTaskParameters);

        assertNotNull(loadFlowParameters);
        OpenLoadFlowParameters openLoadFlowParameters = loadFlowParameters.getExtension(OpenLoadFlowParameters.class);
        assertNotNull(openLoadFlowParameters);
        assertEquals(
            sweTaskParameters.getMaxNewtonRaphsonIterations(),
            openLoadFlowParameters.getMaxNewtonRaphsonIterations()
        );
    }
}

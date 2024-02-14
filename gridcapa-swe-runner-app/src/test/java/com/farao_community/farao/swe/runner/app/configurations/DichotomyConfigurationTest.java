/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.configurations;

import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@SpringBootTest
class DichotomyConfigurationTest {

    @Autowired
    private DichotomyConfiguration dichotomyConfiguration;

    @Test
    void checkParameters() {
        assertEquals(50, dichotomyConfiguration.getParameters().get(DichotomyDirection.ES_FR).getToleranceEsPt());
        assertEquals(10, dichotomyConfiguration.getParameters().get(DichotomyDirection.ES_FR).getToleranceEsFr());

        assertEquals(50, dichotomyConfiguration.getParameters().get(DichotomyDirection.FR_ES).getToleranceEsPt());
        assertEquals(10, dichotomyConfiguration.getParameters().get(DichotomyDirection.FR_ES).getToleranceEsFr());

        assertEquals(10, dichotomyConfiguration.getParameters().get(DichotomyDirection.ES_PT).getToleranceEsPt());
        assertEquals(50, dichotomyConfiguration.getParameters().get(DichotomyDirection.ES_PT).getToleranceEsFr());

        assertEquals(10, dichotomyConfiguration.getParameters().get(DichotomyDirection.ES_PT).getToleranceEsPt());
        assertEquals(50, dichotomyConfiguration.getParameters().get(DichotomyDirection.ES_PT).getToleranceEsFr());
    }
}

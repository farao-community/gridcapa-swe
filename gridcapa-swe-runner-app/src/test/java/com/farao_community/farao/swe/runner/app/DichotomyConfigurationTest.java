/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app;

import com.farao_community.farao.swe.runner.app.configurations.DichotomyConfiguration;
import com.farao_community.farao.swe.runner.app.utils.Direction;
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
        assertEquals(0, dichotomyConfiguration.getDirection().get(Direction.ES_FR).getMinValue());
        assertEquals(6400, dichotomyConfiguration.getDirection().get(Direction.ES_FR).getMaxValue());
        assertEquals(50, dichotomyConfiguration.getDirection().get(Direction.ES_FR).getPrecision());
        assertEquals(10, dichotomyConfiguration.getDirection().get(Direction.ES_FR).getTolerance());

        assertEquals(0, dichotomyConfiguration.getDirection().get(Direction.FR_ES).getMinValue());
        assertEquals(6400, dichotomyConfiguration.getDirection().get(Direction.FR_ES).getMaxValue());
        assertEquals(50, dichotomyConfiguration.getDirection().get(Direction.FR_ES).getPrecision());
        assertEquals(10, dichotomyConfiguration.getDirection().get(Direction.FR_ES).getTolerance());

        assertEquals(0, dichotomyConfiguration.getDirection().get(Direction.ES_PT).getMinValue());
        assertEquals(6400, dichotomyConfiguration.getDirection().get(Direction.ES_PT).getMaxValue());
        assertEquals(50, dichotomyConfiguration.getDirection().get(Direction.ES_PT).getPrecision());
        assertEquals(50, dichotomyConfiguration.getDirection().get(Direction.ES_PT).getTolerance());

        assertEquals(0, dichotomyConfiguration.getDirection().get(Direction.PT_ES).getMinValue());
        assertEquals(6400, dichotomyConfiguration.getDirection().get(Direction.PT_ES).getMaxValue());
        assertEquals(50, dichotomyConfiguration.getDirection().get(Direction.PT_ES).getPrecision());
        assertEquals(50, dichotomyConfiguration.getDirection().get(Direction.PT_ES).getTolerance());
    }
}

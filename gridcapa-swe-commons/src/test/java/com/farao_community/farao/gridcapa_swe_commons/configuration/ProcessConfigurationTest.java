/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_swe_commons.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@SpringBootTest
class ProcessConfigurationTest {

    @Autowired
    private ProcessConfiguration processConfiguration;

    @Test
    void checkProcessConfiguration() {
        assertEquals(20, processConfiguration.getShiftMaxIterationNumber());
        assertEquals("Europe/Paris", processConfiguration.getZoneId());
        assertEquals("http://www.ree.es/OperationalPlanning", processConfiguration.getModelingAuthoritySet().get("REE"));
        assertEquals("http://www.ren.pt/OperationalPlanning", processConfiguration.getModelingAuthoritySet().get("REN"));
        assertEquals("http://www.rte-france.com/OperationalPlanning", processConfiguration.getModelingAuthoritySet().get("RTE"));
        assertEquals("http://www.coreso.eu/OperationalPlanning", processConfiguration.getModelingAuthoritySet().get("SV"));
    }

}

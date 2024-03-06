/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.configurations;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@SpringBootTest
class PstConfigurationTest {

    @Autowired
    private PstConfiguration pstConfiguration;

    @Test
    void checkPstIds() {
        assertEquals("_e071a1d4-fef5-1bd9-5278-d195c5597b6e", pstConfiguration.getPst1Id());
        assertEquals("_7824bc48-fc86-51db-8f9c-01b44933839e", pstConfiguration.getPst2Id());
    }
}

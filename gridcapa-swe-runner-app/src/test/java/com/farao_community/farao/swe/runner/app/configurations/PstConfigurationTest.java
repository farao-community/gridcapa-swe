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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@SpringBootTest
class PstConfigurationTest {

    @Autowired
    private PstConfiguration pstConfiguration;

    @Test
    void checkPstIds() {
        assertTrue(pstConfiguration.getPstIds().contains("twt-not-found-id"));
        assertTrue(pstConfiguration.getPstIds().contains("twt-test-regulation-id"));
        assertTrue(pstConfiguration.getPstIds().contains("twt-without-tap-changer-id"));
    }

    @Test
    void checkPstsToRegulateConfig() {
        final PstConfiguration.PstToRegulate regul1 = new PstConfiguration.PstToRegulate("pstTestId1", "_line1 + _line2");
        final PstConfiguration.PstToRegulate regul2 = new PstConfiguration.PstToRegulate("pstTestId2", "_line3 + _line4");

        final List<PstConfiguration.PstToRegulate> pstsToRegulate = pstConfiguration.getPstsToRegulate();
        assertThat(pstsToRegulate).containsExactly(regul1, regul2);
    }
}

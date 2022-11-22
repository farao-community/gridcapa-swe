/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.monitoring.voltage_monitoring.VoltageMonitoringResult;
import com.farao_community.farao.swe.runner.app.dichotomy.DichotomyDirection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */
@SpringBootTest
class VoltageCheckServiceTest {

    @Autowired
    private VoltageCheckService service;

    @Test
    void checkDoesntReturnVoltageCheckIfNotFrESBorder() {
        Optional<VoltageMonitoringResult> result = service.runVoltageCheck(null, null, DichotomyDirection.ES_PT);
        assertTrue(result.isEmpty());
        result = service.runVoltageCheck(null, null, DichotomyDirection.PT_ES);
        assertTrue(result.isEmpty());
    }
}

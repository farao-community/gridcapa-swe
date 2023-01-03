/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy;

import com.farao_community.farao.dichotomy.shift.ShiftDispatcher;
import com.farao_community.farao.swe.runner.api.resource.ProcessType;
import com.farao_community.farao.swe.runner.app.dichotomy.shift.SweD2ccShiftDispatcher;
import com.farao_community.farao.swe.runner.app.dichotomy.shift.SweIdccShiftDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@SpringBootTest
class NetworkShifterProviderTest {

    @Autowired
    private NetworkShifterProvider networkShifterProvider;
    private final Map<String, Double> intialNetPositions = Map.of("10YES-REE------0", 50., "10YFR-RTE------C", 100., "10YPT-REN------W", 150.);

    @Test
    void checkThatD2ccShiftDispatcherIsChosen() {
        ShiftDispatcher shiftDispatcher = networkShifterProvider.getShiftDispatcher(ProcessType.D2CC, DichotomyDirection.ES_FR, intialNetPositions);
        assertTrue(shiftDispatcher instanceof SweD2ccShiftDispatcher);
    }

    @Test
    void checkThatIdccShiftDispatcherIsChosen() {
        ShiftDispatcher shiftDispatcher = networkShifterProvider.getShiftDispatcher(ProcessType.IDCC, DichotomyDirection.ES_FR, intialNetPositions);
        assertTrue(shiftDispatcher instanceof SweIdccShiftDispatcher);
    }

}

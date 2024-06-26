/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy;

import com.farao_community.farao.dichotomy.api.exceptions.ShiftingException;
import com.farao_community.farao.dichotomy.shift.ShiftDispatcher;
import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import com.farao_community.farao.gridcapa_swe_commons.resource.ProcessType;
import com.farao_community.farao.gridcapa_swe_commons.resource.SweEICode;
import com.farao_community.farao.gridcapa_swe_commons.shift.SweD2ccShiftDispatcher;
import com.farao_community.farao.gridcapa_swe_commons.shift.SweIdccShiftDispatcher;
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
    private final Map<String, Double> intialNetPositions = Map.of(SweEICode.ES_EIC, 50., SweEICode.FR_EIC, 100., SweEICode.PT_EIC, 150.);

    @Test
    void checkThatD2ccShiftDispatcherIsChosen() throws ShiftingException {
        ShiftDispatcher shiftDispatcher = networkShifterProvider.getShiftDispatcher(ProcessType.D2CC, DichotomyDirection.FR_ES, intialNetPositions);
        assertTrue(shiftDispatcher instanceof SweD2ccShiftDispatcher);
        Map<String, Double> shifts = shiftDispatcher.dispatch(1000);
        assertEquals(900, shifts.get(SweEICode.FR_EIC));
        assertEquals(-150, shifts.get(SweEICode.PT_EIC));
        assertEquals(-1050, shifts.get(SweEICode.ES_EIC));
    }

    @Test
    void checkThatIdccShiftDispatcherIsChosen() throws ShiftingException {
        ShiftDispatcher shiftDispatcher = networkShifterProvider.getShiftDispatcher(ProcessType.IDCC, DichotomyDirection.ES_PT, intialNetPositions);
        assertTrue(shiftDispatcher instanceof SweIdccShiftDispatcher);
        Map<String, Double> shifts = shiftDispatcher.dispatch(500);
        assertEquals(-300.0, shifts.get(SweEICode.FR_EIC));
        assertEquals(-650, shifts.get(SweEICode.PT_EIC));
        assertEquals(650, shifts.get(SweEICode.ES_EIC));
    }

}

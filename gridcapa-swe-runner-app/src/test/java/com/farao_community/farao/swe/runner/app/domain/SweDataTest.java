/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.domain;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.cim.CimCrac;
import com.farao_community.farao.swe.runner.api.resource.ProcessType;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@SpringBootTest
class SweDataTest {

    @Mock
    private Network network;

    @Mock
    private CimCrac cimCrac;

    @Mock
    private Crac cracEsPt;

    @Mock
    private Crac cracFrEs;

    @Test
    void simpleTest() {
        SweData sweData = new SweData("id", OffsetDateTime.now(), ProcessType.D2CC, network, cimCrac, cracEsPt, cracFrEs, "glskUrl", "CracEsPt", "CracFrEs");
        assertEquals(ProcessType.D2CC, sweData.getProcessType());
        assertEquals(this.network, sweData.getNetwork());
        assertEquals(this.cimCrac, sweData.getCimCrac());
        assertEquals(this.cracEsPt, sweData.getCracEsPt());
        assertEquals(this.cracFrEs, sweData.getCracFrEs());
        assertEquals("CracEsPt", sweData.getJsonCracPathEsPt());
        assertEquals("CracFrEs", sweData.getJsonCracPathFrEs());
        assertEquals("glskUrl", sweData.getGlskUrl());
    }
}

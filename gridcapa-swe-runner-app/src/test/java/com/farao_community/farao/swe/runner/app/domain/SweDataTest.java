/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.domain;

import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
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
    private CimCracCreationContext cracEsPt;

    @Mock
    private CimCracCreationContext cracFrEs;

    @Test
    void simpleD2ccTest() {
        SweData sweData = new SweData("id", OffsetDateTime.now(), ProcessType.D2CC, network, network, network, network, null, cracFrEs, cracEsPt, "glskUrl", "CracEsPt", "CracFrEs");
        assertEquals(ProcessType.D2CC, sweData.getProcessType());
        assertEquals(this.network, sweData.getNetworkEsFr());
        assertEquals(this.cracEsPt, sweData.getCracEsPt());
        assertEquals(this.cracFrEs, sweData.getCracFrEs());
        assertEquals("CracEsPt", sweData.getJsonCracPathEsPt());
        assertEquals("CracFrEs", sweData.getJsonCracPathFrEs());
        assertEquals("glskUrl", sweData.getGlskUrl());
    }

    @Test
    void simpleIdccTest() {
        SweData sweData = new SweData("id", OffsetDateTime.now(), ProcessType.IDCC, network, network, network, network, null, cracFrEs, cracEsPt, "glskUrl", "CracEsPt", "CracFrEs");
        assertEquals(ProcessType.IDCC, sweData.getProcessType());
        assertEquals(this.network, sweData.getNetworkEsFr());
        assertEquals(this.cracEsPt, sweData.getCracEsPt());
        assertEquals(this.cracFrEs, sweData.getCracFrEs());
        assertEquals("CracEsPt", sweData.getJsonCracPathEsPt());
        assertEquals("CracFrEs", sweData.getJsonCracPathFrEs());
        assertEquals("glskUrl", sweData.getGlskUrl());
    }
}

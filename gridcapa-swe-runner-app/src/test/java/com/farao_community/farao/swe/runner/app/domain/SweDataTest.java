/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.domain;

import com.powsybl.openrao.data.crac.io.cim.craccreator.CimCracCreationContext;
import com.farao_community.farao.gridcapa_swe_commons.resource.ProcessType;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.EnumMap;

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
        SweData sweData = new SweData("id", "runId", OffsetDateTime.now(), ProcessType.D2CC, network, network, network, network, cracFrEs, cracEsPt, "glskUrl", "CracEsPt", "CracFrEs", "raoParametersEsFrUrl", "raoParametersEsPtUrl", new EnumMap<>(CgmesFileType.class), Collections.emptyMap());
        assertEquals(ProcessType.D2CC, sweData.getProcessType());
        assertEquals(this.network, sweData.getNetworkEsFr());
        assertEquals(this.cracEsPt, sweData.getCracEsPt());
        assertEquals(this.cracFrEs, sweData.getCracFrEs());
        assertEquals("CracEsPt", sweData.getJsonCracPathEsPt());
        assertEquals("CracFrEs", sweData.getJsonCracPathFrEs());
        assertEquals("glskUrl", sweData.getGlskUrl());
        assertEquals("raoParametersEsFrUrl", sweData.getRaoParametersEsFrUrl());
        assertEquals("raoParametersEsPtUrl", sweData.getRaoParametersEsPtUrl());
        assertEquals(Collections.emptyMap(), sweData.getMapCgmesInputFiles());
    }

    @Test
    void simpleIdccTest() {
        SweData sweData = new SweData("id", "runId", OffsetDateTime.now(), ProcessType.IDCC, network, network, network, network, cracFrEs, cracEsPt, "glskUrl", "CracEsPt", "CracFrEs", "raoParametersEsFrUrl", "raoParametersEsPtUrl", new EnumMap<>(CgmesFileType.class), Collections.emptyMap());
        assertEquals(ProcessType.IDCC, sweData.getProcessType());
        assertEquals(this.network, sweData.getNetworkEsFr());
        assertEquals(this.cracEsPt, sweData.getCracEsPt());
        assertEquals(this.cracFrEs, sweData.getCracFrEs());
        assertEquals("CracEsPt", sweData.getJsonCracPathEsPt());
        assertEquals("CracFrEs", sweData.getJsonCracPathFrEs());
        assertEquals("glskUrl", sweData.getGlskUrl());
        assertEquals("raoParametersEsFrUrl", sweData.getRaoParametersEsFrUrl());
        assertEquals("raoParametersEsPtUrl", sweData.getRaoParametersEsPtUrl());
        assertEquals(Collections.emptyMap(), sweData.getMapCgmesInputFiles());
    }
}

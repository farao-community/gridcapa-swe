/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.api.resource;

import com.farao_community.farao.gridcapa_swe_commons.resource.ProcessType;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
class SweRequestTest {

    @Test
    void simpleRequestTest() {
        OffsetDateTime now = OffsetDateTime.now();
        SweRequest sweRequest = new SweRequest("id", ProcessType.D2CC, now,
                new SweFileResource("CORESO_SV.xml", "/network/CORESO-CE_SV_000.xml"),
                new SweFileResource("REE_EQ.xml", "/network/REE_EQ_001.xml"),
                new SweFileResource("REE_SSH.xml", "/network/REE_SSH_000.xml"),
                new SweFileResource("REE_TP.xml", "/network/REE_TP_001.xml"),
                new SweFileResource("REN_EQ.xml", "/network/REN_EQ_001.xml"),
                new SweFileResource("REN_SSH.xml", "/network/REN_SSH_000.xml"),
                new SweFileResource("REN_TP.xml", "/network/REN_TP_001.xml"),
                new SweFileResource("RTE_EQ.xml", "/network/RTEFRANCE_EQ_000.xml"),
                new SweFileResource("RTE_SSH.xml", "/network/RTEFRANCE_SSH_000.xml"),
                new SweFileResource("RTE_TP.xml", "/network/RTEFRANCE_TP_000.xml"),
                new SweFileResource("CRAC.xml", "/network/SWE-CRAC_000.xml"),
                new SweFileResource("BOUNDARY_EQ.xml", "/network/BOUNDARY_EQ.xml"),
                new SweFileResource("BOUNDARY_TP.xml", "/network/BOUNDARY_TP.xml"),
                new SweFileResource("GLSK.xml", "/glsk/glsk.xml"));
        assertNotNull(sweRequest);
        assertEquals("id", sweRequest.getId());
        assertEquals(ProcessType.D2CC, sweRequest.getProcessType());
        assertEquals(now, sweRequest.getTargetProcessDateTime());
        assertEquals("REN_SSH.xml", sweRequest.getRenSsh().getFilename());
        assertEquals("CRAC.xml", sweRequest.getCrac().getFilename());
        assertEquals("BOUNDARY_EQ.xml", sweRequest.getBoundaryEq().getFilename());
        assertEquals("BOUNDARY_TP.xml", sweRequest.getBoundaryTp().getFilename());
        assertEquals("GLSK.xml", sweRequest.getGlsk().getFilename());
    }

}

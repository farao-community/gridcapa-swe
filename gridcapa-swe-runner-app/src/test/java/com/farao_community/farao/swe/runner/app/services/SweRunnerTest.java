/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.data.crac_impl.CracImpl;
import com.farao_community.farao.swe.runner.api.resource.ProcessType;
import com.farao_community.farao.swe.runner.api.resource.SweFileResource;
import com.farao_community.farao.swe.runner.api.resource.SweRequest;
import com.farao_community.farao.swe.runner.api.resource.SweResponse;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
class SweRunnerTest {

    @Autowired
    SweRunner sweRunner;

    @MockBean
    NetworkService networkImporter;

    @MockBean
    FileImporter fileImporter;

    @Test
    void run() {
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
        when(networkImporter.importNetwork(sweRequest)).thenReturn(Network.create("network-id", "format"));
        when(fileImporter.importCimCracFromUrlWithNetwork(any(), any(Network.class)))
                .thenReturn(new CracImpl("crac-id", "name"));
        SweResponse sweResponse = sweRunner.run(sweRequest);
        Assertions.assertNotNull(sweResponse);
        Assertions.assertEquals(sweRequest.getId(), sweResponse.getId());
    }
}

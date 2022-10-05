package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.data.crac_creation.creator.cim.CimCrac;
import com.farao_community.farao.data.crac_impl.CracImpl;
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
import static org.mockito.ArgumentMatchers.anyString;
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
        SweRequest sweRequest = new SweRequest("id", now,
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
                new SweFileResource("BOUNDARY_TP.xml", "/network/BOUNDARY_TP.xml"));
        when(networkImporter.importNetwork(sweRequest)).thenReturn(Network.create("network-id", "format"));
        when(fileImporter.importCracFromCimCracAndNetwork(any(CimCrac.class), any(OffsetDateTime.class), any(Network.class), anyString()))
                .thenReturn(new CracImpl("crac-id", "name"));
        SweResponse sweResponse = sweRunner.run(sweRequest);
        Assertions.assertNotNull(sweResponse);
        Assertions.assertEquals(sweRequest.getId(), sweResponse.getId());
    }
}

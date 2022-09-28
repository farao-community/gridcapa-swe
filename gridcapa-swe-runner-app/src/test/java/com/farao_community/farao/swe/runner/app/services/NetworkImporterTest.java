package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.swe.runner.api.resource.SweFileResource;
import com.farao_community.farao.swe.runner.api.resource.SweRequest;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
class NetworkImporterTest {

    @Autowired
    private NetworkService networkImporter;

    private SweRequest sweRequest;

    @BeforeAll
    void setUp() {
        sweRequest = new SweRequest("id", OffsetDateTime.now(),
                new SweFileResource("CORESO_SV.xml", "/network/CORESO-CE_SV_000.xml"),
                new SweFileResource("REE_EQ.xml", "/network/REE_EQ_001.xml"),
                new SweFileResource("REE_SSH.xml", "/network/REE_SSH_000.xml"),
                new SweFileResource("REE_TP.xml", "/network/REE_TP_001.xml"),
                new SweFileResource("REN_EQ.xml", "/network/REN_EQ_001.xml"),
                new SweFileResource("REN_SSH.xml", "/network/REN_SSH_000.xml"),
                new SweFileResource("REN_TP.xml", "/network/REN_TP_001.xml"),
                new SweFileResource("RTE_EQ.xml", "/network/RTEFRANCE_EQ_000.xml"),
                new SweFileResource("RTE_SSH.xml", "/network/RTEFRANCE_SSH_000.xml"),
                new SweFileResource("RTE_TP.xml", "/network/RTEFRANCE_TP_000.xml"));
    }

    @Test
    void getCgmListFromRequestTest() {
        List<SweFileResource> cgmFilesFromRequest = networkImporter.getCgmFilesFromRequest(sweRequest);
        assertEquals(10, cgmFilesFromRequest.size());
    }

    @Test
    void importNetworkSuccess() throws URISyntaxException {
        Network network = networkImporter.importFromZip(Paths.get(Objects.requireNonNull(getClass().getResource("/network/MicroGrid.zip")).toURI()).toString());
        assertNotNull(network);
    }
}

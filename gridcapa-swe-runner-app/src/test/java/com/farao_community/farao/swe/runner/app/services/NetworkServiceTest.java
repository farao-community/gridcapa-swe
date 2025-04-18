/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.gridcapa_swe_commons.resource.ProcessType;
import com.farao_community.farao.swe.runner.api.resource.SweFileResource;
import com.farao_community.farao.swe.runner.api.resource.SweRequest;
import com.powsybl.cgmes.extensions.CgmesMetadataModels;
import com.powsybl.cgmes.model.CgmesSubset;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
class NetworkServiceTest {

    @Autowired
    private NetworkService networkImporter;

    private SweRequest sweRequest;

    @BeforeAll
    void setUp() {
        sweRequest = new SweRequest("id", "currentRunId", ProcessType.D2CC, OffsetDateTime.now(),
                new SweFileResource("CORESO_SV.xml", Objects.requireNonNull(getClass().getResource("/network/MicroGrid_SWE/network_SV.xml")).toExternalForm()),
                new SweFileResource("REE_EQ.xml", Objects.requireNonNull(getClass().getResource("/network/MicroGrid_SWE/network_ES_EQ.xml")).toExternalForm()),
                new SweFileResource("REE_SSH.xml", Objects.requireNonNull(getClass().getResource("/network/MicroGrid_SWE/network_ES_SSH.xml")).toExternalForm()),
                new SweFileResource("REE_TP.xml", Objects.requireNonNull(getClass().getResource("/network/MicroGrid_SWE/network_ES_TP.xml")).toExternalForm()),
                new SweFileResource("REN_EQ.xml", Objects.requireNonNull(getClass().getResource("/network/MicroGrid_SWE/network_PT_EQ.xml")).toExternalForm()),
                new SweFileResource("REN_SSH.xml", Objects.requireNonNull(getClass().getResource("/network/MicroGrid_SWE/network_PT_SSH.xml")).toExternalForm()),
                new SweFileResource("REN_TP.xml", Objects.requireNonNull(getClass().getResource("/network/MicroGrid_SWE/network_PT_TP.xml")).toExternalForm()),
                new SweFileResource("RTE_EQ.xml", Objects.requireNonNull(getClass().getResource("/network/MicroGrid_SWE/network_FR_EQ.xml")).toExternalForm()),
                new SweFileResource("RTE_SSH.xml", Objects.requireNonNull(getClass().getResource("/network/MicroGrid_SWE/network_FR_SSH.xml")).toExternalForm()),
                new SweFileResource("RTE_TP.xml", Objects.requireNonNull(getClass().getResource("/network/MicroGrid_SWE/network_FR_TP.xml")).toExternalForm()),
                new SweFileResource("CRAC.xml", "/network/SWE-CRAC_000.xml"),
                new SweFileResource("BOUNDARY_EQ.xml", Objects.requireNonNull(getClass().getResource("/network/MicroGrid_SWE/20171002T0930Z_ENTSO-E_EQ_BD_2.xml")).toExternalForm()),
                new SweFileResource("BOUNDARY_TP.xml", Objects.requireNonNull(getClass().getResource("/network/MicroGrid_SWE/20171002T0930Z_ENTSO-E_EQ_BD_2.xml")).toExternalForm()),
                new SweFileResource("GLSK.xml", "/glsk/glsk.xml"),
                new ArrayList<>());
    }

    @Test
    void importNetworkSuccess() throws URISyntaxException {
        Network network = networkImporter.importFromZip(Paths.get(Objects.requireNonNull(getClass().getResource("/network/MicroGrid.zip")).toURI()).toString());
        assertNotNull(network);
    }

    @Test
    void importMergedNetwork() {
        Network mergedNetwork = networkImporter.importMergedNetwork(sweRequest);
        assertNotNull(mergedNetwork);
        assertTrue(mergedNetwork.getSubnetworks()
                .stream()
                .map(n -> (CgmesMetadataModels) n.getExtension(CgmesMetadataModels.class))
                .allMatch(a -> a.getModelForSubset(CgmesSubset.EQUIPMENT_BOUNDARY).isPresent() &&
                               a.getModelForSubset(CgmesSubset.TOPOLOGY_BOUNDARY).isPresent()));
        assertEquals(3, mergedNetwork.getSubnetworks().size());
    }

    @Test
    void addHvdcAndPstToNetwork() {
        Network network = Network.read("hvdc/TestCase16Nodes.xiidm", getClass().getResourceAsStream("/hvdc/TestCase16Nodes.xiidm"));
        assertTrue(network.getTwoWindingsTransformer("twt-test-regulation-id").getPhaseTapChanger().isRegulating());

        networkImporter.addHvdcAndPstToNetwork(network);
        assertEquals(2, network.getHvdcLineCount());
        assertFalse(network.getTwoWindingsTransformer("twt-test-regulation-id").getPhaseTapChanger().isRegulating());
    }
}

/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy.shift;

import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@SpringBootTest
class ZonalScalableProviderTest {

    @Autowired
    private ZonalScalableProvider zonalScalableProvider;
    private final String testDirectory = "/20210209/";
    private final String glskFilename = "cim_Glsk.xml";

    @Test
    void testCountryGeneratorsScalableforFR() {
        String glskUrl = Objects.requireNonNull(getClass().getResource(testDirectory + glskFilename)).toString(); //Glsk file does not contains FR
        OffsetDateTime timestamp = OffsetDateTime.parse("2021-02-09T19:30:00Z");
        Network network = Importers.loadNetwork("hvdc/TestCase16Nodes.xiidm", getClass().getResourceAsStream("/hvdc/TestCase16Nodes.xiidm"));
        ZonalData<Scalable>  zonalData = zonalScalableProvider.get(glskUrl, network, timestamp);
        Scalable scalableFR = zonalData.getData("10YFR-RTE------C");
        assertNotNull(scalableFR);
        assertEquals(5, scalableFR.filterInjections(network).size());
        assertEquals("FFR1AA1 _generator", scalableFR.filterInjections(network).get(0).getId());
    }

}

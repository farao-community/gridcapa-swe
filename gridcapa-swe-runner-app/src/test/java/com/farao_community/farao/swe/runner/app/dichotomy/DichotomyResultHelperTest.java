/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy;

import com.powsybl.openrao.data.cracapi.Crac;

import com.powsybl.openrao.data.cracapi.CracCreationContext;
import com.powsybl.openrao.data.cracapi.parameters.CracCreationParameters;
import com.powsybl.openrao.data.craccreation.creator.cim.parameters.CimCracCreationParameters;
import com.powsybl.openrao.data.craccreation.creator.cim.parameters.RangeActionSpeed;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.farao_community.farao.dichotomy.api.results.LimitingCause;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DichotomyResultHelperTest {
    private Crac crac;
    private RaoResult raoResult;

    @BeforeAll
    void setUp() throws IOException {
        Network network = Network.read("/dichotomy/TestCase16NodesWith2Hvdc.xiidm", getClass().getResourceAsStream("/dichotomy/TestCase16NodesWith2Hvdc.xiidm"));
        InputStream cracIs = getClass().getResourceAsStream("/dichotomy/CIM_CRAC.xml");

        Set<RangeActionSpeed> rangeActionSpeeds = Set.of(new RangeActionSpeed("BBE2AA11 FFR3AA11 1", 1), new RangeActionSpeed("BBE2AA12 FFR3AA12 1", 2), new RangeActionSpeed("PRA_1", 3));
        CimCracCreationParameters cimCracCreationParameters = new CimCracCreationParameters();
        cimCracCreationParameters.setRemedialActionSpeed(rangeActionSpeeds);
        CracCreationParameters cracCreationParameters = new CracCreationParameters();
        cracCreationParameters.addExtension(CimCracCreationParameters.class, cimCracCreationParameters);
        CracCreationContext cracCreationContext = Crac.readWithContext("CIM_CRAC.xml", cracIs, network, OffsetDateTime.of(2021, 4, 2, 12, 30, 0, 0, ZoneOffset.UTC), cracCreationParameters);
        crac = cracCreationContext.getCrac();

        InputStream raoResultIs = getClass().getResourceAsStream("/dichotomy/RaoResult.json");
        raoResult = RaoResult.read(raoResultIs, crac);

    }

    @Test
    void getLimitingCauseTest() {
        String messsage = DichotomyResultHelper.limitingCauseToString(LimitingCause.INDEX_EVALUATION_OR_MAX_ITERATION);
        assertEquals("None", messsage);
    }

    @Test
    void getLimitingElementTest() {
        String limitingElement = DichotomyResultHelper.getLimitingElement(crac, raoResult);
        assertEquals("CNEC-1 - DIRECT - Co-2 - outage", limitingElement);
    }

    @Test
    void getActivatedActionInPreventiveTest() {
        List<String> pras = DichotomyResultHelper.getActivatedActionInPreventive(crac, raoResult);
        assertEquals(2, pras.size());
    }

    @Test
    void getActivatedActionInCurativeTest() {
        List<String> cras = DichotomyResultHelper.getActivatedActionInCurative(crac, raoResult);
        assertEquals(3, cras.size());

    }

}

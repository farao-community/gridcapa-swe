/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.api.CracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.cim.CimCrac;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreator;
import com.farao_community.farao.data.crac_creation.creator.cim.importer.CimCracImporter;
import com.farao_community.farao.data.crac_creation.creator.cim.parameters.CimCracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.cim.parameters.RangeActionSpeed;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_json.RaoResultImporter;
import com.farao_community.farao.dichotomy.api.results.LimitingCause;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;

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
    void setUp() {
        Network network = Importers.loadNetwork("/dichotomy/TestCase16NodesWith2Hvdc.xiidm", getClass().getResourceAsStream("/dichotomy/TestCase16NodesWith2Hvdc.xiidm"));
        InputStream cracIs = getClass().getResourceAsStream("/dichotomy/CIM_CRAC.xml");
        CimCracImporter cracImporter = new CimCracImporter();
        CimCrac cimCrac = cracImporter.importNativeCrac(cracIs);
        CimCracCreator cimCracCreator = new CimCracCreator();

        Set<RangeActionSpeed> rangeActionSpeeds = Set.of(new RangeActionSpeed("BBE2AA11 FFR3AA11 1", 1), new RangeActionSpeed("BBE2AA12 FFR3AA12 1", 2), new RangeActionSpeed("PRA_1", 3));
        CimCracCreationParameters cimCracCreationParameters = new CimCracCreationParameters();
        cimCracCreationParameters.setRemedialActionSpeed(rangeActionSpeeds);
        CracCreationParameters cracCreationParameters = new CracCreationParameters();
        cracCreationParameters.addExtension(CimCracCreationParameters.class, cimCracCreationParameters);

        CracCreationContext cracCreationContext = cimCracCreator.createCrac(cimCrac, network, OffsetDateTime.of(2021, 4, 2, 12, 30, 0, 0, ZoneOffset.UTC), cracCreationParameters);
        crac = cracCreationContext.getCrac();

        InputStream raoResultIs = getClass().getResourceAsStream("/dichotomy/RaoResult.json");
        raoResult = new RaoResultImporter().importRaoResult(raoResultIs, crac);

    }

    @Test
    void getimiting() {
        String messsage = DichotomyResultHelper.limitingCauseToString(LimitingCause.INDEX_EVALUATION_OR_MAX_ITERATION);
        assertEquals("None", messsage);
    }

    @Test
    void getLimitingElementTest() {
        String limitingElement = DichotomyResultHelper.getLimitingElement(crac, raoResult);
        assertEquals("CNEC-2 - OPPOSITE - Co-1 - outage", limitingElement);
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

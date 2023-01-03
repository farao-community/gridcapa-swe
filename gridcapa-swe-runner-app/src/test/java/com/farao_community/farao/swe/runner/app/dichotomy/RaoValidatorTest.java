/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy;

import com.farao_community.farao.rao_runner.starter.RaoRunnerClient;
import com.farao_community.farao.swe.runner.api.resource.ProcessType;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.services.FileExporter;
import com.farao_community.farao.swe.runner.app.services.FileImporter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.OffsetDateTime;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
class RaoValidatorTest {

    @MockBean
    private FileExporter fileExporter;
    @MockBean
    private FileImporter fileImporter;
    @MockBean
    private RaoRunnerClient raoRunnerClient;

    @Test
    void matchingCracPathTest() {
        SweData sweData = new SweData("id", OffsetDateTime.now(), ProcessType.D2CC, null, null, null, null, null, null, null, null, "ES_PT_CRAC", "FR_ES_CRAC");
        RaoValidator raoValidator = new RaoValidator(fileExporter, fileImporter, "raoParamsUrl", raoRunnerClient, sweData, DichotomyDirection.PT_ES);
        Assertions.assertEquals("ES_PT_CRAC", raoValidator.getMatchingCracPath(DichotomyDirection.PT_ES, sweData));
        Assertions.assertEquals("FR_ES_CRAC", raoValidator.getMatchingCracPath(DichotomyDirection.ES_FR, sweData));
    }
}

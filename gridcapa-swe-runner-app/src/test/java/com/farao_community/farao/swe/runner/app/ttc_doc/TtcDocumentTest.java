/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.ttc_doc;

import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.swe.runner.app.dichotomy.DichotomyDirection;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyResult;
import com.farao_community.farao.swe.runner.app.parallelization.ExecutionResult;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@SpringBootTest
class TtcDocumentTest {

    @Test
    void simpleTest() {
        SweDichotomyResult dichotomyResult = new SweDichotomyResult(DichotomyDirection.ES_FR, mock(DichotomyResult.class), Optional.empty(), "exportedCgmesUrl", "", "");
        ExecutionResult<SweDichotomyResult> executionResult = new ExecutionResult<>(List.of(dichotomyResult));
        TtcDocument document = new TtcDocument(executionResult);
        InputStream inputStream = document.buildTtcDocFile();
        assertNotNull(inputStream);
    }
}

/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.swe.runner.api.resource.ProcessType;
import com.farao_community.farao.swe.runner.app.dichotomy.DichotomyDirection;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@SpringBootTest
class CgmesExportServiceTest {

    @Autowired
    private CgmesExportService cgmesExportService;

    @Test
    void testBuildCgmesFilename() {
        SweData sweData = mock(SweData.class);
        when(sweData.getTimestamp()).thenReturn(OffsetDateTime.ofInstant(Instant.parse("2022-11-30T00:00:00Z"), ZoneId.of("UTC")));
        when(sweData.getProcessType()).thenReturn(ProcessType.D2CC);
        String result = cgmesExportService.buildCgmesFilename(sweData, "FR", "ESFR");
        assertEquals("20221130T0000Z_2D_FR_ESFR_001.xml", result);
    }

    @Test
    void testCreateFileType() {
        DichotomyDirection directionEsFr = DichotomyDirection.ES_FR;
        String resultEsFr = cgmesExportService.createFileType(directionEsFr);
        assertEquals("CGM_ESFR", resultEsFr);

        DichotomyDirection directionFrEs = DichotomyDirection.FR_ES;
        String resultFrEs = cgmesExportService.createFileType(directionFrEs);
        assertEquals("CGM_FRES", resultFrEs);

        DichotomyDirection directionEsPt = DichotomyDirection.ES_PT;
        String resultEsPt = cgmesExportService.createFileType(directionEsPt);
        assertEquals("CGM_ESPT", resultEsPt);

        DichotomyDirection directionPtEs = DichotomyDirection.PT_ES;
        String resultPtEs = cgmesExportService.createFileType(directionPtEs);
        assertEquals("CGM_PTES", resultPtEs);
    }

}

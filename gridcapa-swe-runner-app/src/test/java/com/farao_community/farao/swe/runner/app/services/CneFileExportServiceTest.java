/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.swe.runner.api.resource.ProcessType;
import com.farao_community.farao.swe.runner.app.dichotomy.DichotomyDirection;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.powsybl.iidm.network.Network;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.InputStream;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */

@SpringBootTest
class CneFileExportServiceTest {

    public static final String DATE_STRING = "2021-02-09T19:30Z";
    @Autowired
    private CneFileExportService cneFileExportService;

    @MockBean
    private MinioAdapter minioAdapter;

    @Mock
    private SweData sweData;

    @Mock
    private RaoResult raoResult;

    @Mock
    private CimCracCreationContext cracCreationContext;

    @Mock
    private Crac crac;

    @Mock
    private Network network;

    private final OffsetDateTime offsetDateTime = OffsetDateTime.parse(DATE_STRING);
    private final DateTime dateTime = DateTime.parse(DATE_STRING);

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void exportCneUrlEsFrHighestValid() {
        when(sweData.getTimestamp()).thenReturn(offsetDateTime);
        when(sweData.getCracFrEs()).thenReturn(cracCreationContext);
        when(sweData.getNetwork()).thenReturn(network);
        when(network.getCaseDate()).thenReturn(dateTime);
        when(cracCreationContext.getTimeStamp()).thenReturn(offsetDateTime);
        when(cracCreationContext.getCrac()).thenReturn(crac);
        when(minioAdapter.generatePreSignedUrl(anyString())).thenAnswer(i -> i.getArgument(0));
        assertEquals("2021/02/09/20_30/OUTPUTS/20210209_1930_CNE_ESFR_LAST_SECURE.zip", cneFileExportService.exportCneUrl(sweData, raoResult, true, ProcessType.D2CC, DichotomyDirection.ES_FR));
        verify(minioAdapter, Mockito.times(1)).uploadOutputForTimestamp(anyString(), any(InputStream.class), anyString(), anyString(), any(OffsetDateTime.class));
    }

    @Test
    void exportCneUrlFrEsLowestInvalid() {
        when(sweData.getTimestamp()).thenReturn(offsetDateTime);
        when(sweData.getCracFrEs()).thenReturn(cracCreationContext);
        when(sweData.getNetwork()).thenReturn(network);
        when(network.getCaseDate()).thenReturn(dateTime);
        when(cracCreationContext.getTimeStamp()).thenReturn(offsetDateTime);
        when(cracCreationContext.getCrac()).thenReturn(crac);
        when(minioAdapter.generatePreSignedUrl(anyString())).thenAnswer(i -> i.getArgument(0));
        assertEquals("2021/02/09/20_30/OUTPUTS/20210209_1930_CNE_FRES_FIRST_UNSECURE.zip", cneFileExportService.exportCneUrl(sweData, raoResult, false, ProcessType.D2CC, DichotomyDirection.FR_ES));
        verify(minioAdapter, Mockito.times(1)).uploadOutputForTimestamp(anyString(), any(InputStream.class), anyString(), anyString(), any(OffsetDateTime.class));
    }

    @Test
    void exportCneUrlEsPtHighestValid() {
        when(sweData.getTimestamp()).thenReturn(offsetDateTime);
        when(sweData.getCracEsPt()).thenReturn(cracCreationContext);
        when(sweData.getNetwork()).thenReturn(network);
        when(network.getCaseDate()).thenReturn(dateTime);
        when(cracCreationContext.getTimeStamp()).thenReturn(offsetDateTime);
        when(cracCreationContext.getCrac()).thenReturn(crac);
        when(minioAdapter.generatePreSignedUrl(anyString())).thenAnswer(i -> i.getArgument(0));
        assertEquals("2021/02/09/20_30/OUTPUTS/20210209_1930_CNE_ESPT_LAST_SECURE.zip", cneFileExportService.exportCneUrl(sweData, raoResult, true, ProcessType.D2CC, DichotomyDirection.ES_PT));
        verify(minioAdapter, Mockito.times(1)).uploadOutputForTimestamp(anyString(), any(InputStream.class), anyString(), anyString(), any(OffsetDateTime.class));
    }

    @Test
    void exportCneUrlPtEsLowestInvalid() {
        when(sweData.getTimestamp()).thenReturn(offsetDateTime);
        when(sweData.getCracEsPt()).thenReturn(cracCreationContext);
        when(sweData.getNetwork()).thenReturn(network);
        when(network.getCaseDate()).thenReturn(dateTime);
        when(cracCreationContext.getTimeStamp()).thenReturn(offsetDateTime);
        when(cracCreationContext.getCrac()).thenReturn(crac);
        when(minioAdapter.generatePreSignedUrl(anyString())).thenAnswer(i -> i.getArgument(0));
        assertEquals("2021/02/09/20_30/OUTPUTS/20210209_1930_CNE_PTES_FIRST_UNSECURE.zip", cneFileExportService.exportCneUrl(sweData, raoResult, false, ProcessType.D2CC, DichotomyDirection.PT_ES));
        verify(minioAdapter, Mockito.times(1)).uploadOutputForTimestamp(anyString(), any(InputStream.class), anyString(), anyString(), any(OffsetDateTime.class));
    }
}

/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.dichotomy.api.results.DichotomyStepResult;
import com.farao_community.farao.dichotomy.api.results.LimitingCause;
import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import com.farao_community.farao.gridcapa_swe_commons.resource.ProcessType;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyValidationData;
import com.powsybl.iidm.network.Network;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.TimeZone;

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
    DichotomyResult<SweDichotomyValidationData> dichotomyResult;

    @Mock
    private CimCracCreationContext cracCreationContext;

    @Mock
    private Crac crac;

    @Mock
    private Network network;

    @Mock
    private DichotomyStepResult<SweDichotomyValidationData> highestValidStep;

    @Mock
    private DichotomyStepResult<SweDichotomyValidationData> lowestInvalidStep;

    private final OffsetDateTime offsetDateTime = OffsetDateTime.parse(DATE_STRING);
    private final DateTime dateTime = DateTime.parse(DATE_STRING);

    MockedStatic<NetworkService> networkService =  Mockito.mockStatic(NetworkService.class);

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Paris"));
    }

    @AfterEach
    public void set() {
        networkService.close();
    }

    @Test
    void exportCneUrlEsFrHighestValid() {
        when(sweData.getTimestamp()).thenReturn(offsetDateTime);
        when(sweData.getCracFrEs()).thenReturn(cracCreationContext);
        when(sweData.getMergedNetwork()).thenReturn(network);
        when(sweData.getProcessType()).thenReturn(ProcessType.D2CC);
        when(network.getCaseDate()).thenReturn(dateTime);
        networkService.when(() -> NetworkService.getNetworkByDirection(sweData, DichotomyDirection.ES_FR)).thenReturn(network);
        when(cracCreationContext.getTimeStamp()).thenReturn(offsetDateTime);
        when(cracCreationContext.getCrac()).thenReturn(crac);
        when(minioAdapter.generatePreSignedUrl(anyString())).thenAnswer(i -> i.getArgument(0));
        when(dichotomyResult.hasValidStep()).thenReturn(true);
        when(dichotomyResult.getHighestValidStep()).thenReturn(highestValidStep);
        when(highestValidStep.getRaoResult()).thenReturn(raoResult);
        assertEquals("2021/02/09/20_30/OUTPUTS/20210209_2030_CNE_ESFR_LAST_SECURE.zip", cneFileExportService.exportCneUrl(sweData, dichotomyResult, true, DichotomyDirection.ES_FR));
        verify(minioAdapter, Mockito.times(1)).uploadOutputForTimestamp(anyString(), any(InputStream.class), anyString(), anyString(), any(OffsetDateTime.class));
    }

    @Test
    void exportCneUrlFrEsLowestInvalid() {
        when(sweData.getTimestamp()).thenReturn(offsetDateTime);
        when(sweData.getCracFrEs()).thenReturn(cracCreationContext);
        when(sweData.getMergedNetwork()).thenReturn(network);
        when(sweData.getProcessType()).thenReturn(ProcessType.D2CC);
        when(network.getCaseDate()).thenReturn(dateTime);
        networkService.when(() -> NetworkService.getNetworkByDirection(sweData, DichotomyDirection.FR_ES)).thenReturn(network);
        when(cracCreationContext.getTimeStamp()).thenReturn(offsetDateTime);
        when(cracCreationContext.getCrac()).thenReturn(crac);
        when(minioAdapter.generatePreSignedUrl(anyString())).thenAnswer(i -> i.getArgument(0));
        when(dichotomyResult.getLowestInvalidStep()).thenReturn(lowestInvalidStep);
        when(lowestInvalidStep.getRaoResult()).thenReturn(raoResult);
        assertEquals("2021/02/09/20_30/OUTPUTS/20210209_2030_CNE_FRES_FIRST_UNSECURE.zip", cneFileExportService.exportCneUrl(sweData, dichotomyResult, false, DichotomyDirection.FR_ES));
        verify(minioAdapter, Mockito.times(1)).uploadOutputForTimestamp(anyString(), any(InputStream.class), anyString(), anyString(), any(OffsetDateTime.class));
    }

    @Test
    void exportCneUrlEsPtHighestValid() {
        when(sweData.getTimestamp()).thenReturn(offsetDateTime);
        when(sweData.getCracEsPt()).thenReturn(cracCreationContext);
        //TODO
        when(sweData.getCracFrEs()).thenReturn(cracCreationContext);
        when(sweData.getMergedNetwork()).thenReturn(network);
        when(sweData.getProcessType()).thenReturn(ProcessType.D2CC);
        when(network.getCaseDate()).thenReturn(dateTime);
        networkService.when(() -> NetworkService.getNetworkByDirection(sweData, DichotomyDirection.ES_PT)).thenReturn(network);
        when(cracCreationContext.getTimeStamp()).thenReturn(offsetDateTime);
        when(cracCreationContext.getCrac()).thenReturn(crac);
        when(dichotomyResult.hasValidStep()).thenReturn(true);
        when(dichotomyResult.getHighestValidStep()).thenReturn(highestValidStep);
        when(highestValidStep.getRaoResult()).thenReturn(raoResult);
        when(minioAdapter.generatePreSignedUrl(anyString())).thenAnswer(i -> i.getArgument(0));
        assertEquals("2021/02/09/20_30/OUTPUTS/20210209_2030_CNE_ESPT_LAST_SECURE.zip", cneFileExportService.exportCneUrl(sweData, dichotomyResult, true, DichotomyDirection.ES_PT));
        verify(minioAdapter, Mockito.times(1)).uploadOutputForTimestamp(anyString(), any(InputStream.class), anyString(), anyString(), any(OffsetDateTime.class));
    }

    @Test
    void exportCneUrlPtEsLowestInvalid() {
        when(sweData.getTimestamp()).thenReturn(offsetDateTime);
        when(sweData.getCracEsPt()).thenReturn(cracCreationContext);
        //TODO
        when(sweData.getCracFrEs()).thenReturn(cracCreationContext);
        when(sweData.getMergedNetwork()).thenReturn(network);
        when(sweData.getProcessType()).thenReturn(ProcessType.D2CC);
        when(network.getCaseDate()).thenReturn(dateTime);
        networkService.when(() -> NetworkService.getNetworkByDirection(sweData, DichotomyDirection.PT_ES)).thenReturn(network);
        when(cracCreationContext.getTimeStamp()).thenReturn(offsetDateTime);
        when(cracCreationContext.getCrac()).thenReturn(crac);
        when(dichotomyResult.getLowestInvalidStep()).thenReturn(lowestInvalidStep);
        when(lowestInvalidStep.getRaoResult()).thenReturn(raoResult);
        when(minioAdapter.generatePreSignedUrl(anyString())).thenAnswer(i -> i.getArgument(0));
        assertEquals("2021/02/09/20_30/OUTPUTS/20210209_2030_CNE_PTES_FIRST_UNSECURE.zip", cneFileExportService.exportCneUrl(sweData, dichotomyResult, false, DichotomyDirection.PT_ES));
        verify(minioAdapter, Mockito.times(1)).uploadOutputForTimestamp(anyString(), any(InputStream.class), anyString(), anyString(), any(OffsetDateTime.class));
    }

    @Test
    void exportLowestInvalidCneUrlFailConditionKOAndReturnNull() {
        when(sweData.getTimestamp()).thenReturn(offsetDateTime);
        when(sweData.getCracEsPt()).thenReturn(cracCreationContext);
        when(sweData.getMergedNetwork()).thenReturn(network);
        //TODO
        when(sweData.getCracFrEs()).thenReturn(cracCreationContext);
        when(sweData.getProcessType()).thenReturn(ProcessType.D2CC);
        when(network.getCaseDate()).thenReturn(dateTime);
        networkService.when(() -> NetworkService.getNetworkByDirection(sweData, DichotomyDirection.PT_ES)).thenReturn(network);
        when(cracCreationContext.getTimeStamp()).thenReturn(offsetDateTime);
        when(cracCreationContext.getCrac()).thenReturn(crac);
        when(dichotomyResult.getLowestInvalidStep()).thenReturn(lowestInvalidStep);
        when(dichotomyResult.getLimitingCause()).thenReturn(LimitingCause.GLSK_LIMITATION);
        when(minioAdapter.generatePreSignedUrl(anyString())).thenAnswer(i -> i.getArgument(0));
        assertEquals("2021/02/09/20_30/OUTPUTS/20210209_2030_CNE_PTES_FIRST_UNSECURE.zip", cneFileExportService.exportCneUrl(sweData, dichotomyResult, false, DichotomyDirection.PT_ES));
        verify(minioAdapter, Mockito.times(1)).uploadOutputForTimestamp(anyString(), any(InputStream.class), anyString(), anyString(), any(OffsetDateTime.class));
    }

    @Test
    void exportHighestValidCneUrlFailConditionKOAndReturnNull() {
        when(sweData.getTimestamp()).thenReturn(offsetDateTime);
        when(sweData.getCracEsPt()).thenReturn(cracCreationContext);

        //TODO
        when(sweData.getCracFrEs()).thenReturn(cracCreationContext);
        when(sweData.getMergedNetwork()).thenReturn(network);
        when(sweData.getProcessType()).thenReturn(ProcessType.D2CC);
        when(network.getCaseDate()).thenReturn(dateTime);
        networkService.when(() -> NetworkService.getNetworkByDirection(sweData, DichotomyDirection.ES_PT)).thenReturn(network);
        when(cracCreationContext.getTimeStamp()).thenReturn(offsetDateTime);
        when(cracCreationContext.getCrac()).thenReturn(crac);
        when(dichotomyResult.hasValidStep()).thenReturn(false);
        when(dichotomyResult.getLimitingCause()).thenReturn(LimitingCause.COMPUTATION_FAILURE);
        when(minioAdapter.generatePreSignedUrl(anyString())).thenAnswer(i -> i.getArgument(0));
        assertEquals("2021/02/09/20_30/OUTPUTS/20210209_2030_CNE_ESPT_LAST_SECURE.zip", cneFileExportService.exportCneUrl(sweData, dichotomyResult, true, DichotomyDirection.ES_PT));
        verify(minioAdapter, Mockito.times(1)).uploadOutputForTimestamp(anyString(), any(InputStream.class), anyString(), anyString(), any(OffsetDateTime.class));
    }
}

/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.farao_community.farao.swe.runner.app.services;

import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracio.cim.craccreator.CimCracCreationContext;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.dichotomy.api.results.DichotomyStepResult;
import com.farao_community.farao.dichotomy.api.results.LimitingCause;
import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import com.farao_community.farao.gridcapa_swe_commons.resource.ProcessType;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyValidationData;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.swecneexporter.xsd.Reason;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    private final ZonedDateTime dateTime = ZonedDateTime.parse(DATE_STRING);

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Paris"));
    }

    @Test
    void exportCneUrlEsFrHighestValid() {
        when(sweData.getTimestamp()).thenReturn(offsetDateTime);
        when(sweData.getCracFrEs()).thenReturn(cracCreationContext);
        when(sweData.getNetworkEsFr()).thenReturn(network);
        when(sweData.getProcessType()).thenReturn(ProcessType.D2CC);
        when(network.getCaseDate()).thenReturn(dateTime);
        when(cracCreationContext.getTimeStamp()).thenReturn(offsetDateTime);
        when(cracCreationContext.getCrac()).thenReturn(crac);
        when(cracCreationContext.getNetworkCaseDate()).thenReturn(offsetDateTime);
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
        when(sweData.getNetworkFrEs()).thenReturn(network);
        when(sweData.getProcessType()).thenReturn(ProcessType.D2CC);
        when(network.getCaseDate()).thenReturn(dateTime);
        when(cracCreationContext.getNetworkCaseDate()).thenReturn(offsetDateTime);
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
        when(sweData.getCracFrEs()).thenReturn(cracCreationContext);
        when(sweData.getNetworkEsPt()).thenReturn(network);
        when(sweData.getProcessType()).thenReturn(ProcessType.D2CC);
        when(network.getCaseDate()).thenReturn(dateTime);
        when(cracCreationContext.getNetworkCaseDate()).thenReturn(offsetDateTime);
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
        when(sweData.getCracFrEs()).thenReturn(cracCreationContext);
        when(sweData.getNetworkPtEs()).thenReturn(network);
        when(sweData.getProcessType()).thenReturn(ProcessType.D2CC);
        when(network.getCaseDate()).thenReturn(dateTime);
        when(cracCreationContext.getNetworkCaseDate()).thenReturn(offsetDateTime);
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
        when(sweData.getNetworkPtEs()).thenReturn(network);
        when(sweData.getCracFrEs()).thenReturn(cracCreationContext);
        when(sweData.getProcessType()).thenReturn(ProcessType.D2CC);
        when(network.getCaseDate()).thenReturn(dateTime);
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
        when(sweData.getCracFrEs()).thenReturn(cracCreationContext);
        when(sweData.getNetworkEsPt()).thenReturn(network);
        when(sweData.getProcessType()).thenReturn(ProcessType.D2CC);
        when(network.getCaseDate()).thenReturn(dateTime);
        when(cracCreationContext.getTimeStamp()).thenReturn(offsetDateTime);
        when(cracCreationContext.getCrac()).thenReturn(crac);
        when(dichotomyResult.hasValidStep()).thenReturn(false);
        when(dichotomyResult.getLimitingCause()).thenReturn(LimitingCause.COMPUTATION_FAILURE);
        when(minioAdapter.generatePreSignedUrl(anyString())).thenAnswer(i -> i.getArgument(0));
        assertEquals("2021/02/09/20_30/OUTPUTS/20210209_2030_CNE_ESPT_LAST_SECURE.zip", cneFileExportService.exportCneUrl(sweData, dichotomyResult, true, DichotomyDirection.ES_PT));
        verify(minioAdapter, Mockito.times(1)).uploadOutputForTimestamp(anyString(), any(InputStream.class), anyString(), anyString(), any(OffsetDateTime.class));
    }

    @Test
    void exportLowestInvalidCneUrlFailConditionKOAndReturnNullWithErrorFileExport() {
        when(sweData.getTimestamp()).thenReturn(offsetDateTime);
        when(sweData.getCracEsPt()).thenReturn(cracCreationContext);
        when(sweData.getNetworkPtEs()).thenReturn(network);
        when(sweData.getCracFrEs()).thenReturn(cracCreationContext);
        when(sweData.getProcessType()).thenReturn(ProcessType.D2CC);
        when(network.getCaseDate()).thenReturn(dateTime);
        when(cracCreationContext.getTimeStamp()).thenReturn(offsetDateTime);
        when(cracCreationContext.getCrac()).thenReturn(crac);
        when(dichotomyResult.getLowestInvalidStep()).thenReturn(lowestInvalidStep);
        when(dichotomyResult.getLimitingCause()).thenReturn(LimitingCause.INDEX_EVALUATION_OR_MAX_ITERATION);
        when(minioAdapter.generatePreSignedUrl(anyString())).thenAnswer(i -> i.getArgument(0));
        assertEquals("2021/02/09/20_30/OUTPUTS/20210209_2030_CNE_PTES_FIRST_UNSECURE.zip", cneFileExportService.exportCneUrl(sweData, dichotomyResult, false, DichotomyDirection.PT_ES));
        verify(minioAdapter, Mockito.times(1)).uploadOutputForTimestamp(anyString(), any(InputStream.class), anyString(), anyString(), any(OffsetDateTime.class));
    }

    @Test
    void exportHighestValidCneUrlFailConditionKOAndReturnNullNoErrorFileExport() {
        when(sweData.getTimestamp()).thenReturn(offsetDateTime);
        when(sweData.getCracEsPt()).thenReturn(cracCreationContext);
        when(sweData.getCracFrEs()).thenReturn(cracCreationContext);
        when(sweData.getNetworkEsPt()).thenReturn(network);
        when(sweData.getProcessType()).thenReturn(ProcessType.D2CC);
        when(network.getCaseDate()).thenReturn(dateTime);
        when(cracCreationContext.getTimeStamp()).thenReturn(offsetDateTime);
        when(cracCreationContext.getCrac()).thenReturn(crac);
        when(dichotomyResult.hasValidStep()).thenReturn(false);
        when(dichotomyResult.getLimitingCause()).thenReturn(LimitingCause.INDEX_EVALUATION_OR_MAX_ITERATION);
        when(minioAdapter.generatePreSignedUrl(anyString())).thenAnswer(i -> i.getArgument(0));
        assertNull(cneFileExportService.exportCneUrl(sweData, dichotomyResult, true, DichotomyDirection.ES_PT));
    }

    @ParameterizedTest
    @CsvSource({
        "GLSK_LIMITATION, B36, GLSK limitation",
        "BALANCE_LOADFLOW_DIVERGENCE, B40, Balance Load Flow divergence",
        "UNKNOWN_TERMINAL_BUS, B32, Unknown terminal bus for balancing",
        "COMPUTATION_FAILURE, B18, Balancing adjustment out of tolerances"
    })
    void testLimitingCause(LimitingCause limitingCause, String code, String message) {
        Reason reason = CneFileExportService.getLimitingCauseErrorReason(limitingCause);
        Assertions.assertThat(reason)
                .isNotNull()
                .hasFieldOrPropertyWithValue("code", code)
                .hasFieldOrPropertyWithValue("text", message);
    }

    @ParameterizedTest
    @EnumSource(value = LimitingCause.class, names = {"CRITICAL_BRANCH", "INDEX_EVALUATION_OR_MAX_ITERATION"})
    void testLimitingCauseDefault(LimitingCause limitingCause) {
        Reason reason = CneFileExportService.getLimitingCauseErrorReason(limitingCause);
        Assertions.assertThat(reason)
                .isNotNull()
                .hasFieldOrPropertyWithValue("code", null)
                .hasFieldOrPropertyWithValue("text", null);
    }
}

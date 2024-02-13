/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.gridcapa.task_manager.api.TaskParameterDto;
import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import com.farao_community.farao.gridcapa_swe_commons.resource.ProcessType;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.swe.runner.app.domain.SweTaskParameters;
import com.farao_community.farao.swe.runner.app.voltage.VoltageMonitoringResultTestUtils;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracimpl.CracImpl;
import com.powsybl.openrao.monitoring.voltagemonitoring.VoltageMonitoringResult;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */
@SpringBootTest
class FileExporterTest {

    @Autowired
    private FileExporter fileExporter;
    @MockBean
    private MinioAdapter minioAdapter;

    private final OffsetDateTime dateTime = OffsetDateTime.parse("2021-04-01T21:30Z");

    @BeforeEach
    void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Paris"));
    }

    @Test
    void saveCracInJsonFormat() {
        Crac crac = new CracImpl("id");
        Mockito.when(minioAdapter.generatePreSignedUrl(Mockito.any())).thenReturn("cracUrl");
        String cracUrl = fileExporter.saveCracInJsonFormat(crac, "test.json", dateTime, ProcessType.D2CC);
        Mockito.verify(minioAdapter, Mockito.times(1)).uploadArtifactForTimestamp(
                Mockito.anyString(),
                Mockito.any(InputStream.class),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.any(OffsetDateTime.class)
        );
        assertEquals("cracUrl", cracUrl);
    }

    @Test
    void saveVoltageMonitoringResultInJson() {
        VoltageMonitoringResult voltageResult = VoltageMonitoringResultTestUtils.getMonitoringResult();
        Mockito.when(minioAdapter.generatePreSignedUrl(Mockito.any())).thenReturn("voltageResult");
        String voltageUrl = fileExporter.saveVoltageMonitoringResultInJsonZip(voltageResult, "voltageResult.json", dateTime, ProcessType.D2CC, "Voltage_ESFR");
        Mockito.verify(minioAdapter, Mockito.times(1)).uploadOutputForTimestamp(
                Mockito.anyString(),
                Mockito.any(InputStream.class),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.any(OffsetDateTime.class)
        );
        assertEquals("voltageResult", voltageUrl);
    }

    @Test
    void saveFailureVoltageMonitoringResultInJson() {
        Mockito.when(minioAdapter.generatePreSignedUrl(Mockito.any())).thenReturn("voltageResult");
        String voltageUrl = fileExporter.saveVoltageMonitoringResultInJsonZip(null, "voltageResult.json", dateTime, ProcessType.D2CC, "Voltage_ESFR");
        Mockito.verify(minioAdapter, Mockito.times(1)).uploadOutputForTimestamp(
                Mockito.anyString(),
                Mockito.any(InputStream.class),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.any(OffsetDateTime.class)
        );
        assertEquals("voltageResult", voltageUrl);
    }

    @Test
    void makeDestinationMinioPath() {
        assertEquals(
                "2021/04/01/23_30/ARTIFACTS/",
                fileExporter.makeDestinationMinioPath(dateTime, FileExporter.FileKind.ARTIFACTS)
        );
        assertEquals(
                "2021/09/01/09_30/OUTPUTS/",
                fileExporter.makeDestinationMinioPath(
                        OffsetDateTime.parse("2021-09-01T07:30Z"),
                        FileExporter.FileKind.OUTPUTS)
        );
    }

    @Test
    void saveRaoParametersTest() {
        SweTaskParameters sweTaskParameters = new SweTaskParameters(List.of(new TaskParameterDto("MAX_CRA", "INT", "27", "12")));
        Mockito.when(minioAdapter.generatePreSignedUrl(Mockito.any())).thenReturn("raoParametersUrl");
        String raoParametersUrl = fileExporter.saveRaoParameters(OffsetDateTime.now(), ProcessType.D2CC, sweTaskParameters, DichotomyDirection.ES_FR);
        Mockito.verify(minioAdapter, Mockito.times(1)).uploadArtifactForTimestamp(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        assertEquals("raoParametersUrl", raoParametersUrl);
    }

    @Test
    void sweRaoParametersEsFrTest() {
        SweTaskParameters sweTaskParameters = new SweTaskParameters(List.of(new TaskParameterDto("MAX_CRA", "INT", "27", "12")));
        RaoParameters raoParameters = fileExporter.getSweRaoParameters(sweTaskParameters, DichotomyDirection.ES_FR);
        assertEquals(2, raoParameters.getNotOptimizedCnecsParameters().getDoNotOptimizeCnecsSecuredByTheirPst().size());
        assertEquals(27, raoParameters.getRaUsageLimitsPerContingencyParameters().getMaxCurativeRa());
    }

    @Test
    void sweRaoParametersEsPtTest() {
        SweTaskParameters sweTaskParameters = new SweTaskParameters(List.of(new TaskParameterDto("MAX_CRA", "INT", "32", "12")));
        RaoParameters raoParameters = fileExporter.getSweRaoParameters(sweTaskParameters, DichotomyDirection.ES_PT);
        assertEquals(0, raoParameters.getNotOptimizedCnecsParameters().getDoNotOptimizeCnecsSecuredByTheirPst().size());
        assertEquals(32, raoParameters.getRaUsageLimitsPerContingencyParameters().getMaxCurativeRa());
    }

    @Test
    void adaptTargetProcessName() {
        assertEquals("SWE_D2CC", fileExporter.adaptTargetProcessName(ProcessType.D2CC));
        assertEquals("SWE_IDCC", fileExporter.adaptTargetProcessName(ProcessType.IDCC));

    }

    @Test
    void cgmFileNameTest() {
        String cgmFileName = fileExporter.getCgmZipFileName(OffsetDateTime.parse("2023-01-01T00:30Z"), DichotomyDirection.ES_FR);
        assertEquals("20230101_0130_CGM_ESFR.zip", cgmFileName);
    }
}

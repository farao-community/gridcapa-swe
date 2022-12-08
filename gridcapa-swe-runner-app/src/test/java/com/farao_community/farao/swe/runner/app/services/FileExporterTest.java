/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_impl.CracImpl;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.monitoring.voltage_monitoring.VoltageMonitoringResult;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.swe.runner.api.resource.ProcessType;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.voltage.VoltageMonitoringResultTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.InputStream;
import java.time.OffsetDateTime;
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
        SweData sweData = new SweData("id", OffsetDateTime.now(), ProcessType.D2CC, null, null, null, null, null, null, null, null, null, null);
        RaoParameters raoParameters = RaoParameters.load();
        Mockito.when(minioAdapter.generatePreSignedUrl(Mockito.any())).thenReturn("raoParametersUrl");
        String raoParametersUrl = fileExporter.saveRaoParameters(sweData);
        Mockito.verify(minioAdapter, Mockito.times(1)).uploadArtifactForTimestamp(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        assertEquals("raoParametersUrl", raoParametersUrl);
    }

    @Test
    void adaptTargetProcessName() {
        assertEquals("SWE_D2CC", fileExporter.adaptTargetProcessName(ProcessType.D2CC));
        assertEquals("SWE_IDCC", fileExporter.adaptTargetProcessName(ProcessType.IDCC));

    }
}

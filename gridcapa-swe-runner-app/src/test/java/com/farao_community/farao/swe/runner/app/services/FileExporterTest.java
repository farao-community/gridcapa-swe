package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_impl.CracImpl;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.swe.runner.api.resource.ProcessType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.InputStream;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FileExporterTest {

    @Autowired
    private FileExporter fileExporter;
    @MockBean
    private MinioAdapter minioAdapter;

    private final OffsetDateTime dateTime = OffsetDateTime.parse("2021-04-01T21:30Z");

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
    void makeDestinationMinioPath() {
        assertEquals(
                "D2CC/2021/04/01/23_30/ARTIFACTS/",
                fileExporter.makeDestinationMinioPath(dateTime, ProcessType.D2CC, FileExporter.FileKind.ARTIFACTS)
        );
        assertEquals(
                "IDCC/2021/09/01/09_30/OUTPUTS/",
                fileExporter.makeDestinationMinioPath(
                        OffsetDateTime.parse("2021-09-01T07:30Z"),
                        ProcessType.IDCC,
                        FileExporter.FileKind.OUTPUTS)
        );
    }
}

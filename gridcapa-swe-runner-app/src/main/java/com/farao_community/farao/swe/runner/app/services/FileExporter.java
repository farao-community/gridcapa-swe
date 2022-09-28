package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_io_api.CracExporters;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.swe.runner.api.exception.SweInvalidDataException;
import com.farao_community.farao.swe.runner.api.resource.ProcessType;
import com.powsybl.commons.datasource.MemDataSource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */
@Service
public class FileExporter {

    private static final String JSON_CRAC_FILE_NAME = "crac.json";
    private static final String MINIO_SEPARATOR = "/";
    private static final String ZONE_ID = "Europe/Paris";

    private final MinioAdapter minioAdapter;

    public FileExporter(MinioAdapter minioAdapter) {
        this.minioAdapter = minioAdapter;
    }

    /**
     * Saves Crac in Json format to MinIO
     * */
    public String saveCracInJsonFormat(Crac crac, OffsetDateTime processTargetDateTime, ProcessType processType) {
        MemDataSource memDataSource = new MemDataSource();
        try (OutputStream os = memDataSource.newOutputStream(JSON_CRAC_FILE_NAME, false)) {
            CracExporters.exportCrac(crac, "Json", os);
        } catch (IOException e) {
            throw new SweInvalidDataException("Error while trying to save converted CRAC file.", e);
        }
        String cracPath = makeDestinationMinioPath(processTargetDateTime, processType, FileKind.ARTIFACTS) + JSON_CRAC_FILE_NAME;
        try (InputStream is = memDataSource.newInputStream(JSON_CRAC_FILE_NAME)) {
            minioAdapter.uploadArtifactForTimestamp(cracPath, is, processType.toString(), "", processTargetDateTime);
        } catch (IOException e) {
            throw new SweInvalidDataException("Error while trying to upload converted CRAC file.", e);
        }
        return minioAdapter.generatePreSignedUrl(cracPath);
    }

    public String makeDestinationMinioPath(OffsetDateTime offsetDateTime, ProcessType processType, FileKind filekind) {
        ZonedDateTime targetDateTime = offsetDateTime.atZoneSameInstant(ZoneId.of(ZONE_ID));
        return processType + MINIO_SEPARATOR
                + targetDateTime.getYear() + MINIO_SEPARATOR
                + String.format("%02d", targetDateTime.getMonthValue()) + MINIO_SEPARATOR
                + String.format("%02d", targetDateTime.getDayOfMonth()) + MINIO_SEPARATOR
                + String.format("%02d", targetDateTime.getHour()) + "_30" + MINIO_SEPARATOR
                + filekind + MINIO_SEPARATOR;
    }

    public enum FileKind {
        ARTIFACTS,
        OUTPUTS

    }
}

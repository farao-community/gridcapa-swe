/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_io_api.CracExporters;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.monitoring.voltage_monitoring.VoltageMonitoringResult;
import com.farao_community.farao.swe.runner.api.exception.SweInvalidDataException;
import com.farao_community.farao.swe.runner.api.resource.ProcessType;
import com.farao_community.farao.swe.runner.app.voltage.VoltageResultMapper;
import com.farao_community.farao.swe.runner.app.voltage.json.VoltageCheckResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
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

    private static final String MINIO_SEPARATOR = "/";
    private static final String ZONE_ID = "Europe/Paris";

    private final MinioAdapter minioAdapter;

    private final VoltageResultMapper voltageResultMapper;

    public FileExporter(MinioAdapter minioAdapter, VoltageResultMapper voltageResultMapper) {
        this.minioAdapter = minioAdapter;
        this.voltageResultMapper = voltageResultMapper;
    }

    /**
     * Saves Crac in Json format to MinIO
     */
    public String saveCracInJsonFormat(Crac crac, String targetName, OffsetDateTime processTargetDateTime, ProcessType processType) {
        MemDataSource memDataSource = new MemDataSource();
        try (OutputStream os = memDataSource.newOutputStream(targetName, false)) {
            CracExporters.exportCrac(crac, "Json", os);
        } catch (IOException e) {
            throw new SweInvalidDataException("Error while trying to save converted CRAC file.", e);
        }
        String cracPath = makeDestinationMinioPath(processTargetDateTime, processType, FileKind.ARTIFACTS) + targetName;
        try (InputStream is = memDataSource.newInputStream(targetName)) {
            minioAdapter.uploadArtifactForTimestamp(cracPath, is, processType.toString(), "", processTargetDateTime);
        } catch (IOException e) {
            throw new SweInvalidDataException("Error while trying to upload converted CRAC file.", e);
        }
        return minioAdapter.generatePreSignedUrl(cracPath);
    }

    public String saveVoltageMonitoringResultInJson(VoltageMonitoringResult result,
                                                    String targetName,
                                                    OffsetDateTime processTargetDateTime,
                                                    ProcessType processType) {
        MemDataSource memDataSource = new MemDataSource();
        try (OutputStream os = memDataSource.newOutputStream(targetName, false)) {
            VoltageCheckResult voltageCheckResult = voltageResultMapper.mapVoltageResult(result);
            ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();
            os.write(objectWriter.writeValueAsBytes(voltageCheckResult));
        } catch (IOException e) {
            throw new SweInvalidDataException("Error while trying to save voltage monitoring result file.", e);
        }
        String voltageResultPath =  makeDestinationMinioPath(processTargetDateTime, processType, FileKind.OUTPUTS) + targetName;
        try (InputStream is = memDataSource.newInputStream(targetName)) {
            minioAdapter.uploadArtifactForTimestamp(voltageResultPath, is, processType.toString(), "", processTargetDateTime);
        } catch (IOException e) {
            throw new SweInvalidDataException("Error while trying to upload converted CRAC file.", e);
        }
        return minioAdapter.generatePreSignedUrl(voltageResultPath);
    }

    protected String makeDestinationMinioPath(OffsetDateTime offsetDateTime, ProcessType processType, FileKind filekind) {
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

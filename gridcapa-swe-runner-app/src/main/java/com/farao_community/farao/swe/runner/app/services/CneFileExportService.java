/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.data.cne_exporter_commons.CneExporterParameters;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.swe_cne_exporter.SweCneExporter;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.swe.runner.api.exception.SweInvalidDataException;
import com.farao_community.farao.swe.runner.api.resource.ProcessType;
import com.farao_community.farao.swe.runner.app.dichotomy.DichotomyDirection;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.powsybl.commons.datasource.MemDataSource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */

@Service
public class CneFileExportService {

    public static final String FILENAME_TIMESTAMP_REGEX = "yyyyMMdd'_'HHmm'_'";
    public static final String TIME_INTERVAL_REGEX = "yyyy'-'MM'-'dd'T'HH':00:00Z'";

    private final FileExporter fileExporter;
    private final MinioAdapter minioAdapter;

    public CneFileExportService(FileExporter fileExporter, MinioAdapter minioAdapter) {
        this.fileExporter = fileExporter;
        this.minioAdapter = minioAdapter;
    }

    public String exportCneUrl(SweData sweData, RaoResult raoResult, boolean isHighestValid, ProcessType processType, DichotomyDirection direction) {

        OffsetDateTime timestamp = sweData.getTimestamp();
        //TODO wait for Farao implementation of parameters
        CneExporterParameters cneExporterParameters = new CneExporterParameters(
                UUID.randomUUID().toString(), 1, "domainId", CneExporterParameters.ProcessType.DAY_AHEAD_CC,
                "10XFR-RTE------Q", CneExporterParameters.RoleType.REGIONAL_SECURITY_COORDINATOR,
                "22XCORESO------S", CneExporterParameters.RoleType.CAPACITY_COORDINATOR,
                extractTimeIntervalFileHeader(timestamp));
        //TODO end

        CimCracCreationContext cracCreationContext = sweData.getCracFrEs();
        if (direction == DichotomyDirection.ES_PT || direction == DichotomyDirection.PT_ES) {
            cracCreationContext = sweData.getCracEsPt();
        }
        MemDataSource memDataSource = new MemDataSource();
        String targetZipFileName = generateCneZipFileName(timestamp, isHighestValid, direction);
        try (OutputStream os = memDataSource.newOutputStream(targetZipFileName, false);
            ZipOutputStream zipOs = new ZipOutputStream(os)) {
            zipOs.putNextEntry(new ZipEntry(fileExporter.zipTargetNameChangeExtension(targetZipFileName, ".xml")));
            SweCneExporter sweCneExporter = new SweCneExporter();
            sweCneExporter.exportCne(cracCreationContext.getCrac(), sweData.getNetwork(), cracCreationContext,
                    raoResult, RaoParameters.load(), cneExporterParameters, zipOs);
            zipOs.closeEntry();
        } catch (IOException e) {
            throw new SweInvalidDataException(String.format("Error while trying to save cne result file [%s].", targetZipFileName), e);
        }
        String cneResultPath =  fileExporter.makeDestinationMinioPath(timestamp, FileExporter.FileKind.OUTPUTS) + targetZipFileName;
        try (InputStream is = memDataSource.newInputStream(targetZipFileName)) {
            minioAdapter.uploadOutputForTimestamp(cneResultPath, is, processType.toString(), generateFileTypeString(isHighestValid, direction), timestamp);
        } catch (IOException e) {
            throw new SweInvalidDataException(String.format("Error while trying to upload cne file [%s].", targetZipFileName), e);
        }
        return minioAdapter.generatePreSignedUrl(cneResultPath);
    }

    private String extractTimeIntervalFileHeader(OffsetDateTime timestamp) {
        OffsetDateTime timestampUtc = OffsetDateTime.of(timestamp.toLocalDateTime(), ZoneOffset.UTC);
        OffsetDateTime timestampUtcPlusOneHour = OffsetDateTime.of(timestamp.toLocalDateTime(), ZoneOffset.UTC).plusHours(1);
        DateTimeFormatter df = DateTimeFormatter.ofPattern(TIME_INTERVAL_REGEX);
        StringBuilder buffer = new StringBuilder(df.format(timestampUtc));
        buffer.append("/");
        buffer.append(df.format(timestampUtcPlusOneHour));
        return buffer.toString();
    }

    private String generateCneZipFileName(OffsetDateTime timestamp, boolean isHighestValid, DichotomyDirection direction) {
        DateTimeFormatter df = DateTimeFormatter.ofPattern(FILENAME_TIMESTAMP_REGEX);
        StringBuilder buffer = new StringBuilder(df.format(timestamp));
        buffer.append(generateFileTypeString(isHighestValid, direction));
        buffer.append(".zip");
        return buffer.toString();
    }

    private String generateFileTypeString(boolean isHighestValid, DichotomyDirection direction) {
        StringBuilder buffer = new StringBuilder("CNE_");
        switch (direction) {
            case ES_FR:
                buffer.append("ESFR");
                break;
            case ES_PT:
                buffer.append("ESPT");
                break;
            case FR_ES:
                buffer.append("FRES");
                break;
            case PT_ES:
                buffer.append("PTES");
                break;
        }
        if (isHighestValid) {
            buffer.append("_lastSecure");
        } else {
            buffer.append("_firstUnsecure");
        }
        return buffer.toString();
    }
}

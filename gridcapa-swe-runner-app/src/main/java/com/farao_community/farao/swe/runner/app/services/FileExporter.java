/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_io_api.CracExporters;
import com.farao_community.farao.minio_adapter.starter.GridcapaFileGroup;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.swe.runner.api.exception.SweInternalException;
import com.farao_community.farao.monitoring.voltage_monitoring.VoltageMonitoringResult;
import com.farao_community.farao.swe.runner.api.exception.SweInvalidDataException;
import com.farao_community.farao.swe.runner.api.resource.ProcessType;
import com.farao_community.farao.swe.runner.app.dichotomy.DichotomyDirection;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.voltage.VoltageResultMapper;
import com.farao_community.farao.swe.runner.app.voltage.json.VoltageCheckResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.iidm.export.Exporters;
import com.powsybl.iidm.network.Network;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Properties;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Service
public class FileExporter {

    private static final String MINIO_SEPARATOR = "/";
    private static final String RAO_PARAMETERS_FILE_NAME = "raoParameters.json";
    private static final String ZONE_ID = "Europe/Paris";
    private static final String PROCESS_TYPE_PREFIX = "SWE_";

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
        String cracPath = makeDestinationMinioPath(processTargetDateTime, FileKind.ARTIFACTS) + targetName;
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
        String voltageResultPath =  makeDestinationMinioPath(processTargetDateTime, FileKind.OUTPUTS) + targetName;
        try (InputStream is = memDataSource.newInputStream(targetName)) {
            minioAdapter.uploadArtifactForTimestamp(voltageResultPath, is, processType.toString(), "", processTargetDateTime);
        } catch (IOException e) {
            throw new SweInvalidDataException("Error while trying to upload converted CRAC file.", e);
        }
        return minioAdapter.generatePreSignedUrl(voltageResultPath);
    }

    public String makeDestinationMinioPath(OffsetDateTime offsetDateTime, FileKind filekind) {
        ZonedDateTime targetDateTime = offsetDateTime.atZoneSameInstant(ZoneId.of(ZONE_ID));
        return targetDateTime.getYear() + MINIO_SEPARATOR
                + String.format("%02d", targetDateTime.getMonthValue()) + MINIO_SEPARATOR
                + String.format("%02d", targetDateTime.getDayOfMonth()) + MINIO_SEPARATOR
                + String.format("%02d", targetDateTime.getHour()) + "_30" + MINIO_SEPARATOR
                + filekind + MINIO_SEPARATOR;
    }

    public String makeDestinationDichotomyPath(OffsetDateTime offsetDateTime, FileKind filekind, DichotomyDirection direction) {
        ZonedDateTime targetDateTime = offsetDateTime.atZoneSameInstant(ZoneId.of(ZONE_ID));
        return  targetDateTime.getYear() + MINIO_SEPARATOR
                + String.format("%02d", targetDateTime.getMonthValue()) + MINIO_SEPARATOR
                + String.format("%02d", targetDateTime.getDayOfMonth()) + MINIO_SEPARATOR
                + String.format("%02d", targetDateTime.getHour()) + "_30" + MINIO_SEPARATOR
                + filekind + MINIO_SEPARATOR
                + direction + MINIO_SEPARATOR;
    }

    public String saveNetworkInArtifact(Network network, String networkFilePath, String fileType, OffsetDateTime processTargetDateTime, ProcessType processType) {
        exportAndUploadNetwork(network, "XIIDM", GridcapaFileGroup.ARTIFACT, networkFilePath, fileType, processTargetDateTime, processType);
        return minioAdapter.generatePreSignedUrl(networkFilePath);
    }

    String exportAndUploadNetwork(Network network, String format, GridcapaFileGroup fileGroup, String filePath, String fileType, OffsetDateTime offsetDateTime, ProcessType processType) {
        try (InputStream is = getNetworkInputStream(network, format)) {
            switch (fileGroup) {
                case OUTPUT:
                    minioAdapter.uploadOutputForTimestamp(filePath, is, adaptTargetProcessName(processType), fileType, offsetDateTime);
                    break;
                case ARTIFACT:
                    minioAdapter.uploadArtifactForTimestamp(filePath.replace(":", ""), is, adaptTargetProcessName(processType), fileType, offsetDateTime);
                    break;
                default:
                    throw new UnsupportedOperationException(String.format("File group %s not supported", fileGroup));
            }
        } catch (IOException e) {
            throw new SweInternalException("Error while trying to save network", e);
        }
        return minioAdapter.generatePreSignedUrl(filePath.replace(":", ""));
    }

    private InputStream getNetworkInputStream(Network network, String format) throws IOException {
        MemDataSource memDataSource = new MemDataSource();
        switch (format) {
            case "UCTE":
                Exporters.export("UCTE", network, new Properties(), memDataSource);
                return memDataSource.newInputStream("", "uct");
            case "XIIDM":
                Exporters.export("XIIDM", network, new Properties(), memDataSource);
                return memDataSource.newInputStream("", "xiidm");
            default:
                throw new UnsupportedOperationException(String.format("Network format %s not supported", format));
        }
    }

    public String saveRaoParameters(SweData sweData) {
        RaoParameters raoParameters = RaoParameters.load();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonRaoParameters.write(raoParameters, baos);
        String raoParametersDestinationPath = makeDestinationMinioPath(sweData.getTimestamp(), FileKind.ARTIFACTS) + RAO_PARAMETERS_FILE_NAME;
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        minioAdapter.uploadArtifactForTimestamp(raoParametersDestinationPath, bais, sweData.getProcessType().toString(), "", sweData.getTimestamp());
        return minioAdapter.generatePreSignedUrl(raoParametersDestinationPath);
    }

    public String exportTtcDocument(SweData sweData, InputStream inputStream, String filename) {
        String filePath = makeDestinationMinioPath(sweData.getTimestamp(), FileKind.OUTPUTS) + filename;
        minioAdapter.uploadOutputForTimestamp(filePath, inputStream, adaptTargetProcessName(sweData.getProcessType()), "", sweData.getTimestamp());
        return minioAdapter.generatePreSignedUrl(filePath);
    }

    private String adaptTargetProcessName(ProcessType processType) {
        return PROCESS_TYPE_PREFIX + processType;
    }

    public enum FileKind {
        ARTIFACTS,
        OUTPUTS
    }
}

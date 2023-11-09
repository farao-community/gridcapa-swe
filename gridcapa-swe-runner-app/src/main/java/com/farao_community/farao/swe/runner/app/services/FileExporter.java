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
import com.farao_community.farao.monitoring.voltage_monitoring.VoltageMonitoringResult;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.swe.runner.api.exception.SweInternalException;
import com.farao_community.farao.swe.runner.api.exception.SweInvalidDataException;
import com.farao_community.farao.swe.runner.api.resource.ProcessType;
import com.farao_community.farao.swe.runner.app.configurations.ProcessConfiguration;
import com.farao_community.farao.swe.runner.app.dichotomy.DichotomyDirection;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.voltage.VoltageResultMapper;
import com.farao_community.farao.swe.runner.app.voltage.json.FailureVoltageCheckResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Service
public class FileExporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileExporter.class);
    private final DateTimeFormatter cgmesFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'_'HHmm'_CGM_[direction].zip'");
    private static final String MINIO_SEPARATOR = "/";
    private static final String RAO_PARAMETERS_FILE_NAME = "raoParameters%s.json";
    private static final String PROCESS_TYPE_PREFIX = "SWE_";
    private final DateTimeFormatter networkFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm_'network.xiidm'");
    public static final String MINIO_DESTINATION_PATH_REGEX = "yyyy'/'MM'/'dd'/'HH'_30/[filekind]/'";
    private final MinioAdapter minioAdapter;
    private final VoltageResultMapper voltageResultMapper;
    private static final Map<String, String> UNOPTIMIZED_CNECS_IN_SERIES_WITH_PSTS = Map.of(
            "_0a3cbdb0-cd71-52b0-b93d-cb48c9fea3e2 + _6f6b15b3-9bcc-7864-7669-522e9f06e931", "_7824bc48-fc86-51db-8f9c-01b44933839e",
            "_1d9c658e-1a01-c0ee-d127-a22e1270a242 + _2e81de07-4c22-5aa1-9683-5e51b054f7f8", "_e071a1d4-fef5-1bd9-5278-d195c5597b6e"
    );

    private final ProcessConfiguration processConfiguration;

    public FileExporter(MinioAdapter minioAdapter, VoltageResultMapper voltageResultMapper, ProcessConfiguration processConfiguration) {
        this.minioAdapter = minioAdapter;
        this.voltageResultMapper = voltageResultMapper;
        this.processConfiguration = processConfiguration;
    }

    public void saveMergedNetworkWithHvdc(Network network, OffsetDateTime targetDateTime) {
        MemDataSource memDataSource = new MemDataSource();
        network.write("XIIDM", new Properties(), memDataSource);
        InputStream xiidm;
        try {
            xiidm = memDataSource.newInputStream("", "xiidm");
        } catch (IOException e) {
            throw new SweInternalException("Could not export XIIDM file", e);
        }
        minioAdapter.uploadArtifactForTimestamp("XIIDM/" + networkFormatter.format(targetDateTime), xiidm, "SWE", "", targetDateTime);
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
            LOGGER.info("Crac file {} is available", cracPath);
        } catch (IOException e) {
            throw new SweInvalidDataException("Error while trying to upload converted CRAC file.", e);
        }
        return minioAdapter.generatePreSignedUrl(cracPath);
    }

    public String saveVoltageMonitoringResultInJsonZip(VoltageMonitoringResult result,
                                                       String targetName,
                                                       OffsetDateTime processTargetDateTime,
                                                       ProcessType processType,
                                                       String fileType) {
        MemDataSource memDataSource = new MemDataSource();
        try (OutputStream os = memDataSource.newOutputStream(targetName, false)) {
            Object resultToWrite = getVoltageMonitoringResult(result);
            ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();
            zipSingleFile(os, objectWriter.writeValueAsBytes(resultToWrite), zipTargetNameChangeExtension(targetName, ".json"));
        } catch (IOException e) {
            throw new SweInvalidDataException("Error while trying to save voltage monitoring result file.", e);
        }
        String voltageResultPath = makeDestinationMinioPath(processTargetDateTime, FileKind.OUTPUTS) + targetName;
        try (InputStream is = memDataSource.newInputStream(targetName)) {
            minioAdapter.uploadOutputForTimestamp(voltageResultPath, is, adaptTargetProcessName(processType), fileType, processTargetDateTime);
        } catch (IOException e) {
            throw new SweInvalidDataException("Error while trying to upload converted CRAC file.", e);
        }
        return minioAdapter.generatePreSignedUrl(voltageResultPath);
    }

    private Object getVoltageMonitoringResult(VoltageMonitoringResult result) {
        return result != null ? voltageResultMapper.mapVoltageResult(result) : new FailureVoltageCheckResult();
    }

    public String zipTargetNameChangeExtension(String targetName, String extension) {
        if (StringUtils.isNotBlank(targetName) && targetName.toLowerCase().contains(".zip")) {
            return targetName.replace(".ZIP", extension).replace(".zip", extension);
        }
        return targetName;
    }

    public void zipSingleFile(OutputStream os, byte[] fileContent, String fileName) {
        try (ZipOutputStream zipOs = new ZipOutputStream(os)) {
            zipOs.putNextEntry(new ZipEntry(fileName));
            zipOs.write(fileContent);
            zipOs.closeEntry();
        } catch (IOException e) {
            throw new SweInvalidDataException(String.format("Error while trying to zip file [%s].", fileName), e);
        }
    }

    public String makeDestinationMinioPath(OffsetDateTime offsetDateTime, FileKind filekind) {
        ZonedDateTime targetDateTime = offsetDateTime.atZoneSameInstant(ZoneId.of(processConfiguration.getZoneId()));
        DateTimeFormatter df = DateTimeFormatter.ofPattern(MINIO_DESTINATION_PATH_REGEX);
        return df.format(targetDateTime).replace("[filekind]", filekind.name());
    }

    public String makeDestinationDichotomyPath(OffsetDateTime offsetDateTime, FileKind filekind, DichotomyDirection direction) {
        return makeDestinationMinioPath(offsetDateTime, filekind) + direction + MINIO_SEPARATOR;
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
                network.write("UCTE", new Properties(), memDataSource);
                return memDataSource.newInputStream("", "uct");
            case "XIIDM":
                network.write("XIIDM", new Properties(), memDataSource);
                return memDataSource.newInputStream("", "xiidm");
            default:
                throw new UnsupportedOperationException(String.format("Network format %s not supported", format));
        }
    }

    public String saveRaoParameters(OffsetDateTime timestamp, ProcessType processType, DichotomyDirection direction) {
        RaoParameters raoParameters = getSweRaoParameters(direction);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonRaoParameters.write(raoParameters, baos);
        String raoParametersFileName = String.format(RAO_PARAMETERS_FILE_NAME, direction);
        String raoParametersDestinationPath = makeDestinationMinioPath(timestamp, FileKind.ARTIFACTS) + raoParametersFileName;
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        minioAdapter.uploadArtifactForTimestamp(raoParametersDestinationPath, bais, processType.toString(), "", timestamp);
        return minioAdapter.generatePreSignedUrl(raoParametersDestinationPath);
    }

    RaoParameters getSweRaoParameters(DichotomyDirection direction) {
        RaoParameters raoParameters = RaoParameters.load();
        if (direction.equals(DichotomyDirection.ES_FR) || direction.equals(DichotomyDirection.FR_ES)) {
            // The cnec in series with pst concern only ES/FR border
            raoParameters.getNotOptimizedCnecsParameters().setDoNotOptimizeCnecsSecuredByTheirPst(UNOPTIMIZED_CNECS_IN_SERIES_WITH_PSTS);
        }
        return raoParameters;
    }

    public String exportTtcDocument(SweData sweData, InputStream inputStream, String filename) {
        String filePath = makeDestinationMinioPath(sweData.getTimestamp(), FileKind.OUTPUTS) + filename;
        minioAdapter.uploadOutputForTimestamp(filePath, inputStream, adaptTargetProcessName(sweData.getProcessType()), "TTC", sweData.getTimestamp());
        return minioAdapter.generatePreSignedUrl(filePath);
    }

    public String exportCgmesZipFile(SweData sweData, Map<String, ByteArrayOutputStream> mapCgmesFiles, DichotomyDirection direction, String filetype) throws IOException {
        String cgmesFilename = getCgmZipFileName(sweData.getTimestamp(), direction);
        String cgmesPath = makeDestinationMinioPath(sweData.getTimestamp(), FileKind.OUTPUTS) + cgmesFilename;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zipOs = new ZipOutputStream(baos)) {

            for (var entry : mapCgmesFiles.entrySet()) {
                zipOs.putNextEntry(new ZipEntry(entry.getKey()));
                byte[] bytes = new byte[1024];
                int length;
                InputStream is = new ByteArrayInputStream(entry.getValue().toByteArray());
                while ((length = is.read(bytes)) >= 0) {
                    zipOs.write(bytes, 0, length);
                }
                is.close();
            }
            zipOs.close();
            baos.close();

            try (InputStream is = new ByteArrayInputStream(baos.toByteArray())) {
                minioAdapter.uploadOutputForTimestamp(cgmesPath, is, adaptTargetProcessName(sweData.getProcessType()), filetype, sweData.getTimestamp());
            } catch (IOException e) {
                throw new SweInvalidDataException("Error while trying to upload zipped CGMES file.", e);
            }
        }
        return minioAdapter.generatePreSignedUrl(cgmesPath);
    }

    String getCgmZipFileName(OffsetDateTime offsetDateTime, DichotomyDirection direction) {
        OffsetDateTime localTime = OffsetDateTime.ofInstant(offsetDateTime.toInstant(), ZoneId.of(processConfiguration.getZoneId()));
        return cgmesFormatter.format(localTime).replace("[direction]", direction.getDashName().replace("-", ""));
    }

    public String adaptTargetProcessName(ProcessType processType) {
        return PROCESS_TYPE_PREFIX + processType;
    }

    public enum FileKind {
        ARTIFACTS,
        OUTPUTS
    }
}

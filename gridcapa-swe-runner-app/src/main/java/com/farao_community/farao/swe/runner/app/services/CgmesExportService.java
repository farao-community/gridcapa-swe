/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.gridcapa_swe_commons.configuration.ProcessConfiguration;
import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import com.farao_community.farao.gridcapa_swe_commons.exception.SweInvalidDataException;
import com.farao_community.farao.gridcapa_swe_commons.hvdc.HvdcLinkProcessor;
import com.farao_community.farao.gridcapa_swe_commons.hvdc.parameters.HvdcCreationParameters;
import com.farao_community.farao.gridcapa_swe_commons.hvdc.parameters.SwePreprocessorParameters;
import com.farao_community.farao.gridcapa_swe_commons.hvdc.parameters.json.JsonSwePreprocessorImporter;
import com.farao_community.farao.swe.runner.api.resource.SweFileResource;
import com.farao_community.farao.swe.runner.app.domain.CgmesFileType;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyValidationData;
import com.farao_community.farao.swe.runner.app.utils.UrlValidationService;
import com.powsybl.cgmes.conversion.CgmesExport;
import com.powsybl.cgmes.extensions.CgmesControlArea;
import com.powsybl.cgmes.extensions.CgmesControlAreas;
import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.Properties;

import static com.farao_community.farao.swe.runner.app.services.NetworkService.TSO_BY_COUNTRY;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@Service
public class CgmesExportService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CgmesExportService.class);
    private static final double DEFAULT_P_TOLERANCE = 10;
    private static final DateTimeFormatter CGMES_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm'Z'_'[process]_[tso]_[type]_001.xml'");
    private final Logger businessLogger;
    private final FileExporter fileExporter;

    private final FileImporter fileImporter;
    private final UrlValidationService urlValidationService;

    private static final Properties SSH_FILES_EXPORT_PARAMS = new Properties();

    private static final Properties SV_FILE_EXPORT_PARAMS = new Properties();

    static {
        SSH_FILES_EXPORT_PARAMS.put(CgmesExport.PROFILES, "SSH");
        SSH_FILES_EXPORT_PARAMS.put(CgmesExport.EXPORT_BOUNDARY_POWER_FLOWS, true);

        SV_FILE_EXPORT_PARAMS.put(CgmesExport.PROFILES, "SV");
        SV_FILE_EXPORT_PARAMS.put(CgmesExport.EXPORT_BOUNDARY_POWER_FLOWS, true);
    }

    public CgmesExportService(Logger businessLogger, FileExporter fileExporter, FileImporter fileImporter, UrlValidationService urlValidationService, ProcessConfiguration processConfiguration) {
        this.businessLogger = businessLogger;
        this.fileExporter = fileExporter;
        this.fileImporter = fileImporter;
        this.urlValidationService = urlValidationService;
        String modelingAuthoritySet = processConfiguration.getModelingAuthoritySet();
        SSH_FILES_EXPORT_PARAMS.put(CgmesExport.MODELING_AUTHORITY_SET, modelingAuthoritySet);
        SV_FILE_EXPORT_PARAMS.put(CgmesExport.MODELING_AUTHORITY_SET, modelingAuthoritySet);
    }

    public String buildAndExportCgmesFiles(DichotomyDirection direction, SweData sweData, DichotomyResult<SweDichotomyValidationData> dichotomyResult) {
        if (dichotomyResult.hasValidStep()) {
            businessLogger.info("Start export of the CGMES files");
            String networkWithPraUrl = dichotomyResult.getHighestValidStep().getValidationData().getRaoResponse().getNetworkWithPraFileUrl();
            try (InputStream networkIs = urlValidationService.openUrlStream(networkWithPraUrl)) {
                Network networkWithPra = Network.read("networkWithPra.xiidm", networkIs);
                applyHvdcSetPointToAcEquivalentModel(networkWithPra);
                LoadFlow.run(networkWithPra, networkWithPra.getVariantManager().getWorkingVariantId(), LocalComputationManager.getDefault(), LoadFlowParameters.load());
                Map<String, ByteArrayOutputStream> mapCgmesFiles = generateCgmesFile(networkWithPra, sweData);
                return fileExporter.exportCgmesZipFile(sweData, mapCgmesFiles, direction, buildFileType(direction));
            } catch (IOException e) {
                throw new SweInvalidDataException(String.format("Can not export cgmes file associated with direction %s", direction.getDashName()), e);
            }
        } else {
            businessLogger.error("Dichotomy does not have a valid step, CGMES files won't be exported");
            return null;
        }
    }

    private void applyHvdcSetPointToAcEquivalentModel(Network networkWithPra) {
        SwePreprocessorParameters params = JsonSwePreprocessorImporter.read(getClass().getResourceAsStream("/hvdc/SwePreprocessorParameters.json"));
        Set<HvdcCreationParameters> hvdcCreationParameters = params.getHvdcCreationParametersSet();
        HvdcLinkProcessor.replaceHvdcByEquivalentModel(networkWithPra, hvdcCreationParameters);
    }

    Map<String, ByteArrayOutputStream> generateCgmesFile(Network mergedNetwork, SweData sweData) throws IOException {
        Map<String, ByteArrayOutputStream> mapCgmesFiles = new HashMap<>();
        mapCgmesFiles.putAll(createAllSshFiles(mergedNetwork, sweData));
        mapCgmesFiles.putAll(createCommonFile(mergedNetwork, sweData));
        mapCgmesFiles.putAll(retrieveEqAndTpFiles(sweData));
        return mapCgmesFiles;
    }

    Map<String, ByteArrayOutputStream> createAllSshFiles(Network mergedNetwork, SweData sweData) throws IOException {
        LOGGER.info("Building SSH files");
        Map<String, ByteArrayOutputStream> mapSshFiles = new HashMap<>();
        Map<Country, Network> subnetworksByCountry = new EnumMap<>(Country.class);
        mergedNetwork.getSubnetworks().forEach(network -> {
            if (network.getCountries().size() != 1) {
                LOGGER.error("Subnetwork with id {} contains countries : {}, it will not be exported to SSH file", network.getNameOrId(), network.getCountries());
            } else {
                Country country = network.getCountries().stream().toList().get(0);
                subnetworksByCountry.put(country, network);
            }
        });

        for (Map.Entry<Country, String> entry : TSO_BY_COUNTRY.entrySet()) {
            Country country = entry.getKey();
            String tso = entry.getValue();
            if (subnetworksByCountry.containsKey(country)) {
                LOGGER.info("Building cgmes files for country {}", country);
                mapSshFiles.putAll(createOneSsh(subnetworksByCountry.get(country), sweData, tso));
            }
        }
        return mapSshFiles;
    }

    private Map<String, ByteArrayOutputStream> retrieveEqAndTpFiles(SweData sweData) throws IOException {
        LOGGER.info("Retrieving EQ & TP files");
        Map<String, ByteArrayOutputStream> mapFiles = new HashMap<>();
        mapFiles.putAll(createOneFile(sweData, CgmesFileType.RTE_TP));
        mapFiles.putAll(createOneFile(sweData, CgmesFileType.REE_TP));
        mapFiles.putAll(createOneFile(sweData, CgmesFileType.REN_TP));
        mapFiles.putAll(createOneFile(sweData, CgmesFileType.RTE_EQ));
        mapFiles.putAll(createOneFile(sweData, CgmesFileType.REE_EQ));
        mapFiles.putAll(createOneFile(sweData, CgmesFileType.REN_EQ));
        return mapFiles;
    }

    private Map<String, ByteArrayOutputStream> createOneSsh(Network network, SweData sweData, String tso) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            updateControlAreasExtension(network);
            MemDataSource memDataSource = new MemDataSource();
            network.write("CGMES", SSH_FILES_EXPORT_PARAMS, memDataSource);
            String filenameFromCgmesExport = network.getNameOrId() + "_SSH.xml";
            baos.write(memDataSource.getData(filenameFromCgmesExport));
            String newFileName = buildCgmesFilename(sweData, tso, "SSH");
            return Map.of(newFileName, baos);
        }
    }

    private Map<String, ByteArrayOutputStream> createOneFile(SweData sweData, CgmesFileType cgmesFileType) throws IOException {
        try (InputStream inputStream = getInputStreamFromData(sweData, cgmesFileType);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            IOUtils.copy(inputStream, outputStream);
            return Map.of(buildCgmesFilename(sweData, cgmesFileType.getTso(), cgmesFileType.getFileType()), outputStream);
        }
    }

    private InputStream getInputStreamFromData(SweData sweData, CgmesFileType cgmesFileType) {
        SweFileResource sweFileResource = sweData.getMapCgmesInputFiles().get(cgmesFileType);
        if (sweFileResource != null) {
            return fileImporter.importCgmesFiles(sweFileResource.getUrl());
        } else {
            throw new SweInvalidDataException(String.format("Can not find file associated with %s", cgmesFileType.name()));
        }
    }

    private void updateControlAreasExtension(Network network) {
        CgmesControlAreas controlAreas = network.getExtension(CgmesControlAreas.class);
        if (controlAreas != null && controlAreas.getCgmesControlAreas().size() == 1) {
            // We use this method for each subnetwork, we should have only one ControlArea by subnetwork
            Optional<CgmesControlArea> controlAreaOpt = controlAreas.getCgmesControlAreas().stream().findFirst();
            controlAreaOpt.ifPresent(controlArea -> {
                controlArea.setNetInterchange(computeNetInterchange(network));
                controlArea.setPTolerance(DEFAULT_P_TOLERANCE);
            });
        }
    }

    private double computeNetInterchange(Network network) {
        return network.getDanglingLineStream().filter(dl -> !Double.isNaN(dl.getBoundary().getP())).mapToDouble(dl -> dl.getBoundary().getP()).sum();
    }

    private Map<String, ByteArrayOutputStream> createCommonFile(Network network, SweData sweData) throws IOException {
        LOGGER.info("Building SV file");
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            MemDataSource memDataSource = new MemDataSource();
            network.write("CGMES", SV_FILE_EXPORT_PARAMS, memDataSource);
            String filenameFromCgmesExport = network.getNameOrId() + "_SV.xml";
            os.write(memDataSource.getData(filenameFromCgmesExport));
            String outputFilename = buildCgmesFilename(sweData, "CGMSWE", "SV");
            return Map.of(outputFilename, os);
        }
    }

    String buildCgmesFilename(SweData sweData, String tso, String type) {
        return CGMES_FORMATTER.format(sweData.getTimestamp())
                .replace("[process]", sweData.getProcessType().getCode())
                .replace("[tso]", tso)
                .replace("[type]", type);
    }

    String buildFileType(DichotomyDirection direction) {
        return "CGM_" + direction.getShortName();
    }
}

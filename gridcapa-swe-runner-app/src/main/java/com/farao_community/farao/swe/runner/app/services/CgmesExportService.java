/*
/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
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
import com.farao_community.farao.gridcapa_swe_commons.resource.ProcessType;
import com.farao_community.farao.swe.runner.api.resource.SweFileResource;
import com.farao_community.farao.swe.runner.app.domain.CgmesFileType;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyValidationData;
import com.farao_community.farao.swe.runner.app.domain.SweTaskParameters;
import com.farao_community.farao.swe.runner.app.utils.OpenLoadFlowParametersUtil;
import com.farao_community.farao.swe.runner.app.utils.UrlValidationService;
import com.powsybl.cgmes.conversion.CgmesExport;
import com.powsybl.cgmes.extensions.CgmesControlArea;
import com.powsybl.cgmes.extensions.CgmesControlAreas;
import com.powsybl.cgmes.extensions.CgmesMetadataModels;
import com.powsybl.cgmes.extensions.CgmesMetadataModelsAdder;
import com.powsybl.cgmes.model.CgmesMetadataModel;
import com.powsybl.cgmes.model.CgmesSubset;
import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.ExportersServiceLoader;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import static com.farao_community.farao.swe.runner.app.services.NetworkService.TSO_BY_COUNTRY;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@Service
public class CgmesExportService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CgmesExportService.class);
    private static final double DEFAULT_P_TOLERANCE = 10;
    private static final String DEFAULT_VERSION = "001";
    private static final DateTimeFormatter CGMES_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm'Z'_'[process]_[tso]_[type]_[version]'");
    public static final String MODELING_AUTHORITY_DEFAULT_VALUE = "https://farao-community.github.io/";
    private final Logger businessLogger;
    private final FileExporter fileExporter;

    private final FileImporter fileImporter;
    private final UrlValidationService urlValidationService;
    private final ProcessConfiguration processConfiguration;

    private static final Properties SSH_FILES_EXPORT_PARAMS = new Properties();

    private static final Properties SV_FILE_EXPORT_PARAMS = new Properties();

    static {
        SSH_FILES_EXPORT_PARAMS.put(CgmesExport.PROFILES, "SSH");
        SSH_FILES_EXPORT_PARAMS.put(CgmesExport.EXPORT_BOUNDARY_POWER_FLOWS, true);
        SSH_FILES_EXPORT_PARAMS.put(CgmesExport.NAMING_STRATEGY, "cgmes");

        SV_FILE_EXPORT_PARAMS.put(CgmesExport.PROFILES, "SV");
        SV_FILE_EXPORT_PARAMS.put(CgmesExport.EXPORT_BOUNDARY_POWER_FLOWS, true);
        SV_FILE_EXPORT_PARAMS.put(CgmesExport.NAMING_STRATEGY, "cgmes");
        SV_FILE_EXPORT_PARAMS.put(CgmesExport.UPDATE_DEPENDENCIES, false);
    }

    public CgmesExportService(Logger businessLogger, FileExporter fileExporter, FileImporter fileImporter, UrlValidationService urlValidationService, ProcessConfiguration processConfiguration) {
        this.businessLogger = businessLogger;
        this.fileExporter = fileExporter;
        this.fileImporter = fileImporter;
        this.urlValidationService = urlValidationService;
        this.processConfiguration = processConfiguration;
        SV_FILE_EXPORT_PARAMS.put(CgmesExport.MODELING_AUTHORITY_SET, processConfiguration.getModelingAuthorityMap().getOrDefault("SV", MODELING_AUTHORITY_DEFAULT_VALUE));
    }

    public String buildAndExportCgmesFiles(DichotomyDirection direction, SweData sweData, DichotomyResult<SweDichotomyValidationData> dichotomyResult, SweTaskParameters sweTaskParameters) {
        if (dichotomyResult.hasValidStep()) {
            businessLogger.info("Start export of the CGMES files");
            String networkWithPraUrl = dichotomyResult.getHighestValidStep().getValidationData().getRaoResponse().getNetworkWithPraFileUrl();
            try (InputStream networkIs = urlValidationService.openUrlStream(networkWithPraUrl)) {
                Network networkWithPra = Network.read("networkWithPra.xiidm", networkIs);
                applyHvdcSetPointToAcEquivalentModel(networkWithPra);
                LoadFlowParameters loadFlowParameters = OpenLoadFlowParametersUtil.getLoadFlowParameters(sweTaskParameters);
                LoadFlow.run(networkWithPra, networkWithPra.getVariantManager().getWorkingVariantId(), LocalComputationManager.getDefault(), loadFlowParameters);
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
        List<String> outputSshIds = new ArrayList<>();
        mapCgmesFiles.putAll(createAllSshFiles(mergedNetwork, sweData, outputSshIds));
        mapCgmesFiles.putAll(createCommonFile(mergedNetwork, sweData, outputSshIds));
        mapCgmesFiles.putAll(retrieveEqAndTpFiles(sweData));
        return mapCgmesFiles;
    }

    Map<String, ByteArrayOutputStream> createAllSshFiles(Network mergedNetwork, SweData sweData, List<String> outputSshIds) throws IOException {
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
                mapSshFiles.putAll(createOneSsh(subnetworksByCountry.get(country), sweData, tso, outputSshIds));
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

    private Map<String, ByteArrayOutputStream> createOneSsh(Network network, SweData sweData, String tso, List<String> outputSshIds) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            updateControlAreasExtension(network);
            MemDataSource memDataSource = new MemDataSource();
            updateModelAuthorityParameter(tso);
            ReportNode reporterSsh = ReportNode
                    .newRootReportNode()
                    .withMessageTemplate("CgmesId", tso)
                    .build();
            network.write(new ExportersServiceLoader(), "CGMES", SSH_FILES_EXPORT_PARAMS, memDataSource, reporterSsh);
            outputSshIds.add(getCgmesIdFromReporter(reporterSsh));

            String filenameFromCgmesExport = network.getNameOrId() + "_SSH.xml";
            baos.write(memDataSource.getData(filenameFromCgmesExport));
            CgmesMetadataModels modelsExtension = network.getExtension(CgmesMetadataModels.class);
            String sshVersionInFileName = modelsExtension != null ?
                    getFormattedVersionString(modelsExtension.getModelForSubset(CgmesSubset.STEADY_STATE_HYPOTHESIS).orElseThrow().getVersion()) : DEFAULT_VERSION;
            String newFileName = buildCgmesFilename(sweData, tso, "SSH", sshVersionInFileName);
            return Map.of(newFileName, baos);
        }
    }

    private static String getFormattedVersionString(int version) {
        return String.format("%03d", version + 1);
    }

    private String getCgmesIdFromReporter(ReportNode reporterSsh) {
        for (ReportNode report : reporterSsh.getChildren()) {
            if ("CgmesId".equals(report.getMessageKey())) {
                return report.getMessageTemplate();
            }
        }
        return "no id";
    }

    private void updateModelAuthorityParameter(String tso) {
        if (tso.equals(TSO_BY_COUNTRY.get(Country.FR))) {
            SSH_FILES_EXPORT_PARAMS.put(CgmesExport.MODELING_AUTHORITY_SET,
                    processConfiguration.getModelingAuthorityMap().getOrDefault(TSO_BY_COUNTRY.get(Country.FR), MODELING_AUTHORITY_DEFAULT_VALUE));
        } else if (tso.equals(TSO_BY_COUNTRY.get(Country.ES))) {
            SSH_FILES_EXPORT_PARAMS.put(CgmesExport.MODELING_AUTHORITY_SET,
                    processConfiguration.getModelingAuthorityMap().getOrDefault(TSO_BY_COUNTRY.get(Country.ES), MODELING_AUTHORITY_DEFAULT_VALUE));
        } else if (tso.equals(TSO_BY_COUNTRY.get(Country.PT))) {
            SSH_FILES_EXPORT_PARAMS.put(CgmesExport.MODELING_AUTHORITY_SET,
                    processConfiguration.getModelingAuthorityMap().getOrDefault(TSO_BY_COUNTRY.get(Country.PT), MODELING_AUTHORITY_DEFAULT_VALUE));
        }
    }

    private Map<String, ByteArrayOutputStream> createOneFile(SweData sweData, CgmesFileType cgmesFileType) throws IOException {
        try (InputStream inputStream = getInputStreamFromData(sweData, cgmesFileType);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            IOUtils.copy(inputStream, outputStream);
            return Map.of(buildCgmesFilename(sweData, cgmesFileType.getTso(), cgmesFileType.getFileType(), DEFAULT_VERSION), outputStream);
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

    private Map<String, ByteArrayOutputStream> createCommonFile(Network network, SweData sweData, List<String> outputSshIds) throws IOException {
        LOGGER.info("Building SV file");
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            MemDataSource memDataSource = new MemDataSource();
            addSvMetadataExtension(network, outputSshIds);
            network.write("CGMES", SV_FILE_EXPORT_PARAMS, memDataSource);
            String filenameFromCgmesExport = network.getNameOrId() + "_SV.xml";
            os.write(memDataSource.getData(filenameFromCgmesExport));
            String outputFilename = buildCgmesFilename(sweData, "CGMSWE", "SV", DEFAULT_VERSION);
            return Map.of(outputFilename, os);
        }
    }

    private void addSvMetadataExtension(Network network, List<String> outputSshIds) {
        // For the SV file, the dependentOn should contain TP and SSH ids
        // The ids of TP are present in the subnetwork SV dependentOn
        List<String> svDependantOn = new ArrayList<>();
        StringBuilder svId = new StringBuilder();
        fillInitialSvIds(network, svDependantOn, svId);
        svDependantOn.addAll(outputSshIds);
        String svModelingAuthority = processConfiguration.getModelingAuthorityMap().getOrDefault("SV", MODELING_AUTHORITY_DEFAULT_VALUE);
        network.newExtension(CgmesMetadataModelsAdder.class)
                .newModel()
                .setId(svId.toString())
                .setSubset(CgmesSubset.STATE_VARIABLES)
                .setDescription("SV Model")
                .setVersion(0)
                .addProfile("http://state-variables")
                .setModelingAuthoritySet(svModelingAuthority)
                .add()
                .add();
        CgmesMetadataModels cgmModelsExtension = network.getExtension(CgmesMetadataModels.class);
        cgmModelsExtension.getModelForSubset(CgmesSubset.STATE_VARIABLES).ifPresent(
                svModel -> svModel.addDependentOn(svDependantOn));
    }

    private void fillInitialSvIds(Network network, List<String> svDependencies, StringBuilder svId) {
        Network subnetwork = (Network) network.getSubnetworks().toArray()[0];
        CgmesMetadataModels modelsExtension = subnetwork.getExtension(CgmesMetadataModels.class);
        if (modelsExtension != null) {
            modelsExtension.getModelForSubset(CgmesSubset.STATE_VARIABLES).ifPresent(
                    svModel -> {
                        svId.append(svModel.getId());
                        List<String> initialSvDependantOn = copyListDependencies(svModel);
                        removeInitialSshFromInitialDependencies(network, initialSvDependantOn);
                        svDependencies.addAll(initialSvDependantOn);
                    });
        }
    }

    private static void removeInitialSshFromInitialDependencies(Network network, List<String> initialSvDependantOn) {
        network.getSubnetworks()
                .stream()
                .map(subNetwork -> subNetwork.getExtension(CgmesMetadataModels.class))
                .filter(Objects::nonNull)
                .map(modelsExtension -> ((CgmesMetadataModels) modelsExtension).getModelForSubset(CgmesSubset.STEADY_STATE_HYPOTHESIS))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(CgmesMetadataModel::getId)
                .forEach(initialSvDependantOn::remove);
    }

    @NotNull
    private static List<String> copyListDependencies(CgmesMetadataModel svModel) {
        if (svModel != null) {
            return new ArrayList<>(svModel.getDependentOn());
        }
        return new ArrayList<>();
    }

    String buildCgmesFilename(final SweData sweData,
                              final String tso,
                              final String type,
                              final String version) {
        return CGMES_FORMATTER.format(sweData.getTimestamp())
                .replace("[process]", ProcessType.IDCC_IDCF == sweData.getProcessType() ?
                        computeTimeDifference(sweData.getTimestamp()) : sweData.getProcessType().getCode())
                .replace("[tso]", tso)
                .replace("[type]", type)
                .replace("[version]", version);
    }

    private CharSequence computeTimeDifference(final OffsetDateTime timestamp) {
        final long hoursDifference = Math.min(23, Math.abs(ChronoUnit.HOURS.between(OffsetDateTime.now(), timestamp)));
        return String.format("%02d", hoursDifference);
    }

    String buildFileType(DichotomyDirection direction) {
        return "CGM_" + direction.getShortName();
    }
}

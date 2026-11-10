/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.dichotomy.api.results.DichotomyStepResult;
import com.farao_community.farao.gridcapa_swe_commons.configuration.ProcessConfiguration;
import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import com.farao_community.farao.gridcapa_swe_commons.exception.SweInvalidDataException;
import com.farao_community.farao.gridcapa_swe_commons.exception.SweInvalidDataNoDetailsException;
import com.farao_community.farao.gridcapa_swe_commons.hvdc.HvdcInformation;
import com.farao_community.farao.gridcapa_swe_commons.hvdc.HvdcLinkProcessor;
import com.farao_community.farao.gridcapa_swe_commons.hvdc.parameters.HvdcCreationParameters;
import com.farao_community.farao.gridcapa_swe_commons.hvdc.parameters.SwePreprocessorParameters;
import com.farao_community.farao.gridcapa_swe_commons.hvdc.parameters.json.JsonSwePreprocessorImporter;
import com.farao_community.farao.gridcapa_swe_commons.loadflow.LoadFlowUtil;
import com.farao_community.farao.gridcapa_swe_commons.resource.ProcessType;
import com.farao_community.farao.swe.runner.api.resource.SweFileResource;
import com.farao_community.farao.swe.runner.app.domain.CgmesFileType;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyValidationData;
import com.farao_community.farao.swe.runner.app.domain.SweTaskParameters;
import com.farao_community.farao.swe.runner.app.utils.OpenLoadFlowParametersUtil;
import com.farao_community.farao.swe.runner.app.utils.UrlValidationService;
import com.powsybl.cgmes.conversion.CgmesExport;
import com.powsybl.cgmes.conversion.CgmesImport;
import com.powsybl.cgmes.conversion.export.CgmesExportUtil;
import com.powsybl.cgmes.conversion.naming.NamingStrategyFactory;
import com.powsybl.cgmes.extensions.CgmesMetadataModels;
import com.powsybl.cgmes.extensions.CgmesMetadataModelsAdder;
import com.powsybl.cgmes.model.CgmesMetadataModel;
import com.powsybl.cgmes.model.CgmesNames;
import com.powsybl.cgmes.model.CgmesSubset;
import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TopologyKind;
import com.powsybl.loadflow.LoadFlowParameters;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private static final int DEFAULT_VERSION = 1;
    private static final DateTimeFormatter CGMES_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm'Z'_'[process]_[tso]_[type]_[version]'");
    private static final Properties SV_FILE_EXPORT_PARAMS = new Properties();

    public static final String MODELING_AUTHORITY_DEFAULT_VALUE = "https://farao-community.github.io/";

    private final Logger businessLogger;
    private final FileExporter fileExporter;
    private final FileImporter fileImporter;
    private final UrlValidationService urlValidationService;
    private final ProcessConfiguration processConfiguration;

    static {
        SV_FILE_EXPORT_PARAMS.put(CgmesExport.PROFILES, "SV");
        SV_FILE_EXPORT_PARAMS.put(CgmesExport.EXPORT_BOUNDARY_POWER_FLOWS, true);
        SV_FILE_EXPORT_PARAMS.put(CgmesExport.NAMING_STRATEGY, NamingStrategyFactory.CGMES);
        SV_FILE_EXPORT_PARAMS.put(CgmesExport.UPDATE_DEPENDENCIES, false);
    }

    public CgmesExportService(Logger businessLogger,
                              FileExporter fileExporter,
                              FileImporter fileImporter,
                              UrlValidationService urlValidationService,
                              ProcessConfiguration processConfiguration) {
        this.businessLogger = businessLogger;
        this.fileExporter = fileExporter;
        this.fileImporter = fileImporter;
        this.urlValidationService = urlValidationService;
        this.processConfiguration = processConfiguration;
        SV_FILE_EXPORT_PARAMS.put(CgmesExport.MODELING_AUTHORITY_SET, processConfiguration.getModelingAuthorityMap().getOrDefault("SV", MODELING_AUTHORITY_DEFAULT_VALUE));
    }

    public String buildAndExportLastSecureCgmesFiles(final DichotomyDirection direction,
                                                     final SweData sweData,
                                                     final Path temporaryBoundariesDirectory,
                                                     final DichotomyResult<SweDichotomyValidationData> dichotomyResult,
                                                     final SweTaskParameters sweTaskParameters) {
        if (dichotomyResult.hasValidStep()) {
            businessLogger.info("Start export of the Last Secure CGMES files");
            final DichotomyStepResult<SweDichotomyValidationData> highestValidStep = dichotomyResult.getHighestValidStep();
            return buildAndExportCgmesFiles(direction, sweData, temporaryBoundariesDirectory, sweTaskParameters, highestValidStep, true);
        } else {
            businessLogger.error("Dichotomy does not have a valid step, Last Secure CGMES files won't be exported");
            return null;
        }
    }

    public String buildAndExportFirstUnsecureCgmesFiles(final DichotomyDirection direction,
                                                        final SweData sweData,
                                                        final Path temporaryBoundariesDirectory,
                                                        final DichotomyResult<SweDichotomyValidationData> dichotomyResult,
                                                        final SweTaskParameters sweTaskParameters) {
        final DichotomyStepResult<SweDichotomyValidationData> lowestInvalidStep = dichotomyResult.getLowestInvalidStep();

        if (lowestInvalidStep == null || lowestInvalidStep.getValidationData() == null) {
            businessLogger.error("Dichotomy does not have an invalid step, First Unsecure Shifted CGMES files won't be exported");
            return null;
        }

        businessLogger.info("Start export of the First Unsecure CGMES files");
        return buildAndExportCgmesFiles(direction, sweData, temporaryBoundariesDirectory, sweTaskParameters, lowestInvalidStep, false);
    }

    String buildAndExportCgmesFiles(final DichotomyDirection direction,
                                    final SweData sweData,
                                    final Path temporaryBoundariesDirectory,
                                    final SweTaskParameters sweTaskParameters,
                                    final DichotomyStepResult<SweDichotomyValidationData> stepResult,
                                    final boolean isHighestValid) {
        final String networkWithPraUrl = stepResult.getValidationData().getRaoResponse().getNetworkWithPraFileUrl();

        LOGGER.info("BOUNDARY_LOCATION: {}", temporaryBoundariesDirectory);

        try (final InputStream networkIs = urlValidationService.openUrlStream(networkWithPraUrl)) {
            final Network networkWithPra = Network.read("networkWithPra.xiidm", networkIs);
            applyHvdcSetPointToAcEquivalentModel(networkWithPra, sweData.getHvdcInformationList());
            final LoadFlowParameters loadFlowParameters = OpenLoadFlowParametersUtil.getLoadFlowParameters(sweTaskParameters);
            LoadFlowUtil.runLoadFlowWithMdc(networkWithPra, networkWithPra.getVariantManager().getWorkingVariantId(), loadFlowParameters);
            final Map<String, ByteArrayOutputStream> mapCgmesFiles = generateCgmesFile(networkWithPra, sweData, temporaryBoundariesDirectory);
            return fileExporter.exportCgmesZipFile(sweData, mapCgmesFiles, direction, buildFileType(direction, isHighestValid), isHighestValid);
        } catch (final IOException e) {
            throw new SweInvalidDataException(String.format("Can not export cgmes file associated with direction %s", direction.getDashName()), e);
        }
    }

    /*
     * CGMES file profiles depend on the network topology. We consider topology to be the same for all switches.
     * Node-Breaker topology - SSH and TP files created / Bus-Breaker topology - SSH files created, TP files retrieved.
     * */
    private static List<String> defineCgmesProfilesFromTypology(final Network network) {
        TopologyKind networkTopology = network.getSwitchStream().map(s -> s.getVoltageLevel().getTopologyKind()).findFirst().orElse(TopologyKind.BUS_BREAKER);
        boolean isNodeBreakerTopology = networkTopology == TopologyKind.NODE_BREAKER;
        LOGGER.info("Network Topology Kind - {}", networkTopology);
        if (isNodeBreakerTopology) {
            return List.of("SSH", "TP");
        } else {
            return List.of("SSH");
        }
    }

    private void applyHvdcSetPointToAcEquivalentModel(Network networkWithPra,
                                                      List<HvdcInformation> hvdcInformationList) {
        try {
            SwePreprocessorParameters params = JsonSwePreprocessorImporter.read(getClass().getResourceAsStream("/hvdc/SwePreprocessorParameters.json"));
            Set<HvdcCreationParameters> hvdcCreationParameters = params.getHvdcCreationParametersSet();
            HvdcLinkProcessor.replaceHvdcByEquivalentModel(networkWithPra, hvdcCreationParameters, hvdcInformationList);
        } catch (SweInvalidDataNoDetailsException e) {
            businessLogger.warn(e.getMessage());
        }
    }

    Map<String, ByteArrayOutputStream> generateCgmesFile(Network mergedNetwork,
                                                         SweData sweData,
                                                         Path temporaryBoundariesDirectory) throws IOException {
        List<String> inputSshIds = new ArrayList<>();
        List<String> outputSshIds = new ArrayList<>();
        Map<String, ByteArrayOutputStream> mapCgmesFiles = new HashMap<>(createAllTsoFiles(mergedNetwork, sweData, inputSshIds, outputSshIds, temporaryBoundariesDirectory));
        mapCgmesFiles.putAll(createCommonFile(mergedNetwork, sweData, inputSshIds, outputSshIds));
        retrieveEqFiles(sweData, mapCgmesFiles);
        // Retrieving TP files if not created
        List<String> createdTpFiles = mapCgmesFiles.keySet().stream().filter(key -> key.contains("TP")).toList();
        for (Map.Entry<Country, String> entry : TSO_BY_COUNTRY.entrySet()) {
            String tso = entry.getValue();
            boolean tsoInCreatedTp = createdTpFiles.stream().anyMatch(file -> file.contains(tso));
            if (!tsoInCreatedTp) {
                LOGGER.info("Retrieving TP file for {}", tso);
                retrieveTpFiles(sweData, mapCgmesFiles, tso);
            }
        }
        return mapCgmesFiles;
    }

    Map<String, ByteArrayOutputStream> createAllTsoFiles(Network mergedNetwork,
                                                         SweData sweData,
                                                         List<String> inputSshIds,
                                                         List<String> outputSshIds,
                                                         Path temporaryBoundariesDirectory) throws IOException {
        Map<String, ByteArrayOutputStream> mapFiles = new HashMap<>();
        Map<Country, Network> subnetworksByCountry = new EnumMap<>(Country.class);
        mergedNetwork.getSubnetworks().forEach(network -> {
            if (network.getCountries().size() != 1) {
                LOGGER.error("Subnetwork with id {} contains countries : {}, it will not be exported to SSH file", network.getNameOrId(), network.getCountries());
            } else {
                Country country = network.getCountries().stream().toList().getFirst();
                subnetworksByCountry.put(country, network);
            }
        });

        for (Map.Entry<Country, String> entry : TSO_BY_COUNTRY.entrySet()) {
            Country country = entry.getKey();
            String tso = entry.getValue();
            if (subnetworksByCountry.containsKey(country)) {
                LOGGER.info("Building cgmes files for country {}", country);
                mapFiles.putAll(createTsoFiles(subnetworksByCountry.get(country), sweData, tso, inputSshIds, outputSshIds, temporaryBoundariesDirectory));
            }
        }
        return mapFiles;
    }

    Map<String, ByteArrayOutputStream> createTsoFiles(Network network,
                                                      SweData sweData,
                                                      String tso,
                                                      List<String> inputSshIds,
                                                      List<String> outputSshIds,
                                                      Path temporaryBoundariesDirectory) throws IOException {
        updateControlAreasExtension(network);
        Map<String, ByteArrayOutputStream> mapFiles = new HashMap<>();
        MemDataSource memDataSource = new MemDataSource();
        List<String> cgmesProfile = defineCgmesProfilesFromTypology(network);
        Properties tsoFilesExportParams = new Properties();
        tsoFilesExportParams.put(CgmesExport.PROFILES, cgmesProfile);
        tsoFilesExportParams.put(CgmesExport.EXPORT_BOUNDARY_POWER_FLOWS, true);
        tsoFilesExportParams.put(CgmesExport.NAMING_STRATEGY, NamingStrategyFactory.CGMES);
        tsoFilesExportParams.put(CgmesExport.CGM_EXPORT_WITH_TP, "IGM");
        tsoFilesExportParams.put(CgmesImport.BOUNDARY_LOCATION, temporaryBoundariesDirectory.toString());
        updateModelAuthorityParameter(tso, tsoFilesExportParams);
        network.write("CGMES", tsoFilesExportParams, memDataSource);
        int version = getMetadataModelVersion(network);
        updateSshMetadataModel(network, inputSshIds, outputSshIds, version);
        for (String profile : cgmesProfile) {
            LOGGER.info("Creating CGMES file {}", profile);
            putAndRenameFile(network.getNameOrId(), sweData, tso, memDataSource, mapFiles, profile, version);
        }
        return mapFiles;
    }

    private void retrieveEqFiles(SweData sweData,
                                 Map<String, ByteArrayOutputStream> mapCgmesFiles) throws IOException {
        LOGGER.info("Retrieving EQ files");
        mapCgmesFiles.putAll(createOneFile(sweData, CgmesFileType.RTE_EQ));
        mapCgmesFiles.putAll(createOneFile(sweData, CgmesFileType.REE_EQ));
        mapCgmesFiles.putAll(createOneFile(sweData, CgmesFileType.REN_EQ));
    }

    /* TP files are only retrieved if the topology kind is not Node Breaker for a given TSO */
    private void retrieveTpFiles(SweData sweData,
                                 Map<String, ByteArrayOutputStream> mapCgmesFiles,
                                 String tso) throws IOException {
        if (tso.equals(TSO_BY_COUNTRY.get(Country.FR))) {
            mapCgmesFiles.putAll(createOneFile(sweData, CgmesFileType.RTE_TP));
        } else if (tso.equals(TSO_BY_COUNTRY.get(Country.ES))) {
            mapCgmesFiles.putAll(createOneFile(sweData, CgmesFileType.REE_TP));
        } else if (tso.equals(TSO_BY_COUNTRY.get(Country.PT))) {
            mapCgmesFiles.putAll(createOneFile(sweData, CgmesFileType.REN_TP));
        }
    }

    private void updateModelAuthorityParameter(String tso,
                                               Properties tsoFilesExportParams) {
        if (tso.equals(TSO_BY_COUNTRY.get(Country.FR))) {
            tsoFilesExportParams.put(CgmesExport.MODELING_AUTHORITY_SET,
                                     processConfiguration.getModelingAuthorityMap().getOrDefault(TSO_BY_COUNTRY.get(Country.FR), MODELING_AUTHORITY_DEFAULT_VALUE));
        } else if (tso.equals(TSO_BY_COUNTRY.get(Country.ES))) {
            tsoFilesExportParams.put(CgmesExport.MODELING_AUTHORITY_SET,
                                     processConfiguration.getModelingAuthorityMap().getOrDefault(TSO_BY_COUNTRY.get(Country.ES), MODELING_AUTHORITY_DEFAULT_VALUE));
        } else if (tso.equals(TSO_BY_COUNTRY.get(Country.PT))) {
            tsoFilesExportParams.put(CgmesExport.MODELING_AUTHORITY_SET,
                                     processConfiguration.getModelingAuthorityMap().getOrDefault(TSO_BY_COUNTRY.get(Country.PT), MODELING_AUTHORITY_DEFAULT_VALUE));
        }
    }

    private void updateSshMetadataModel(Network network,
                                        List<String> inputSshIds,
                                        List<String> outputSshIds,
                                        int version) {
        CgmesMetadataModels modelsExtension = network.getExtension(CgmesMetadataModels.class);
        String newSshId = "urn:uuid:" + CgmesExportUtil.getUniqueRandomId();
        if (modelsExtension != null && modelsExtension.getModelForSubset(CgmesSubset.STEADY_STATE_HYPOTHESIS).isPresent()) {
            Optional<CgmesMetadataModel> modelForSsh = modelsExtension.getModelForSubset(CgmesSubset.STEADY_STATE_HYPOTHESIS);
            if (modelForSsh.isPresent()) {
                Set<String> dependentOn = modelForSsh.get().getDependentOn();
                String initialId = modelForSsh.get().getId();
                inputSshIds.add(initialId);
                modelForSsh.get().clearDependencies().clearSupersedes().setVersion(version).setId(newSshId)
                        .addDependentOn(dependentOn).addSupersedes(initialId);
            }
        } else {
            network.newExtension(CgmesMetadataModelsAdder.class)
                    .newModel()
                    .setId(newSshId)
                    .setSubset(CgmesSubset.STEADY_STATE_HYPOTHESIS)
                    .setDescription("SSH Model")
                    .setVersion(version)
                    .addProfile("http://entsoe.eu/CIM/SteadyStateHypothesis/1/1")
                    .setModelingAuthoritySet(MODELING_AUTHORITY_DEFAULT_VALUE)
                    .add()
                    .add();
        }
        outputSshIds.add(newSshId);
    }

    private int getMetadataModelVersion(Network network) {
        // the version of ssh should be incremented from the initial version
        // The version in the output filename should be the same as in the "fullModel"
        CgmesMetadataModels modelsExtension = network.getExtension(CgmesMetadataModels.class);
        int version = DEFAULT_VERSION;
        if (modelsExtension != null && modelsExtension.getModelForSubset(CgmesSubset.STEADY_STATE_HYPOTHESIS).isPresent()) {
            Optional<CgmesMetadataModel> modelForSsh = modelsExtension.getModelForSubset(CgmesSubset.STEADY_STATE_HYPOTHESIS);
            if (modelForSsh.isPresent()) {
                version = modelForSsh.get().getVersion() + 1;
            }
        }
        return version;
    }

    private static String getFormattedVersionString(int version) {
        return String.format("%03d", version);
    }

    private Map<String, ByteArrayOutputStream> createOneFile(SweData sweData,
                                                             CgmesFileType cgmesFileType) throws IOException {
        try (InputStream inputStream = getInputStreamFromData(sweData, cgmesFileType);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            IOUtils.copy(inputStream, outputStream);
            return Map.of(buildCgmesFilename(sweData, cgmesFileType.getTso(), cgmesFileType.getFileType(), getFormattedVersionString(DEFAULT_VERSION)), outputStream);
        }
    }

    private InputStream getInputStreamFromData(SweData sweData,
                                               CgmesFileType cgmesFileType) {
        SweFileResource sweFileResource = sweData.getMapCgmesInputFiles().get(cgmesFileType);
        if (sweFileResource != null) {
            return fileImporter.importCgmesFiles(sweFileResource.getUrl());
        } else {
            throw new SweInvalidDataException(String.format("Can not find file associated with %s", cgmesFileType.name()));
        }
    }

    private void updateControlAreasExtension(final Network network) {
        network.getAreaStream()
                .filter(area -> CgmesNames.CONTROL_AREA_TYPE_KIND_INTERCHANGE.equalsIgnoreCase(area.getAreaType()))
                .findFirst()
                .ifPresent(controlArea -> {
                    controlArea.setInterchangeTarget(computeNetInterchange(network));
                    controlArea.setProperty(CgmesNames.P_TOLERANCE, String.valueOf(DEFAULT_P_TOLERANCE));
                });
    }

    private double computeNetInterchange(Network network) {
        return network.getBoundaryLineStream().filter(bl -> !Double.isNaN(bl.getBoundary().getP())).mapToDouble(bl -> bl.getBoundary().getP()).sum();
    }

    private void putAndRenameFile(String baseName,
                                  SweData sweData,
                                  String tso,
                                  MemDataSource memDataSource,
                                  Map<String, ByteArrayOutputStream> mapFiles,
                                  String type,
                                  int version) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            String defaultFilenameFromCgmesExport = baseName + "_" + type + ".xml";
            baos.write(memDataSource.getData(defaultFilenameFromCgmesExport));
            String newFileName = buildCgmesFilename(sweData, tso, type, getFormattedVersionString(version));
            mapFiles.put(newFileName, baos);
        }
    }

    private Map<String, ByteArrayOutputStream> createCommonFile(Network network,
                                                                SweData sweData,
                                                                List<String> inputSshIds,
                                                                List<String> outputSshIds) throws IOException {
        LOGGER.info("Building SV file");
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            MemDataSource memDataSource = new MemDataSource();
            addSvMetadataExtension(network, inputSshIds, outputSshIds);
            network.write("CGMES", SV_FILE_EXPORT_PARAMS, memDataSource);
            String filenameFromCgmesExport = network.getNameOrId() + "_SV.xml";
            os.write(memDataSource.getData(filenameFromCgmesExport));
            String outputFilename = buildCgmesFilename(sweData, "CGMSWE", "SV", getFormattedVersionString(DEFAULT_VERSION)); // update version
            return Map.of(outputFilename, os);
        }
    }

    private void addSvMetadataExtension(Network network,
                                        List<String> inputSshIds,
                                        List<String> outputSshIds) {
        // For the SV file, the dependentOn should contain TP and SSH ids
        // The ids of TP are present in the subnetwork SV dependentOn
        StringBuilder initialSvId = new StringBuilder();
        List<String> svDependantOn = fillSvDependencies(network, initialSvId, inputSshIds, outputSshIds);
        SV_FILE_EXPORT_PARAMS.put(CgmesExport.MODELING_AUTHORITY_SET, processConfiguration.getModelingAuthorityMap().getOrDefault("SV", MODELING_AUTHORITY_DEFAULT_VALUE));

        String svModelingAuthority = processConfiguration.getModelingAuthorityMap().getOrDefault("SV", MODELING_AUTHORITY_DEFAULT_VALUE);
        String newSvId = "urn:uuid:" + CgmesExportUtil.getUniqueRandomId();
        network.newExtension(CgmesMetadataModelsAdder.class)
                .newModel()
                .setId(newSvId)
                .setSubset(CgmesSubset.STATE_VARIABLES)
                .setDescription("SV Model")
                .setVersion(DEFAULT_VERSION) //Sv version  always set to default version 1
                .addProfile("http://entsoe.eu/CIM/StateVariables/4/1")
                .setModelingAuthoritySet(svModelingAuthority)
                .addSupersedes(initialSvId.toString())
                .add()
                .add();
        CgmesMetadataModels cgmModelsExtension = network.getExtension(CgmesMetadataModels.class);
        cgmModelsExtension.getModelForSubset(CgmesSubset.STATE_VARIABLES).ifPresent(
                svModel -> svModel.addDependentOn(svDependantOn));
    }

    private List<String> fillSvDependencies(Network network,
                                            StringBuilder svId,
                                            List<String> inputSshIds,
                                            List<String> outputSshIds) {
        List<String> svDependencies = new ArrayList<>();
        Network subnetwork = (Network) network.getSubnetworks().toArray()[0];
        CgmesMetadataModels modelsExtension = subnetwork.getExtension(CgmesMetadataModels.class);
        if (modelsExtension != null) {
            modelsExtension.getModelForSubset(CgmesSubset.STATE_VARIABLES).ifPresent(
                    svModel -> {
                        svId.append(svModel.getId());
                        List<String> initialSvDependantOn = copyListDependencies(svModel);
                        initialSvDependantOn.removeAll(inputSshIds);
                        svDependencies.addAll(initialSvDependantOn);
                    });
        }
        svDependencies.addAll(outputSshIds);
        return svDependencies;
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
        final long timeGap = ChronoUnit.HOURS.between(OffsetDateTime.now(), timestamp);
        // Value must be capped to 0 <=  n <= 23
        final long hoursCappedAtMin = Math.min(23, timeGap);
        final long hoursCapped = Math.max(0, hoursCappedAtMin);
        return String.format("%02d", hoursCapped);
    }

    String buildFileType(final DichotomyDirection direction,
                         final boolean isHighestValid) {
        final String fileTypeFormat = "CGM_%s_%s";
        final String fileTypeEnd = isHighestValid ? "LAST_SECURE" : "FIRST_UNSECURE";
        return String.format(fileTypeFormat, direction.getShortName(), fileTypeEnd);
    }
}

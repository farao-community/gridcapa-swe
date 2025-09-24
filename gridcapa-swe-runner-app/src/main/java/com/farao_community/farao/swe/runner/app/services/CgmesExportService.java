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
import com.farao_community.farao.gridcapa_swe_commons.exception.SweInvalidDataNoDetailsException;
import com.farao_community.farao.gridcapa_swe_commons.hvdc.HvdcInformation;
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
import com.powsybl.cgmes.conversion.export.CgmesExportUtil;
import com.powsybl.cgmes.conversion.naming.NamingStrategyFactory;
import com.powsybl.cgmes.extensions.CgmesMetadataModels;
import com.powsybl.cgmes.extensions.CgmesMetadataModelsAdder;
import com.powsybl.cgmes.model.CgmesMetadataModel;
import com.powsybl.cgmes.model.CgmesNames;
import com.powsybl.cgmes.model.CgmesSubset;
import com.powsybl.commons.datasource.MemDataSource;
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
    public static final String MODELING_AUTHORITY_DEFAULT_VALUE = "https://farao-community.github.io/";
    private final Logger businessLogger;
    private final FileExporter fileExporter;

    private final FileImporter fileImporter;
    private final UrlValidationService urlValidationService;
    private final ProcessConfiguration processConfiguration;
    private final RemoveRemoteVoltageRegulationInFranceService removeRemoteVoltageRegulationInFranceService;

    private static final Properties SSH_FILES_EXPORT_PARAMS = new Properties();

    private static final Properties SV_FILE_EXPORT_PARAMS = new Properties();

    static {
        SSH_FILES_EXPORT_PARAMS.put(CgmesExport.PROFILES, "SSH");
        SSH_FILES_EXPORT_PARAMS.put(CgmesExport.EXPORT_BOUNDARY_POWER_FLOWS, true);
        SSH_FILES_EXPORT_PARAMS.put(CgmesExport.NAMING_STRATEGY, NamingStrategyFactory.CGMES);

        SV_FILE_EXPORT_PARAMS.put(CgmesExport.PROFILES, "SV");
        SV_FILE_EXPORT_PARAMS.put(CgmesExport.EXPORT_BOUNDARY_POWER_FLOWS, true);
        SV_FILE_EXPORT_PARAMS.put(CgmesExport.NAMING_STRATEGY, NamingStrategyFactory.CGMES);
        SV_FILE_EXPORT_PARAMS.put(CgmesExport.UPDATE_DEPENDENCIES, false);
    }

    public CgmesExportService(Logger businessLogger, FileExporter fileExporter, FileImporter fileImporter, UrlValidationService urlValidationService, ProcessConfiguration processConfiguration, RemoveRemoteVoltageRegulationInFranceService removeRemoteVoltageRegulationInFranceService) {
        this.businessLogger = businessLogger;
        this.fileExporter = fileExporter;
        this.fileImporter = fileImporter;
        this.urlValidationService = urlValidationService;
        this.processConfiguration = processConfiguration;
        this.removeRemoteVoltageRegulationInFranceService = removeRemoteVoltageRegulationInFranceService;
        SV_FILE_EXPORT_PARAMS.put(CgmesExport.MODELING_AUTHORITY_SET, processConfiguration.getModelingAuthorityMap().getOrDefault("SV", MODELING_AUTHORITY_DEFAULT_VALUE));
    }

    public String buildAndExportCgmesFiles(DichotomyDirection direction, SweData sweData, DichotomyResult<SweDichotomyValidationData> dichotomyResult, SweTaskParameters sweTaskParameters) {
        if (dichotomyResult.hasValidStep()) {
            businessLogger.info("Start export of the CGMES files");
            String networkWithPraUrl = dichotomyResult.getHighestValidStep().getValidationData().getRaoResponse().getNetworkWithPraFileUrl();
            try (InputStream networkIs = urlValidationService.openUrlStream(networkWithPraUrl)) {
                Network networkWithPra = Network.read("networkWithPra.xiidm", networkIs);
                applyHvdcSetPointToAcEquivalentModel(networkWithPra, sweData.getHvdcInformationList());
                LoadFlowParameters loadFlowParameters = OpenLoadFlowParametersUtil.getLoadFlowParameters(sweTaskParameters);
                LoadFlow.run(networkWithPra, networkWithPra.getVariantManager().getWorkingVariantId(), LocalComputationManager.getDefault(), loadFlowParameters);
                removeRemoteVoltageRegulationInFranceService.resetRemoteVoltageRegulationInFrance(networkWithPra, sweData.getReplacedVoltageRegulations());
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

    public void exportLastNetworkwithpra(final DichotomyDirection direction, final SweData sweData, final DichotomyResult<SweDichotomyValidationData> dichotomyResult) {
        if (dichotomyResult.hasValidStep()) {
            String networkWithPraUrl = dichotomyResult.getHighestValidStep().getValidationData().getRaoResponse().getNetworkWithPraFileUrl();
            try (InputStream networkIs = urlValidationService.openUrlStream(networkWithPraUrl)) {
                fileExporter.exportNetworkWithPra(sweData, networkIs, direction, buildFileType(direction));
            } catch (IOException e) {
                throw new SweInvalidDataException(String.format("Can not export networkWithPra file associated with direction %s", direction.getDashName()), e);
            }
        } else {
            businessLogger.error("Dichotomy does not have a valid step, networkWithPra file won't be exported");
        }
    }

    private void applyHvdcSetPointToAcEquivalentModel(Network networkWithPra, List<HvdcInformation> hvdcInformationList) {
        try {
            SwePreprocessorParameters params = JsonSwePreprocessorImporter.read(getClass().getResourceAsStream("/hvdc/SwePreprocessorParameters.json"));
            Set<HvdcCreationParameters> hvdcCreationParameters = params.getHvdcCreationParametersSet();
            HvdcLinkProcessor.replaceHvdcByEquivalentModel(networkWithPra, hvdcCreationParameters, hvdcInformationList);
        } catch (SweInvalidDataNoDetailsException e) {
            businessLogger.warn(e.getMessage());
        }

    }

    Map<String, ByteArrayOutputStream> generateCgmesFile(Network mergedNetwork, SweData sweData) throws IOException {
        Map<String, ByteArrayOutputStream> mapCgmesFiles = new HashMap<>();
        List<String> inputSshIds = new ArrayList<>();
        List<String> outputSshIds = new ArrayList<>();
        mapCgmesFiles.putAll(createAllSshFiles(mergedNetwork, sweData, inputSshIds, outputSshIds));
        mapCgmesFiles.putAll(createCommonFile(mergedNetwork, sweData, inputSshIds, outputSshIds));
        mapCgmesFiles.putAll(retrieveEqAndTpFiles(sweData));
        return mapCgmesFiles;
    }

    Map<String, ByteArrayOutputStream> createAllSshFiles(Network mergedNetwork, SweData sweData, List<String> inputSshIds, List<String> outputSshIds) throws IOException {
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
                mapSshFiles.putAll(createOneSsh(subnetworksByCountry.get(country), sweData, tso, inputSshIds, outputSshIds));
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

    private Map<String, ByteArrayOutputStream> createOneSsh(Network network, SweData sweData, String tso, List<String> inputSshIds, List<String> outputSshIds) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            updateControlAreasExtension(network);
            int sshVersion = updateSshMetadataModel(network, inputSshIds, outputSshIds);
            MemDataSource memDataSource = new MemDataSource();
            updateModelAuthorityParameter(tso);

            network.write(new ExportersServiceLoader(), "CGMES", SSH_FILES_EXPORT_PARAMS, memDataSource);

            String filenameFromCgmesExport = network.getNameOrId() + "_SSH.xml";
            baos.write(memDataSource.getData(filenameFromCgmesExport));

            String newFileName = buildCgmesFilename(sweData, tso, "SSH", getFormattedVersionString(sshVersion));
            return Map.of(newFileName, baos);
        }
    }

    private int updateSshMetadataModel(Network network, List<String> inputSshIds, List<String> outputSshIds) {
        // the version of ssh should be incremented from the initial version
        // The version in the output filename should be the same as in the "fullModel"
        CgmesMetadataModels modelsExtension = network.getExtension(CgmesMetadataModels.class);
        String newSshId = "urn:uuid:" + CgmesExportUtil.getUniqueRandomId();
        if (modelsExtension != null && modelsExtension.getModelForSubset(CgmesSubset.STEADY_STATE_HYPOTHESIS).isPresent()) {
            Optional<CgmesMetadataModel> modelForSsh = modelsExtension.getModelForSubset(CgmesSubset.STEADY_STATE_HYPOTHESIS);
            if (modelForSsh.isPresent()) {
                int initialVersion = modelForSsh.get().getVersion();
                Set<String> dependentOn = modelForSsh.get().getDependentOn();
                String initialId = modelForSsh.get().getId();
                inputSshIds.add(initialId);

                outputSshIds.add(newSshId);
                int version = initialVersion + 1;
                modelForSsh.get().clearDependencies().clearSupersedes().setVersion(version).setId(newSshId)
                        .addDependentOn(dependentOn).addSupersedes(initialId);

                return version;
            } else {
                return 1;
            }

        } else {
            network.newExtension(CgmesMetadataModelsAdder.class)
                    .newModel()
                    .setId(newSshId)
                    .setSubset(CgmesSubset.STEADY_STATE_HYPOTHESIS)
                    .setDescription("SSH Model")
                    .setVersion(DEFAULT_VERSION)
                    .addProfile("http://entsoe.eu/CIM/SteadyStateHypothesis/1/1")
                    .setModelingAuthoritySet(MODELING_AUTHORITY_DEFAULT_VALUE)
                    .add()
                    .add();
            outputSshIds.add(newSshId);
            return DEFAULT_VERSION;
        }
    }

    private static String getFormattedVersionString(int version) {
        return String.format("%03d", version);
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
            return Map.of(buildCgmesFilename(sweData, cgmesFileType.getTso(), cgmesFileType.getFileType(), getFormattedVersionString(DEFAULT_VERSION)), outputStream);
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
        return network.getDanglingLineStream().filter(dl -> !Double.isNaN(dl.getBoundary().getP())).mapToDouble(dl -> dl.getBoundary().getP()).sum();
    }

    private Map<String, ByteArrayOutputStream> createCommonFile(Network network, SweData sweData, List<String> inputSshIds, List<String> outputSshIds) throws IOException {
        LOGGER.info("Building SV file");
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            MemDataSource memDataSource = new MemDataSource();
            addSvMetadataExtension(network, inputSshIds, outputSshIds);
            network.write("CGMES", SV_FILE_EXPORT_PARAMS, memDataSource);
            String filenameFromCgmesExport = network.getNameOrId() + "_SV.xml";
            os.write(memDataSource.getData(filenameFromCgmesExport));
            String outputFilename = buildCgmesFilename(sweData, "CGMSWE", "SV", getFormattedVersionString(DEFAULT_VERSION));
            return Map.of(outputFilename, os);
        }
    }

    private void addSvMetadataExtension(Network network, List<String> inputSshIds, List<String> outputSshIds) {
        // For the SV file, the dependentOn should contain TP and SSH ids
        // The ids of TP are present in the subnetwork SV dependentOn
        StringBuilder initialSvId = new StringBuilder();
        List<String> svDependantOn = fillSvDependencies(network, initialSvId, inputSshIds, outputSshIds);
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

    private List<String> fillSvDependencies(Network network, StringBuilder svId, List<String> inputSshIds, List<String> outputSshIds) {
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

    String buildFileType(DichotomyDirection direction) {
        return "CGM_" + direction.getShortName();
    }
}

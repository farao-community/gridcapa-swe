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
import com.powsybl.cgmes.extensions.CgmesControlArea;
import com.powsybl.cgmes.extensions.CgmesControlAreas;
import com.powsybl.cgmes.extensions.CgmesMetadataModels;
import com.powsybl.cgmes.model.CgmesMetadataModel;
import com.powsybl.cgmes.model.CgmesSubset;
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
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
    private static final String DEFAULT_VERSION = "001";
    private static final DateTimeFormatter CGMES_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm'Z'_'[process]_[tso]_[type]_[version]'");
    public static final String MODELING_AUTHORITY_DEFAULT_VALUE = "https://farao-community.github.io/";
    private final Logger businessLogger;
    private final FileExporter fileExporter;

    private final FileImporter fileImporter;
    private final UrlValidationService urlValidationService;
    private final RemoveRemoteVoltageRegulationInFranceService removeRemoteVoltageRegulationInFranceService;

    private static final Properties SSH_SV_FILE_EXPORT_PARAMS = new Properties();

    static {
        SSH_SV_FILE_EXPORT_PARAMS.put(CgmesExport.PROFILES, List.of("SV", "SSH"));
        SSH_SV_FILE_EXPORT_PARAMS.put(CgmesExport.EXPORT_BOUNDARY_POWER_FLOWS, true);
        SSH_SV_FILE_EXPORT_PARAMS.put(CgmesExport.NAMING_STRATEGY, "cgmes");
        SSH_SV_FILE_EXPORT_PARAMS.put(CgmesExport.CGM_EXPORT, true);
        SSH_SV_FILE_EXPORT_PARAMS.put(CgmesExport.UPDATE_DEPENDENCIES, true);
    }

    public CgmesExportService(final Logger businessLogger,
                              final FileExporter fileExporter,
                              final FileImporter fileImporter,
                              final UrlValidationService urlValidationService,
                              final ProcessConfiguration processConfiguration,
                              final RemoveRemoteVoltageRegulationInFranceService removeRemoteVoltageRegulationInFranceService) {
        this.businessLogger = businessLogger;
        this.fileExporter = fileExporter;
        this.fileImporter = fileImporter;
        this.urlValidationService = urlValidationService;
        this.removeRemoteVoltageRegulationInFranceService = removeRemoteVoltageRegulationInFranceService;
        SSH_SV_FILE_EXPORT_PARAMS.put(CgmesExport.MODELING_AUTHORITY_SET, processConfiguration.getModelingAuthorityMap().getOrDefault("SV", MODELING_AUTHORITY_DEFAULT_VALUE));
    }

    public String buildAndExportCgmesFiles(final DichotomyDirection direction, final SweData sweData, final DichotomyResult<SweDichotomyValidationData> dichotomyResult, final SweTaskParameters sweTaskParameters) {
        if (dichotomyResult.hasValidStep()) {
            businessLogger.info("Start export of the CGMES files");
            final String networkWithPraUrl = dichotomyResult.getHighestValidStep().getValidationData().getRaoResponse().getNetworkWithPraFileUrl();
            try (final InputStream networkIs = urlValidationService.openUrlStream(networkWithPraUrl)) {
                final Network networkWithPra = Network.read("networkWithPra.xiidm", networkIs);
                applyHvdcSetPointToAcEquivalentModel(networkWithPra, sweData.getHvdcInformationList());
                final LoadFlowParameters loadFlowParameters = OpenLoadFlowParametersUtil.getLoadFlowParameters(sweTaskParameters);
                LoadFlow.run(networkWithPra, networkWithPra.getVariantManager().getWorkingVariantId(), LocalComputationManager.getDefault(), loadFlowParameters);
                removeRemoteVoltageRegulationInFranceService.resetRemoteVoltageRegulationInFrance(networkWithPra, sweData.getReplacedVoltageRegulations());
                final Map<String, ByteArrayOutputStream> mapCgmesFiles = generateCgmesFile(networkWithPra, sweData);
                return fileExporter.exportCgmesZipFile(sweData, mapCgmesFiles, direction, buildFileType(direction));
            } catch (final IOException e) {
                throw new SweInvalidDataException(String.format("Can not export cgmes file associated with direction %s", direction.getDashName()), e);
            }
        } else {
            businessLogger.error("Dichotomy does not have a valid step, CGMES files won't be exported");
            return null;
        }
    }

    private void applyHvdcSetPointToAcEquivalentModel(final Network networkWithPra, final List<HvdcInformation> hvdcInformationList) {
        try {
            final SwePreprocessorParameters params = JsonSwePreprocessorImporter.read(getClass().getResourceAsStream("/hvdc/SwePreprocessorParameters.json"));
            final Set<HvdcCreationParameters> hvdcCreationParameters = params.getHvdcCreationParametersSet();
            HvdcLinkProcessor.replaceHvdcByEquivalentModel(networkWithPra, hvdcCreationParameters, hvdcInformationList);
        } catch (final SweInvalidDataNoDetailsException e) {
            businessLogger.warn(e.getMessage());
        }

    }

    Map<String, ByteArrayOutputStream> generateCgmesFile(final Network mergedNetwork, final SweData sweData) throws IOException {
        final Map<String, ByteArrayOutputStream> mapCgmesFiles = new HashMap<>();
        mapCgmesFiles.putAll(createSshSvFiles(mergedNetwork, sweData));
        mapCgmesFiles.putAll(retrieveEqAndTpFiles(sweData));
        return mapCgmesFiles;
    }

    Map<String, ByteArrayOutputStream> createSshSvFiles(final Network mergedNetwork, final SweData sweData) throws IOException {
        LOGGER.info("Building SSH and SV files");
        final Map<String, ByteArrayOutputStream> mapFiles = new HashMap<>();
        mergedNetwork.getSubnetworks().forEach(this::updateControlAreasExtension);
        final MemDataSource memDataSource = new MemDataSource();
        mergedNetwork.write("CGMES", SSH_SV_FILE_EXPORT_PARAMS, memDataSource);
        final String outputVersion = getNextVersion(mergedNetwork);
        for (final Map.Entry<Country, String> entry : TSO_BY_COUNTRY.entrySet()) {
            final Country country = entry.getKey();
            final String tso = entry.getValue();
            try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                final String filenameFromCgmesExport = mergedNetwork.getNameOrId() + "_" + country.toString() + "_SSH.xml";
                if (memDataSource.getData(filenameFromCgmesExport) != null) {
                    baos.write(memDataSource.getData(filenameFromCgmesExport));
                    final String newFileName = buildCgmesFilename(sweData, tso, "SSH", outputVersion);
                    mapFiles.put(newFileName, baos);
                }
            }
        }
        try (final ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            final String filenameFromCgmesExport = mergedNetwork.getNameOrId() + "_SV.xml";
            os.write(memDataSource.getData(filenameFromCgmesExport));
            final String svFilename = buildCgmesFilename(sweData, "CGMSWE", "SV", outputVersion);
            mapFiles.put(svFilename, os);
        }
        return mapFiles;
    }

    /**
     * Rule used in Powsybl CGMES Export : version number is the same for SSH & SV and is equal to the max + 1
     *
     * @param mergedNetwork
     * @return
     */
    private String getNextVersion(final Network mergedNetwork) {
        int maxCurrentVersion = getVersionNumber(mergedNetwork, CgmesSubset.STATE_VARIABLES);
        for (final Network subnetwork : mergedNetwork.getSubnetworks()) {
            maxCurrentVersion = Math.max(getVersionNumber(subnetwork, CgmesSubset.STEADY_STATE_HYPOTHESIS), maxCurrentVersion);
        }
        return getFormattedVersionString(Math.max(maxCurrentVersion + 1, 2));
    }

    private int getVersionNumber(final Network network, final CgmesSubset subset) {
        // Retrieve model version
        // In the case of a CGM export, the SSH subsets are updated and their version number is incremented
        final CgmesMetadataModels networkModels = network.getExtension(CgmesMetadataModels.class);
        final Optional<CgmesMetadataModel> networkSubsetModel = networkModels != null ?
                networkModels.getModelForSubset(subset) :
                Optional.empty();
        return networkSubsetModel.map(CgmesMetadataModel::getVersion).orElse(-1);
    }

    private Map<String, ByteArrayOutputStream> retrieveEqAndTpFiles(final SweData sweData) throws IOException {
        LOGGER.info("Retrieving EQ & TP files");
        final Map<String, ByteArrayOutputStream> mapFiles = new HashMap<>();
        mapFiles.putAll(createOneFile(sweData, CgmesFileType.RTE_TP));
        mapFiles.putAll(createOneFile(sweData, CgmesFileType.REE_TP));
        mapFiles.putAll(createOneFile(sweData, CgmesFileType.REN_TP));
        mapFiles.putAll(createOneFile(sweData, CgmesFileType.RTE_EQ));
        mapFiles.putAll(createOneFile(sweData, CgmesFileType.REE_EQ));
        mapFiles.putAll(createOneFile(sweData, CgmesFileType.REN_EQ));
        return mapFiles;
    }

    private static String getFormattedVersionString(final int version) {
        return String.format("%03d", version);
    }

    private Map<String, ByteArrayOutputStream> createOneFile(final SweData sweData, final CgmesFileType cgmesFileType) throws IOException {
        try (final InputStream inputStream = getInputStreamFromData(sweData, cgmesFileType);
             final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            IOUtils.copy(inputStream, outputStream);
            return Map.of(buildCgmesFilename(sweData, cgmesFileType.getTso(), cgmesFileType.getFileType(), DEFAULT_VERSION), outputStream);
        }
    }

    private InputStream getInputStreamFromData(final SweData sweData, final CgmesFileType cgmesFileType) {
        final SweFileResource sweFileResource = sweData.getMapCgmesInputFiles().get(cgmesFileType);
        if (sweFileResource != null) {
            return fileImporter.importCgmesFiles(sweFileResource.getUrl());
        } else {
            throw new SweInvalidDataException(String.format("Can not find file associated with %s", cgmesFileType.name()));
        }
    }

    private void updateControlAreasExtension(final Network network) {
        final CgmesControlAreas controlAreas = network.getExtension(CgmesControlAreas.class);
        if (controlAreas != null && controlAreas.getCgmesControlAreas().size() == 1) {
            // We use this method for each subnetwork, we should have only one ControlArea by subnetwork
            final Optional<CgmesControlArea> controlAreaOpt = controlAreas.getCgmesControlAreas().stream().findFirst();
            controlAreaOpt.ifPresent(controlArea -> {
                controlArea.setNetInterchange(computeNetInterchange(network));
                controlArea.setPTolerance(DEFAULT_P_TOLERANCE);
            });
        }
    }

    private double computeNetInterchange(final Network network) {
        return network.getDanglingLineStream().filter(dl -> !Double.isNaN(dl.getBoundary().getP())).mapToDouble(dl -> dl.getBoundary().getP()).sum();
    }

    String buildCgmesFilename(final SweData sweData, final String tso, final String type, final String version) {
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

    String buildFileType(final DichotomyDirection direction) {
        return "CGM_" + direction.getShortName();
    }
}

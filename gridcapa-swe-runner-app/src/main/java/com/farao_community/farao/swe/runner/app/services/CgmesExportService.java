/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.swe.runner.api.exception.SweInternalException;
import com.farao_community.farao.swe.runner.api.exception.SweInvalidDataException;
import com.farao_community.farao.swe.runner.app.dichotomy.DichotomyDirection;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyValidationData;
import com.farao_community.farao.swe.runner.app.hvdc.HvdcLinkProcessor;
import com.farao_community.farao.swe.runner.app.hvdc.parameters.HvdcCreationParameters;
import com.farao_community.farao.swe.runner.app.hvdc.parameters.SwePreprocessorParameters;
import com.farao_community.farao.swe.runner.app.hvdc.parameters.json.JsonSwePreprocessorImporter;
import com.farao_community.farao.swe.runner.app.utils.UrlValidationService;
import com.powsybl.cgmes.conversion.export.*;
import com.powsybl.cgmes.extensions.CgmesSvMetadata;
import com.powsybl.commons.xml.XmlUtil;
import com.powsybl.iidm.mergingview.MergingView;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.powsybl.iidm.xml.IidmXmlConstants.INDENT;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Service
public class CgmesExportService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CgmesExportService.class);
    public static final String FR = "RTEFRANCE";
    public static final String ES = "REE";
    public static final String PT = "REN";
    private final DateTimeFormatter cgmesFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm'Z'_'[process]_[country]_[type]_001.xml'");
    private final FileExporter fileExporter;
    private MergingView mergingView;
    private final UrlValidationService urlValidationService;

    public CgmesExportService(FileExporter fileExporter, UrlValidationService urlValidationService) {
        this.fileExporter = fileExporter;
        this.urlValidationService = urlValidationService;
    }

    public String buildAndExportCgmesFiles(DichotomyDirection direction, SweData sweData, DichotomyResult<SweDichotomyValidationData> dichotomyResult) {
        if (dichotomyResult.hasValidStep()) {
            LOGGER.info("Start export of the CGMES files");
            this.mergingView = sweData.getMergingViewData().getMergingView();
            applyRemedialActions(direction, sweData, dichotomyResult);
            String networkWithPraUrl = dichotomyResult.getHighestValidStep().getValidationData().getRaoResponse().getNetworkWithPraFileUrl();
            applyNetworkWithPraResultToMergingView(networkWithPraUrl, mergingView);
            return exportMergingView(sweData, direction);
        } else {
            LOGGER.error("Not valid step, CGMES files wont be exported");
            return null;
        }
    }

    void applyNetworkWithPraResultToMergingView(String networkWithPraUrl, MergingView mergingView) {
        try (InputStream networkIs = urlValidationService.openUrlStream(networkWithPraUrl)) {
            LOGGER.info("Applying last shift to the network");
            Network networkWithPra = Network.read("networkWithPra.xiidm", networkIs);
            mergingView.getGenerators().forEach(generator -> generator.setTargetP(networkWithPra.getGenerator(generator.getId()).getTargetP()));
            mergingView.getLoads().forEach(load -> load.setP0(networkWithPra.getLoad(load.getId()).getP0()));

            LOGGER.info("Applying HVDC values to AC equivalent model");
            // Applying the HVDC set point to the CGMES equivalent model if it was changed during the RAO computation
            SwePreprocessorParameters params = JsonSwePreprocessorImporter.read(getClass().getResourceAsStream("/hvdc/SwePreprocessorParameters.json"));
            Set<HvdcCreationParameters> hvdcCreationParameters = params.getHvdcCreationParametersSet();
            hvdcCreationParameters.forEach(parameter -> HvdcLinkProcessor.connectEquivalentGeneratorsAndLoads(mergingView, parameter, networkWithPra.getHvdcLine(parameter.getId())));

        } catch (IOException e) {
            throw new SweInternalException("Could not export CGMES files", e);
        }
    }

    private void applyRemedialActions(DichotomyDirection direction, SweData sweData, DichotomyResult<SweDichotomyValidationData> dichotomyResult) {
        LOGGER.info("Applying remedial actions to the network");
        Crac matchingCrac = getMatchingCrac(direction, sweData);
        applyNetworkActions(dichotomyResult.getHighestValidStep().getRaoResult().getActivatedNetworkActionsDuringState(matchingCrac.getPreventiveState()));
        applyRangeActions(dichotomyResult.getHighestValidStep().getRaoResult().getActivatedRangeActionsDuringState(matchingCrac.getPreventiveState()),
                dichotomyResult.getHighestValidStep().getRaoResult().getOptimizedSetPointsOnState(matchingCrac.getPreventiveState()));
    }

    private Crac getMatchingCrac(DichotomyDirection direction, SweData sweData) {
        if (direction.equals(DichotomyDirection.ES_FR) || direction.equals(DichotomyDirection.FR_ES)) {
            return sweData.getCracFrEs().getCrac();
        } else if (direction.equals(DichotomyDirection.ES_PT) || direction.equals(DichotomyDirection.PT_ES)) {
            return sweData.getCracEsPt().getCrac();
        }
        throw new SweInvalidDataException("Unknown direction");
    }

    private void applyNetworkActions(Set<NetworkAction> activatedNetworkActionsDuringState) {
        for (NetworkAction action : activatedNetworkActionsDuringState) {
            action.apply(mergingView);
        }
    }

    private void applyRangeActions(Set<RangeAction<?>> activatedRangeActionsDuringState, Map<RangeAction<?>, Double> optimizedSetPointsOnState) {
        for (RangeAction<?> action : activatedRangeActionsDuringState) {
            action.apply(mergingView, optimizedSetPointsOnState.get(action));
        }
    }

    private String exportMergingView(SweData sweData, DichotomyDirection direction) {
        try {
            Map<String, ByteArrayOutputStream> mapCgmesFiles = new HashMap<>();
            mapCgmesFiles.putAll(createAllSshFiles(sweData));
            mapCgmesFiles.putAll(createAllTpFiles(sweData));
            mapCgmesFiles.putAll(createAllEqFiles(sweData));
            mapCgmesFiles.putAll(createCommonFile(sweData.getMergingViewData().getNetworkFr(), sweData.getMergingViewData().getNetworkEs(), sweData.getMergingViewData().getNetworkPt(), sweData));
            return fileExporter.exportCgmesZipFile(sweData, mapCgmesFiles, direction, createFileType(direction));
        } catch (XMLStreamException | IOException e) {
            throw new SweInternalException("Could not export CGMES files", e);
        }
    }

    private Map<String, ByteArrayOutputStream> createAllSshFiles(SweData sweData) throws XMLStreamException, IOException {
        LOGGER.info("Building SSH files");
        Map<String, ByteArrayOutputStream> mapSshFiles = new HashMap<>();
        mapSshFiles.putAll(createOneSsh(sweData.getMergingViewData().getNetworkFr(), sweData, FR));
        mapSshFiles.putAll(createOneSsh(sweData.getMergingViewData().getNetworkEs(), sweData, ES));
        mapSshFiles.putAll(createOneSsh(sweData.getMergingViewData().getNetworkPt(), sweData, PT));
        return mapSshFiles;
    }

    private Map<String, ByteArrayOutputStream> createAllTpFiles(SweData sweData) throws XMLStreamException, IOException {
        LOGGER.info("Building TP files");
        Map<String, ByteArrayOutputStream> mapTpFiles = new HashMap<>();
        mapTpFiles.putAll(createOneTp(sweData.getMergingViewData().getNetworkFr(), sweData, FR));
        mapTpFiles.putAll(createOneTp(sweData.getMergingViewData().getNetworkEs(), sweData, ES));
        mapTpFiles.putAll(createOneTp(sweData.getMergingViewData().getNetworkPt(), sweData, PT));
        return mapTpFiles;
    }

    private Map<String, ByteArrayOutputStream> createAllEqFiles(SweData sweData) throws XMLStreamException, IOException {
        LOGGER.info("Building EQ files");
        Map<String, ByteArrayOutputStream> mapEqFiles = new HashMap<>();
        mapEqFiles.putAll(createOneEq(sweData.getMergingViewData().getNetworkFr(), sweData, FR));
        mapEqFiles.putAll(createOneEq(sweData.getMergingViewData().getNetworkEs(), sweData, ES));
        mapEqFiles.putAll(createOneEq(sweData.getMergingViewData().getNetworkPt(), sweData, PT));
        return mapEqFiles;
    }

    private Map<String, ByteArrayOutputStream> createOneSsh(Network network, SweData sweData, String country) throws IOException, XMLStreamException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            XMLStreamWriter writer = XmlUtil.initializeWriter(true, INDENT, os);
            SteadyStateHypothesisExport.write(network, writer, new CgmesExportContext(network));
            return Map.of(buildCgmesFilename(sweData, country, "SSH"), os);
        }
    }

    private Map<String, ByteArrayOutputStream> createOneTp(Network network, SweData sweData, String country) throws IOException, XMLStreamException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            XMLStreamWriter writer = XmlUtil.initializeWriter(true, INDENT, os);
            TopologyExport.write(network, writer, new CgmesExportContext(network));
            return Map.of(buildCgmesFilename(sweData, country, "TP"), os);
        }
    }

    private Map<String, ByteArrayOutputStream> createOneEq(Network network, SweData sweData, String country) throws IOException, XMLStreamException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            XMLStreamWriter writer = XmlUtil.initializeWriter(true, INDENT, os);
            EquipmentExport.write(network, writer, new CgmesExportContext(network));
            return Map.of(buildCgmesFilename(sweData, country, "EQ"), os);
        }
    }

    private Map<String, ByteArrayOutputStream> createCommonFile(Network n1, Network n2, Network n3, SweData sweData) throws XMLStreamException, IOException {
        LOGGER.info("Building SV file");
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            XMLStreamWriter writer = XmlUtil.initializeWriter(true, INDENT, os);
            StateVariablesExport.write(mergingView, writer, createContext(mergingView, n1, n2, n3));
            return Map.of(buildCgmesFilename(sweData, "CGMSWE", "SV"), os);
        }
    }

    private static CgmesExportContext createContext(MergingView mergingView, Network n1, Network n2, Network n3) {
        CgmesExportContext context = new CgmesExportContext();
        context.setScenarioTime(mergingView.getCaseDate())
                .getSvModelDescription()
                .addDependencies(n1.getExtension(CgmesSvMetadata.class).getDependencies())
                .addDependencies(n2.getExtension(CgmesSvMetadata.class).getDependencies())
                .addDependencies(n3.getExtension(CgmesSvMetadata.class).getDependencies());
        context.addIidmMappings(n1);
        context.addIidmMappings(n2);
        context.addIidmMappings(n3);
        return context;
    }

    String buildCgmesFilename(SweData sweData, String country, String type) {
        String formattedFilename = cgmesFormatter.format(sweData.getTimestamp());
        return formattedFilename.replace("[process]", sweData.getProcessType().getCode()).replace("[country]", country).replace("[type]", type);
    }

    String createFileType(DichotomyDirection direction) {
        StringBuilder sb = new StringBuilder();
        sb.append("CGM_");
        switch (direction) {
            case ES_FR:
                sb.append("ESFR");
                break;
            case FR_ES:
                sb.append("FRES");
                break;
            case ES_PT:
                sb.append("ESPT");
                break;
            case PT_ES:
                sb.append("PTES");
                break;
        }
        return sb.toString();
    }
}

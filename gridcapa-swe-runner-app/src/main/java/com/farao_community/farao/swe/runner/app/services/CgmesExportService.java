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
import com.farao_community.farao.swe.runner.api.resource.SweFileResource;
import com.farao_community.farao.swe.runner.app.dichotomy.DichotomyDirection;
import com.farao_community.farao.swe.runner.app.domain.CgmesFileType;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyValidationData;
import com.farao_community.farao.swe.runner.app.hvdc.HvdcLinkProcessor;
import com.farao_community.farao.swe.runner.app.hvdc.parameters.HvdcCreationParameters;
import com.farao_community.farao.swe.runner.app.hvdc.parameters.SwePreprocessorParameters;
import com.farao_community.farao.swe.runner.app.hvdc.parameters.json.JsonSwePreprocessorImporter;
import com.farao_community.farao.swe.runner.app.utils.UrlValidationService;
import com.powsybl.cgmes.conversion.export.CgmesExportContext;
import com.powsybl.cgmes.conversion.export.StateVariablesExport;
import com.powsybl.cgmes.conversion.export.SteadyStateHypothesisExport;
import com.powsybl.cgmes.extensions.CgmesSvMetadata;
import com.powsybl.commons.xml.XmlUtil;
import com.powsybl.iidm.mergingview.MergingView;
import com.powsybl.iidm.network.Network;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.xml.stream.*;
import java.io.ByteArrayInputStream;
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
    private static final DateTimeFormatter CGMES_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm'Z'_'[process]_[tso]_[type]_001.xml'");

    private final FileExporter fileExporter;
    private final FileImporter fileImporter;
    private final UrlValidationService urlValidationService;
    private MergingView mergingView;

    public CgmesExportService(FileExporter fileExporter, FileImporter fileImporter, UrlValidationService urlValidationService) {
        this.fileExporter = fileExporter;
        this.fileImporter = fileImporter;
        this.urlValidationService = urlValidationService;
    }

    public String buildAndExportCgmesFiles(DichotomyDirection direction, SweData sweData, DichotomyResult<SweDichotomyValidationData> dichotomyResult) {
        if (dichotomyResult.hasValidStep()) {
            LOGGER.info("Start export of the CGMES files");
            this.mergingView = sweData.getMergingViewData().getMergingView();
            applyRemedialActions(direction, sweData, dichotomyResult);
            Network networkWithPra  = getNetworkWithPra(dichotomyResult.getHighestValidStep().getValidationData().getRaoResponse().getNetworkWithPraFileUrl());
            applyNetworkWithPraResultToMergingView(networkWithPra, mergingView);
            return exportMergingView(sweData, direction, networkWithPra);
        } else {
            LOGGER.error("Not valid step, CGMES files wont be exported");
            return null;
        }
    }


    //TODO FIXME
    // ----------------------------------------------------------
    // SV changes maybe use network with PRA instead of merging view ! start (1) OK
    // ----------------------------------------------------------
    // SSH <- merging view ? remedial action / pays + shift ! next (2),
    // my idea for SSH, use country network as context and network with PRA as input !
    // ----------------------------------------------------------
    // entry TP end EQ files could be used directly as outputs ! hold (3)
    // ----------------------------------------------------------
    void applyNetworkWithPraResultToMergingView(Network networkWithPra, MergingView mergingView) {
        LOGGER.info("Applying last shift to the network");
        mergingView.getGenerators().forEach(generator -> {
            if (networkWithPra.getGenerator(generator.getId()) != null) {
                generator.setTargetP(networkWithPra.getGenerator(generator.getId()).getTargetP());
            }
        });
        mergingView.getLoads().forEach(load -> {
            if (networkWithPra.getLoad(load.getId()) != null) {
                load.setP0(networkWithPra.getLoad(load.getId()).getP0());
            }
        });

        LOGGER.info("Applying HVDC values to AC equivalent model");
        // Applying the HVDC set point to the CGMES equivalent model if it was changed during the RAO computation
        SwePreprocessorParameters params = JsonSwePreprocessorImporter.read(getClass().getResourceAsStream("/hvdc/SwePreprocessorParameters.json"));
        Set<HvdcCreationParameters> hvdcCreationParameters = params.getHvdcCreationParametersSet();
        hvdcCreationParameters.stream().filter(param -> networkWithPra.getHvdcLine(param.getId()) != null).forEach(parameter -> HvdcLinkProcessor.connectEquivalentGeneratorsAndLoads(mergingView, parameter, networkWithPra.getHvdcLine(parameter.getId())));

    }

    private Network getNetworkWithPra(String networkWithPraUrl) {
        try (InputStream networkIs = urlValidationService.openUrlStream(networkWithPraUrl)) {
            return Network.read("networkWithPra.xiidm", networkIs);
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

    private String exportMergingView(SweData sweData, DichotomyDirection direction, Network networkWithPra) {
        try {
            Map<String, ByteArrayOutputStream> mapCgmesFiles = new HashMap<>();
            mapCgmesFiles.putAll(createAllSshFiles(sweData));
            mapCgmesFiles.putAll(createCommonFile(networkWithPra, sweData.getMergingViewData().getNetworkFr(), sweData.getMergingViewData().getNetworkEs(), sweData.getMergingViewData().getNetworkPt(), sweData));
            mapCgmesFiles.putAll(retrieveEqAndTpFiles(sweData));
            return fileExporter.exportCgmesZipFile(sweData, mapCgmesFiles, direction, buildFileType(direction));
        } catch (XMLStreamException | IOException e) {
            throw new SweInternalException("Could not export CGMES files", e);
        }
    }

    private Map<String, ByteArrayOutputStream> createAllSshFiles(SweData sweData) throws XMLStreamException, IOException {
        LOGGER.info("Building SSH files");
        Map<String, ByteArrayOutputStream> mapSshFiles = new HashMap<>();
        mapSshFiles.putAll(createOneSsh(sweData.getMergingViewData().getNetworkFr(), sweData, CgmesFileType.RTE_SSH));
        mapSshFiles.putAll(createOneSsh(sweData.getMergingViewData().getNetworkEs(), sweData, CgmesFileType.REE_SSH));
        mapSshFiles.putAll(createOneSsh(sweData.getMergingViewData().getNetworkPt(), sweData, CgmesFileType.REN_SSH));
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

    private Map<String, ByteArrayOutputStream> createOneSsh(Network network, SweData sweData, CgmesFileType cgmesFileType) throws IOException, XMLStreamException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            XMLStreamWriter writer = XmlUtil.initializeWriter(true, INDENT, os);
            CgmesExportContext cgmesExportContext = new CgmesExportContext(network);
            SteadyStateHypothesisExport.write(network, writer, cgmesExportContext);
            ByteArrayOutputStream newBaos = addInformationToXmlFile(os);
            return Map.of(buildCgmesFilename(sweData, cgmesFileType.getTso(), "SSH"), newBaos);
        }
    }

    private ByteArrayOutputStream addInformationToXmlFile(ByteArrayOutputStream os) throws XMLStreamException, IOException {
        // Read initial XML
        XMLInputFactory inputFactory = XMLInputFactory.newFactory();
        inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        inputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
        XMLStreamReader reader = inputFactory.createXMLStreamReader(new ByteArrayInputStream(os.toByteArray()));

        // Create new writer to write modified XML
        XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            XMLStreamWriter writerStream = outputFactory.createXMLStreamWriter(baos);

            // Go through initial XML and copy each data in the new XML
            while (reader.hasNext()) {
                int eventType = reader.next();
                switch (eventType) {
                    case XMLStreamConstants.START_ELEMENT:
                        // Write start element in the new XML
                        writerStream.writeStartElement(reader.getLocalName());

                        // copy element's attributes
                        for (int i = 0; i < reader.getAttributeCount(); i++) {
                            writerStream.writeAttribute(
                                    reader.getAttributeLocalName(i),
                                    reader.getAttributeValue(i)
                            );
                        }

                        // Add pTolerance
                        addPTolerance(reader, writerStream);

                        break;
                    case XMLStreamConstants.CHARACTERS:
                        // Write characters
                        writerStream.writeCharacters(reader.getText());
                        break;
                    case XMLStreamConstants.END_ELEMENT:
                        // Write end element in the new XML
                        writerStream.writeEndElement();
                        break;
                    default:
                        break;
                }
            }

            writerStream.close();
            reader.close();
            return baos;
        }
    }

    private void addPTolerance(XMLStreamReader reader, XMLStreamWriter writerStream) throws XMLStreamException {
        // I try to find what I need to modify it
        if (reader.getLocalName().equals("ControlArea")) {
            writerStream.writeStartElement("ControlArea.pTolerance");
            writerStream.writeCharacters("10");
            writerStream.writeEndElement();
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

    private Map<String, ByteArrayOutputStream> createCommonFile(Network networkWithPra, Network n1, Network n2, Network n3, SweData sweData) throws XMLStreamException, IOException {
        LOGGER.info("Building SV file");
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            XMLStreamWriter writer = XmlUtil.initializeWriter(true, INDENT, os);
            StateVariablesExport.write(networkWithPra, writer, createContext(mergingView, n1, n2, n3));
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

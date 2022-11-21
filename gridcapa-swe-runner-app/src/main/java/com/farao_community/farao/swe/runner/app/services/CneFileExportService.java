/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.cne_exporter_commons.CneExporterParameters;
import com.farao_community.farao.data.cne_exporter_commons.CneUtil;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.swe_cne_exporter.SweCneClassCreator;
import com.farao_community.farao.data.swe_cne_exporter.SweCneExporter;
import com.farao_community.farao.data.swe_cne_exporter.SweCneUtil;
import com.farao_community.farao.data.swe_cne_exporter.xsd.*;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.dichotomy.api.results.LimitingCause;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.swe.runner.api.exception.SweInvalidDataException;
import com.farao_community.farao.swe.runner.api.resource.ProcessType;
import com.farao_community.farao.swe.runner.app.configurations.ProcessConfiguration;
import com.farao_community.farao.swe.runner.app.dichotomy.DichotomyDirection;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.powsybl.commons.datasource.MemDataSource;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.time.ZoneId;
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

    public static final String FILENAME_TIMESTAMP_REGEX = "yyyyMMdd'_'HHmm'_CNE_[direction]_[secureType].zip'";
    public static final String TIME_INTERVAL_REGEX = "yyyy'-'MM'-'dd'T'HH':00:00Z'";
    public static final String RTE_SYSTEM_OPERATOR_SENDER_ID = "10XFR-RTE------Q";
    public static final String CORESO_CAPACITY_COORDINATOR_RECEIVER_ID = "22XCORESO------S";
    public static final String LAST_SECURE_STRING = "LAST_SECURE";
    public static final String FIRST_UNSECURE_STRING = "FIRST_UNSECURE";

    private final FileExporter fileExporter;
    private final MinioAdapter minioAdapter;
    private final ProcessConfiguration processConfiguration;

    public CneFileExportService(FileExporter fileExporter, MinioAdapter minioAdapter, ProcessConfiguration processConfiguration) {
        this.fileExporter = fileExporter;
        this.minioAdapter = minioAdapter;
        this.processConfiguration = processConfiguration;
    }

    public String exportCneUrl(SweData sweData, DichotomyResult<RaoResponse> dichotomyResult, boolean isHighestValid, ProcessType processType, DichotomyDirection direction) {
        OffsetDateTime timestamp = sweData.getTimestamp();
        CneExporterParameters cneExporterParameters = getCneExporterParameters(timestamp);
        CimCracCreationContext cracCreationContext = getCimCracCreationContext(sweData, direction);
        MemDataSource memDataSource = new MemDataSource();
        String targetZipFileName = generateCneZipFileName(timestamp, isHighestValid, direction);
        exportAndZipCneFile(sweData, dichotomyResult, cneExporterParameters, cracCreationContext, memDataSource, targetZipFileName, isHighestValid);
        String cneResultPath =  fileExporter.makeDestinationMinioPath(timestamp, FileExporter.FileKind.OUTPUTS) + targetZipFileName;
        uploadFileToMinio(isHighestValid, processType, direction, timestamp, memDataSource, targetZipFileName, cneResultPath);
        return minioAdapter.generatePreSignedUrl(cneResultPath);
    }

    private RaoResult extractRaoResult(DichotomyResult<RaoResponse> dichotomyResult, boolean isHighestValid) {
        RaoResult raoResult = null;
        if (isHighestValid) {
            if (dichotomyResult.hasValidStep()) {
                raoResult = dichotomyResult.getHighestValidStep().getRaoResult();
            }
        } else {
            raoResult = dichotomyResult.getLowestInvalidStep().getRaoResult();
        }
        return raoResult;
    }

    private CimCracCreationContext getCimCracCreationContext(SweData sweData, DichotomyDirection direction) {
        if (direction == DichotomyDirection.ES_PT || direction == DichotomyDirection.PT_ES) {
            return sweData.getCracEsPt();
        }
        return sweData.getCracFrEs();
    }

    private void exportAndZipCneFile(SweData sweData, DichotomyResult<RaoResponse> dichotomyResult, CneExporterParameters cneExporterParameters, CimCracCreationContext cracCreationContext, MemDataSource memDataSource, String targetZipFileName, boolean isHighestValid) {
        try (OutputStream os = memDataSource.newOutputStream(targetZipFileName, false);
             ZipOutputStream zipOs = new ZipOutputStream(os)) {
            zipOs.putNextEntry(new ZipEntry(fileExporter.zipTargetNameChangeExtension(targetZipFileName, ".xml")));
            RaoResult raoResult = extractRaoResult(dichotomyResult, isHighestValid);
            if (raoResult == null) {
                marshallMarketDocumentToXml(zipOs, createErrorMarketDocument(sweData, dichotomyResult, cneExporterParameters, cracCreationContext));
            } else {
                SweCneExporter sweCneExporter = new SweCneExporter();
                sweCneExporter.exportCne(cracCreationContext.getCrac(), sweData.getNetwork(), cracCreationContext,
                        raoResult, null, RaoParameters.load(), cneExporterParameters, zipOs);
            }
            zipOs.closeEntry();
        } catch (IOException | JAXBException | DatatypeConfigurationException | FaraoException e) {
            throw new SweInvalidDataException(String.format("Error while trying to save cne result file [%s].", targetZipFileName), e);
        }
    }

    private CriticalNetworkElementMarketDocument createErrorMarketDocument(SweData sweData, DichotomyResult<RaoResponse> dichotomyResult, CneExporterParameters cneExporterParameters, CimCracCreationContext cracCreationContext) throws DatatypeConfigurationException {
        CriticalNetworkElementMarketDocument marketDocument = createErrorMarketDocumentAndInitializeHeader(sweData, cneExporterParameters);
        OffsetDateTime offsetDateTime = cracCreationContext.getTimeStamp().withMinute(0);
        Point point = SweCneClassCreator.newPoint(1);
        SeriesPeriod period = SweCneClassCreator.newPeriod(offsetDateTime, "PT60M", point);
        Reason reason = getLimitingCauseErrorReason(dichotomyResult.getLimitingCause());
        point.getReason().add(reason);
        marketDocument.getTimeSeries().add(SweCneClassCreator.newTimeSeries("B54", "A01", period));
        return marketDocument;
    }

    private CriticalNetworkElementMarketDocument createErrorMarketDocumentAndInitializeHeader(SweData sweData, CneExporterParameters cneExporterParameters) {
        CriticalNetworkElementMarketDocument marketDocument = new CriticalNetworkElementMarketDocument();
        marketDocument.setMRID(cneExporterParameters.getDocumentId());
        marketDocument.setRevisionNumber(String.valueOf(cneExporterParameters.getRevisionNumber()));
        marketDocument.setType("B06");
        marketDocument.setProcessProcessType(cneExporterParameters.getProcessType().getCode());
        marketDocument.setSenderMarketParticipantMRID(SweCneUtil.createPartyIDString("A01", cneExporterParameters.getSenderId()));
        marketDocument.setSenderMarketParticipantMarketRoleType(cneExporterParameters.getSenderRole().getCode());
        marketDocument.setReceiverMarketParticipantMRID(SweCneUtil.createPartyIDString("A01", cneExporterParameters.getReceiverId()));
        marketDocument.setReceiverMarketParticipantMarketRoleType(cneExporterParameters.getReceiverRole().getCode());
        marketDocument.setCreatedDateTime(CneUtil.createXMLGregorianCalendarNow());
        marketDocument.setTimePeriodTimeInterval(SweCneUtil.createEsmpDateTimeIntervalForWholeDay(cneExporterParameters.getTimeInterval()));
        marketDocument.setTimePeriodTimeInterval(SweCneUtil.createEsmpDateTimeInterval(sweData.getNetwork().getCaseDate().toDate().toInstant().atOffset(ZoneOffset.UTC)));
        return marketDocument;
    }

    private static void marshallMarketDocumentToXml(OutputStream outputStream, CriticalNetworkElementMarketDocument marketDocument) throws JAXBException, IOException {
        StringWriter stringWriter = new StringWriter();
        JAXBContext jaxbContext = JAXBContext.newInstance(CriticalNetworkElementMarketDocument.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty("jaxb.formatted.output", true);
        jaxbMarshaller.setProperty("jaxb.schemaLocation", "iec62325-451-n-cne_v2_3.xsd");
        QName qName = new QName("http://www.w3.org/2001/XMLSchema-instance", "CriticalNetworkElement_MarketDocument");
        JAXBElement<CriticalNetworkElementMarketDocument> root = new JAXBElement<>(qName, CriticalNetworkElementMarketDocument.class, marketDocument);
        jaxbMarshaller.marshal(root, stringWriter);
        String result = stringWriter.toString().replace("xsi:CriticalNetworkElement_MarketDocument", "CriticalNetworkElement_MarketDocument");
        outputStream.write(result.getBytes());
    }

    private Reason getLimitingCauseErrorReason(LimitingCause limitingCause) {
        Reason reason = new Reason();
        switch (limitingCause) {
            case GLSK_LIMITATION:
                reason.setCode("B36");
                reason.setText("GLSK limitation");
                break;
            case COMPUTATION_FAILURE:
                reason.setCode("Z04");
                reason.setText("Balancing adjustment out of tolerances");
                break;
            case CRITICAL_BRANCH:
            case INDEX_EVALUATION_OR_MAX_ITERATION:
            default:
                //TODO
        }
        return reason;
    }

    private void uploadFileToMinio(boolean isHighestValid, ProcessType processType, DichotomyDirection direction, OffsetDateTime timestamp, MemDataSource memDataSource, String targetZipFileName, String cneResultPath) {
        try (InputStream is = memDataSource.newInputStream(targetZipFileName)) {
            minioAdapter.uploadOutputForTimestamp(cneResultPath, is, fileExporter.adaptTargetProcessName(processType), generateFileTypeString(isHighestValid, direction), timestamp);
        } catch (IOException e) {
            throw new SweInvalidDataException(String.format("Error while trying to upload cne file [%s].", targetZipFileName), e);
        }
    }

    private CneExporterParameters getCneExporterParameters(OffsetDateTime timestamp) {
        // limit size to 35 characters, a UUID is 36 characters long
        String mRid =  UUID.randomUUID().toString().substring(1);
        return new CneExporterParameters(
                mRid, 1, "", CneExporterParameters.ProcessType.Z01,
                RTE_SYSTEM_OPERATOR_SENDER_ID, CneExporterParameters.RoleType.SYSTEM_OPERATOR,
                CORESO_CAPACITY_COORDINATOR_RECEIVER_ID, CneExporterParameters.RoleType.CAPACITY_COORDINATOR,
                extractTimeIntervalFileHeader(timestamp));
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
        OffsetDateTime localTime = OffsetDateTime.ofInstant(timestamp.toInstant(), ZoneId.of(processConfiguration.getZoneId()));
        return df.format(localTime).replace("[direction]", direction.getDirection().replace("-", ""))
                .replace("[secureType]", isHighestValid ? LAST_SECURE_STRING : FIRST_UNSECURE_STRING);

    }

    private String generateFileTypeString(boolean isHighestValid, DichotomyDirection direction) {
        return "CNE_" + direction.getDirection().replace("-", "") + "_" + (isHighestValid ? LAST_SECURE_STRING : FIRST_UNSECURE_STRING);
    }
}

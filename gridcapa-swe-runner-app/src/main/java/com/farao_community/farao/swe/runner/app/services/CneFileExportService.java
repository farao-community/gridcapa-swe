/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.dichotomy.api.results.LimitingCause;
import com.farao_community.farao.gridcapa_swe_commons.configuration.ProcessConfiguration;
import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import com.farao_community.farao.gridcapa_swe_commons.exception.SweInvalidDataException;
import com.farao_community.farao.gridcapa_swe_commons.resource.ProcessType;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.swe.runner.app.CneProperties;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyValidationData;
import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.io.cim.craccreator.CimCracCreationContext;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.io.cne.commons.CneUtil;
import com.powsybl.openrao.data.raoresult.io.cne.swe.SweCneClassCreator;
import com.powsybl.openrao.data.raoresult.io.cne.swe.SweCneUtil;
import com.powsybl.openrao.data.raoresult.io.cne.swe.xsd.CriticalNetworkElementMarketDocument;
import com.powsybl.openrao.data.raoresult.io.cne.swe.xsd.Point;
import com.powsybl.openrao.data.raoresult.io.cne.swe.xsd.Reason;
import com.powsybl.openrao.data.raoresult.io.cne.swe.xsd.SeriesPeriod;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.springframework.stereotype.Service;

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
import java.util.Properties;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */

@Service
public class CneFileExportService {

    private static final String FILENAME_TIMESTAMP_REGEX = "yyyyMMdd'_'HHmm'_CNE_[direction]_[secureType].zip'";
    private static final String TIME_INTERVAL_REGEX = "yyyy'-'MM'-'dd'T'HH':00:00Z'";
    private static final String RTE_SYSTEM_OPERATOR_SENDER_ID = "10XFR-RTE------Q";
    private static final String CORESO_CAPACITY_COORDINATOR_RECEIVER_ID = "22XCORESO------S";
    private static final String LAST_SECURE_STRING = "LAST_SECURE";
    private static final String FIRST_UNSECURE_STRING = "FIRST_UNSECURE";

    private final FileExporter fileExporter;
    private final MinioAdapter minioAdapter;
    private final ProcessConfiguration processConfiguration;

    public CneFileExportService(FileExporter fileExporter, MinioAdapter minioAdapter, ProcessConfiguration processConfiguration) {
        this.fileExporter = fileExporter;
        this.minioAdapter = minioAdapter;
        this.processConfiguration = processConfiguration;
    }

    public String exportCneUrl(SweData sweData, DichotomyResult<SweDichotomyValidationData> dichotomyResult, boolean isHighestValid, DichotomyDirection direction) {
        //if base case unsecure, then is highest valid with limiting cause index evaluation, then we don't want an error file
        if (!dichotomyResult.hasValidStep() && isHighestValid && dichotomyResult.getLimitingCause() == LimitingCause.INDEX_EVALUATION_OR_MAX_ITERATION) {
            return null;
        }
        final OffsetDateTime timestamp = sweData.getTimestamp();
        final Properties cneExporterProperties = getCneExporterProperties(timestamp);
        final CimCracCreationContext cracCreationContext = getCimCracCreationContext(sweData, direction);
        final MemDataSource memDataSource = new MemDataSource();
        final String targetZipFileName = generateCneZipFileName(timestamp, isHighestValid, direction);
        exportAndZipCneFile(sweData, direction, dichotomyResult, cneExporterProperties, cracCreationContext, memDataSource, targetZipFileName, isHighestValid);
        final String cneResultPath = fileExporter.makeDestinationMinioPath(timestamp, FileExporter.FileKind.OUTPUTS) + targetZipFileName;
        uploadFileToMinio(isHighestValid, sweData.getProcessType(), direction, timestamp, memDataSource, targetZipFileName, cneResultPath);
        return minioAdapter.generatePreSignedUrl(cneResultPath);
    }

    private RaoResult extractRaoResult(DichotomyResult<SweDichotomyValidationData> dichotomyResult, boolean isHighestValid) {
        RaoResult raoResult = null;
        // Whether we want to extract the highest valid step or the lowest invalid step, we must ensure that such a step exists in the dichotomy result before extracting it
        // If there is no step in the dichotomy result that matches what we want, then the returned value is null
        if (isHighestValid && dichotomyResult.hasValidStep()) {
            raoResult = dichotomyResult.getHighestValidStep().getRaoResult();
        } else if (!isHighestValid && !Double.isNaN(dichotomyResult.getLowestInvalidStepValue())) {
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

    void exportAndZipCneFile(SweData sweData,
                             DichotomyDirection direction,
                             DichotomyResult<SweDichotomyValidationData> dichotomyResult,
                             Properties cneExporterProperties,
                             CimCracCreationContext cracCreationContext,
                             MemDataSource memDataSource,
                             String targetZipFileName,
                             boolean isHighestValid) {
        try (OutputStream os = memDataSource.newOutputStream(targetZipFileName, false);
            ZipOutputStream zipOs = new ZipOutputStream(os)) {
            zipOs.putNextEntry(new ZipEntry(fileExporter.zipTargetNameChangeExtension(targetZipFileName, ".xml")));
            if (!isHighestValid && dichotomyResult.isRaoFailed()) {
                // The RAO failed and we want to generate a CNE First unsecure: generate the CNE file with B18 failure message
                handleFirstUnsecureForFailedRao(sweData, direction, cneExporterProperties, cracCreationContext, zipOs);
            } else {
                handleOtherResult(sweData, direction, dichotomyResult, cneExporterProperties, cracCreationContext, isHighestValid, zipOs);
            }
            zipOs.closeEntry();
        } catch (IOException | JAXBException | DatatypeConfigurationException | OpenRaoException e) {
            throw new SweInvalidDataException(String.format("Error while trying to save cne result file [%s].", targetZipFileName), e);
        }
    }

    private void handleFirstUnsecureForFailedRao(final SweData sweData, final DichotomyDirection direction, final Properties cneExporterProperties, final CimCracCreationContext cracCreationContext, final ZipOutputStream zipOs) throws DatatypeConfigurationException, JAXBException, IOException {
        final Reason reason = getReason("B18", "RAO failure");
        fillCneWithError(sweData, direction, cneExporterProperties, cracCreationContext, zipOs, reason);
    }

    private void handleOtherResult(final SweData sweData, final DichotomyDirection direction, final DichotomyResult<SweDichotomyValidationData> dichotomyResult, final Properties cneExporterProperties, final CimCracCreationContext cracCreationContext, final boolean isHighestValid, final ZipOutputStream zipOs) throws DatatypeConfigurationException, JAXBException, IOException {
        final RaoResult raoResult = extractRaoResult(dichotomyResult, isHighestValid);
        if (raoResult == null) {
            // No RaoResult available for highest valid / lowest invalid step: generate a CNE file with limiting cause
            final Reason reason = getLimitingCauseErrorReason(dichotomyResult.getLimitingCause());
            fillCneWithError(sweData, direction, cneExporterProperties, cracCreationContext, zipOs, reason);
        } else {
            // There is a RaoResult for highest valid / lowest invalid step, its data can be exported in CNE file
            raoResult.write("SWE-CNE", cracCreationContext, cneExporterProperties, zipOs);
        }
    }

    private void fillCneWithError(final SweData sweData, final DichotomyDirection direction, final Properties cneExporterProperties, final CimCracCreationContext cracCreationContext, final ZipOutputStream zipOs, final Reason reason) throws DatatypeConfigurationException, JAXBException, IOException {
        final CriticalNetworkElementMarketDocument errorMarketDocument = createErrorMarketDocument(sweData, direction, cneExporterProperties, cracCreationContext, reason);
        marshallMarketDocumentToXml(zipOs, errorMarketDocument);
    }

    private CriticalNetworkElementMarketDocument createErrorMarketDocument(final SweData sweData, final DichotomyDirection direction, final Properties cneExporterProperties, final CimCracCreationContext cracCreationContext, final Reason reason) throws DatatypeConfigurationException {
        final CriticalNetworkElementMarketDocument marketDocument = createErrorMarketDocumentAndInitializeHeader(sweData, direction, cneExporterProperties);
        final OffsetDateTime offsetDateTime = cracCreationContext.getTimeStamp().withMinute(0);
        final Point point = SweCneClassCreator.newPoint(1);
        final SeriesPeriod period = SweCneClassCreator.newPeriod(offsetDateTime, "PT60M", point);
        point.getReason().add(reason);
        marketDocument.getTimeSeries().add(SweCneClassCreator.newTimeSeries("B54", "A01", period));
        return marketDocument;
    }

    private CriticalNetworkElementMarketDocument createErrorMarketDocumentAndInitializeHeader(SweData sweData, DichotomyDirection direction, Properties cneExporterProperties) {
        final CriticalNetworkElementMarketDocument marketDocument = new CriticalNetworkElementMarketDocument();
        marketDocument.setMRID(cneExporterProperties.getProperty(CneProperties.DOCUMENT_ID.getPrefixedKey()));
        marketDocument.setRevisionNumber(String.valueOf(cneExporterProperties.getProperty(CneProperties.REVISION_NUMBER.getPrefixedKey())));
        marketDocument.setType("B06");
        marketDocument.setProcessProcessType(cneExporterProperties.getProperty(CneProperties.PROCESS_TYPE.getPrefixedKey()));
        marketDocument.setSenderMarketParticipantMRID(SweCneUtil.createPartyIDString("A01", cneExporterProperties.getProperty(CneProperties.SENDER_ID.getPrefixedKey())));
        marketDocument.setSenderMarketParticipantMarketRoleType(cneExporterProperties.getProperty(CneProperties.SENDER_ROLE.getPrefixedKey()));
        marketDocument.setReceiverMarketParticipantMRID(SweCneUtil.createPartyIDString("A01", cneExporterProperties.getProperty(CneProperties.RECEIVER_ID.getPrefixedKey())));
        marketDocument.setReceiverMarketParticipantMarketRoleType(cneExporterProperties.getProperty(CneProperties.RECEIVER_ROLE.getPrefixedKey()));
        marketDocument.setCreatedDateTime(CneUtil.createXMLGregorianCalendarNow());
        marketDocument.setTimePeriodTimeInterval(SweCneUtil.createEsmpDateTimeIntervalForWholeDay(cneExporterProperties.getProperty(CneProperties.TIME_INTERVAL.getPrefixedKey())));
        marketDocument.setTimePeriodTimeInterval(SweCneUtil.createEsmpDateTimeInterval(NetworkService.getNetworkByDirection(sweData, direction).getCaseDate().toInstant().atOffset(ZoneOffset.UTC)));
        return marketDocument;
    }

    private static void marshallMarketDocumentToXml(OutputStream outputStream, CriticalNetworkElementMarketDocument marketDocument) throws JAXBException, IOException {
        final StringWriter stringWriter = new StringWriter();
        final JAXBContext jaxbContext = JAXBContext.newInstance(CriticalNetworkElementMarketDocument.class);
        final Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty("jaxb.formatted.output", true);
        jaxbMarshaller.setProperty("jaxb.schemaLocation", "iec62325-451-n-cne_v2_3.xsd");
        final QName qName = new QName("http://www.w3.org/2001/XMLSchema-instance", "CriticalNetworkElement_MarketDocument");
        final JAXBElement<CriticalNetworkElementMarketDocument> root = new JAXBElement<>(qName, CriticalNetworkElementMarketDocument.class, marketDocument);
        jaxbMarshaller.marshal(root, stringWriter);
        final String result = stringWriter.toString().replace("xsi:CriticalNetworkElement_MarketDocument", "CriticalNetworkElement_MarketDocument");
        outputStream.write(result.getBytes());
    }

    static Reason getLimitingCauseErrorReason(final LimitingCause limitingCause) {
        return switch (limitingCause) {
            case GLSK_LIMITATION ->
                    getReason("B36", "GLSK limitation");
            case BALANCE_LOADFLOW_DIVERGENCE ->
                    getReason("B40", "Balance Load Flow divergence");
            case UNKNOWN_TERMINAL_BUS ->
                    getReason("B32", "Unknown terminal bus for balancing");
            case COMPUTATION_FAILURE ->
                    getReason("B18", "Balancing adjustment out of tolerances");
            default -> new Reason();
        };
    }

    private static Reason getReason(final String code, final String text) {
        final Reason reason = new Reason();
        reason.setCode(code);
        reason.setText(text);
        return reason;
    }

    private void uploadFileToMinio(boolean isHighestValid, ProcessType processType, DichotomyDirection direction, OffsetDateTime timestamp, MemDataSource memDataSource, String targetZipFileName, String cneResultPath) {
        try (InputStream is = memDataSource.newInputStream(targetZipFileName)) {
            minioAdapter.uploadOutputForTimestamp(cneResultPath, is, fileExporter.adaptTargetProcessName(processType), generateFileTypeString(isHighestValid, direction), timestamp);
        } catch (IOException e) {
            throw new SweInvalidDataException(String.format("Error while trying to upload cne file [%s].", targetZipFileName), e);
        }
    }

    Properties getCneExporterProperties(OffsetDateTime timestamp) {
        // limit size to 35 characters, a UUID is 36 characters long
        final String mRid = UUID.randomUUID().toString().substring(1);
        final Properties properties = new Properties();
        properties.setProperty(CneProperties.DOCUMENT_ID.getPrefixedKey(), mRid);
        properties.setProperty(CneProperties.REVISION_NUMBER.getPrefixedKey(), "1");
        properties.setProperty(CneProperties.DOMAIN_ID.getPrefixedKey(), "");
        properties.setProperty(CneProperties.PROCESS_TYPE.getPrefixedKey(), "Z01");
        properties.setProperty(CneProperties.SENDER_ID.getPrefixedKey(), RTE_SYSTEM_OPERATOR_SENDER_ID);
        properties.setProperty(CneProperties.SENDER_ROLE.getPrefixedKey(), "A04");
        properties.setProperty(CneProperties.RECEIVER_ID.getPrefixedKey(), CORESO_CAPACITY_COORDINATOR_RECEIVER_ID);
        properties.setProperty(CneProperties.RECEIVER_ROLE.getPrefixedKey(), "A36");
        properties.setProperty(CneProperties.TIME_INTERVAL.getPrefixedKey(), extractTimeIntervalFileHeader(timestamp));
        return properties;
    }

    private String extractTimeIntervalFileHeader(OffsetDateTime timestamp) {
        final OffsetDateTime timestampUtc = OffsetDateTime.of(timestamp.toLocalDateTime(), ZoneOffset.UTC);
        final OffsetDateTime timestampUtcPlusOneHour = OffsetDateTime.of(timestamp.toLocalDateTime(), ZoneOffset.UTC).plusHours(1);
        final DateTimeFormatter df = DateTimeFormatter.ofPattern(TIME_INTERVAL_REGEX);
        return df.format(timestampUtc) + "/" + df.format(timestampUtcPlusOneHour);
    }

    private String generateCneZipFileName(OffsetDateTime timestamp, boolean isHighestValid, DichotomyDirection direction) {
        final DateTimeFormatter df = DateTimeFormatter.ofPattern(FILENAME_TIMESTAMP_REGEX);
        final OffsetDateTime localTime = OffsetDateTime.ofInstant(timestamp.toInstant(), ZoneId.of(processConfiguration.getZoneId()));
        return df.format(localTime).replace("[direction]", direction.getDashName().replace("-", ""))
                .replace("[secureType]", isHighestValid ? LAST_SECURE_STRING : FIRST_UNSECURE_STRING);

    }

    private String generateFileTypeString(boolean isHighestValid, DichotomyDirection direction) {
        return "CNE_" + direction.getDashName().replace("-", "") + "_" + (isHighestValid ? LAST_SECURE_STRING : FIRST_UNSECURE_STRING);
    }
}

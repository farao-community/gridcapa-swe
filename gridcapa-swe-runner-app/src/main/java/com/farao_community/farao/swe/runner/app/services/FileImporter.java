/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.gridcapa_swe_commons.exception.SweInvalidDataException;
import com.farao_community.farao.swe.runner.api.resource.SweFileResource;
import com.farao_community.farao.swe.runner.app.domain.SweTaskParameters;
import com.farao_community.farao.swe.runner.app.utils.UrlValidationService;
import com.powsybl.glsk.api.io.GlskDocumentImporters;
import com.powsybl.glsk.cim.CimGlskDocument;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.RaUsageLimits;
import com.powsybl.openrao.data.cracapi.parameters.CracCreationParameters;
import com.powsybl.openrao.data.cracapi.parameters.JsonCracCreationParameters;
import com.powsybl.openrao.data.craccreation.creator.cim.craccreator.CimCracCreationContext;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.data.raoresultjson.RaoResultJsonImporter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.time.OffsetDateTime;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Service
public class FileImporter {

    private static final Object LOCK_GLSK = new Object();
    private static final Logger LOGGER = LoggerFactory.getLogger(FileImporter.class);
    private final UrlValidationService urlValidationService;

    public FileImporter(UrlValidationService urlValidationService) {
        this.urlValidationService = urlValidationService;
    }

    public CimCracCreationContext importCracFromCimCracAndNetwork(SweFileResource cracFile, OffsetDateTime processDateTime, Network network, String cracCreationParams, SweTaskParameters sweTaskParameters) {
        CracCreationParameters cimCracCreationParameters = getCimCracCreationParameters(cracCreationParams);
        RaUsageLimits raUsageLimits = cimCracCreationParameters.getRaUsageLimitsPerInstant().get("curative");
        if (raUsageLimits == null) {
            raUsageLimits = new RaUsageLimits();
            cimCracCreationParameters.addRaUsageLimitsForInstant("curative", raUsageLimits);
        }
        raUsageLimits.setMaxRa(sweTaskParameters.getMaxCra());

        return importCrac(
                cracFile,
                processDateTime,
                network,
                cimCracCreationParameters);
    }

    public Crac importCracFromJson(String cracUrl, Network network) {
        LOGGER.info("Importing Crac from json file url");
        try (InputStream cracResultStream = urlValidationService.openUrlStream(cracUrl)) {
            return Crac.read(FilenameUtils.getName(new URL(cracUrl).getPath()), cracResultStream, network);
        } catch (IOException e) {
            throw new SweInvalidDataException(String.format("Cannot import crac from JSON : %s", cracUrl), e);
        }
    }

    private CimCracCreationContext importCrac(SweFileResource crac, OffsetDateTime targetProcessDateTime, Network network, CracCreationParameters params) {
        LOGGER.info("Importing native Crac from Cim Crac and Network for process date: {}", targetProcessDateTime);
        try {
            return (CimCracCreationContext) Crac.readWithContext(crac.getFilename(), urlValidationService.openUrlStream(crac.getUrl()), network, targetProcessDateTime, params);
        } catch (IOException e) {
            throw new SweInvalidDataException(String.format("Cannot read crac with context for process date: %s", targetProcessDateTime), e);
        }
    }

    private CracCreationParameters getCimCracCreationParameters(String paramFilePath) {
        LOGGER.info("Importing Crac Creation Parameters file: {}", paramFilePath);
        if (StringUtils.isAllBlank(paramFilePath)) {
            return new CracCreationParameters();
        }
        return JsonCracCreationParameters.read(getClass().getResourceAsStream(paramFilePath));
    }

    public ZonalData<Scalable> importGlsk(String glskUrl, Network network, Instant instant) {
        try (InputStream glskResultStream = urlValidationService.openUrlStream(glskUrl)) {
            synchronized (LOCK_GLSK) {
                LOGGER.info("Importing Glsk file : {}", glskUrl);
                return GlskDocumentImporters.importGlsk(glskResultStream).getZonalScalable(network, instant);
            }
        } catch (IOException e) {
            throw new SweInvalidDataException("Cannot import glsk from url", e);
        }
    }

    public CimGlskDocument importCimGlskDocument(String glskUrl) {
        try (InputStream glskResultStream = urlValidationService.openUrlStream(glskUrl)) {
            synchronized (LOCK_GLSK) {
                LOGGER.info("Importing Glsk file : {}", glskUrl);
                return CimGlskDocument.importGlsk(glskResultStream);
            }
        } catch (IOException e) {
            throw new SweInvalidDataException("Cannot import glsk from url", e);
        }
    }

    public RaoResult importRaoResult(String raoResultUrl, Crac crac) {
        try (InputStream raoResultStream = urlValidationService.openUrlStream(raoResultUrl)) {
            RaoResult raoResult =  new RaoResultJsonImporter().importData(raoResultStream, crac);
            return  raoResult;
        } catch (IOException e) {
            throw new SweInvalidDataException("Cannot import rao result from url", e);
        }
    }

    public InputStream importCgmesFiles(String url) {
        try {
            return urlValidationService.openUrlStream(url);
        } catch (Exception e) {
            throw new SweInvalidDataException("Cannot import cgmes file from url", e);
        }
    }
}

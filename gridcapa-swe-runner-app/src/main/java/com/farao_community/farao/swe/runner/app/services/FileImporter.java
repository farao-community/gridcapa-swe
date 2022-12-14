/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.api.CracCreators;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.JsonCracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.cim.CimCrac;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.importer.CimCracImporter;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_json.RaoResultImporter;
import com.farao_community.farao.swe.runner.api.exception.SweInvalidDataException;
import com.farao_community.farao.swe.runner.api.resource.SweRequest;
import com.farao_community.farao.swe.runner.app.utils.UrlValidationService;
import com.powsybl.glsk.api.io.GlskDocumentImporters;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Network;
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

    public CimCrac importCimCrac(SweRequest sweRequest) {
        try (InputStream cracInputStream = urlValidationService.openUrlStream(sweRequest.getCrac().getUrl())) {
            LOGGER.info("Importing Cim Crac file from url");
            CimCracImporter cimCracImporter = new CimCracImporter();
            return cimCracImporter.importNativeCrac(cracInputStream);
        } catch (IOException e) {
            throw new SweInvalidDataException("Cannot import crac from url", e);
        }
    }

    public CimCracCreationContext importCracFromCimCracAndNetwork(CimCrac cimCrac, OffsetDateTime processDateTime, Network network, String cracCreationParams) {
        return importCrac(
                cimCrac,
                processDateTime,
                network,
                getCimCracCreationParameters(cracCreationParams));
    }

    public Crac importCracFromJson(String cracUrl) {
        LOGGER.info("Importing Crac from json file url");
        try (InputStream cracResultStream = urlValidationService.openUrlStream(cracUrl)) {
            return CracImporters.importCrac(FilenameUtils.getName(new URL(cracUrl).getPath()), cracResultStream);
        } catch (IOException e) {
            throw new SweInvalidDataException(String.format("Cannot import crac from JSON : %s", cracUrl));
        }
    }

    private CimCracCreationContext importCrac(CimCrac cimCrac, OffsetDateTime targetProcessDateTime, Network network, CracCreationParameters params) {
        LOGGER.info("Importing native Crac from Cim Crac and Network for process date: {}", targetProcessDateTime);
        return (CimCracCreationContext) CracCreators.createCrac(cimCrac, network, targetProcessDateTime, params);
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

    public RaoResult importRaoResult(String raoResultUrl, Crac crac) {
        try (InputStream raoResultStream = urlValidationService.openUrlStream(raoResultUrl)) {
            return new RaoResultImporter().importRaoResult(raoResultStream, crac);
        } catch (IOException e) {
            throw new SweInvalidDataException("Cannot import rao result from url", e);
        }
    }
}

/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.swe.runner.api.resource.SweRequest;
import com.farao_community.farao.swe.runner.api.resource.SweResponse;
import com.farao_community.farao.swe.runner.app.configurations.UrlConfiguration;
import com.farao_community.farao.swe.runner.app.dichotomy.DichotomyParallelization;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweTaskParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Service
public class SweRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(SweRunner.class);
    private final DichotomyParallelization dichotomyParallelization;
    private final FilesService filesService;
    private final Logger businessLogger;
    private final RestTemplateBuilder restTemplateBuilder;
    private final UrlConfiguration urlConfiguration;

    public SweRunner(DichotomyParallelization dichotomyParallelization, FilesService filesService, Logger businessLogger, RestTemplateBuilder restTemplateBuilder, UrlConfiguration urlConfiguration) {
        this.dichotomyParallelization = dichotomyParallelization;
        this.filesService = filesService;
        this.businessLogger = businessLogger;
        this.restTemplateBuilder = restTemplateBuilder;
        this.urlConfiguration = urlConfiguration;
    }

    public SweResponse run(final SweRequest sweRequest,
                           final OffsetDateTime startTime) {
        LOGGER.info("Request received for timestamp {}", sweRequest.getTargetProcessDateTime());
        if (checkIsInterrupted(sweRequest)) {
            businessLogger.warn("Computation has been interrupted for timestamp {}", sweRequest.getTargetProcessDateTime());
            LOGGER.info("Response sent for timestamp {} : run has been interrupted", sweRequest.getTargetProcessDateTime());
            return new SweResponse(sweRequest.getId(), null, true, false, false);
        }
        SweTaskParameters sweTaskParameters = new SweTaskParameters(sweRequest.getTaskParameterList());
        logSweParameters(sweRequest, sweTaskParameters);
        SweData sweData = filesService.importFiles(sweRequest, sweTaskParameters);
        SweResponse sweResponse = dichotomyParallelization.launchDichotomy(sweData, sweTaskParameters, startTime);
        LOGGER.info("Response sent for timestamp {}", sweRequest.getTargetProcessDateTime());
        return sweResponse;
    }

    private void logSweParameters(SweRequest sweRequest, SweTaskParameters sweTaskParameters) {
        final String sweParametersString = sweTaskParameters.toJsonString();
        if (sweRequest.getTaskParameterList().stream().anyMatch(p -> !Objects.equals(p.getValue(), p.getDefaultValue()))) {
            businessLogger.warn("SWE task parameters: {}", sweParametersString);
        } else {
            businessLogger.info("SWE task parameters: {}", sweParametersString);
        }
    }

    private boolean checkIsInterrupted(SweRequest sweRequest) {
        ResponseEntity<Boolean> responseEntity = restTemplateBuilder.build().getForEntity(getInterruptedUrl(sweRequest.getCurrentRunId()), Boolean.class);
        return responseEntity.getBody() != null && responseEntity.getStatusCode() == HttpStatus.OK && responseEntity.getBody();
    }

    private String getInterruptedUrl(String runId) {
        return urlConfiguration.getInterruptServerUrl() + runId;
    }
}

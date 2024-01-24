/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.swe.runner.api.resource.SweRequest;
import com.farao_community.farao.swe.runner.api.resource.SweResponse;
import com.farao_community.farao.swe.runner.app.dichotomy.DichotomyParallelization;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweTaskParameters;
import com.farao_community.farao.swe.runner.app.utils.Threadable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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

    public SweRunner(DichotomyParallelization dichotomyParallelization, FilesService filesService, Logger businessLogger) {
        this.dichotomyParallelization = dichotomyParallelization;
        this.filesService = filesService;
        this.businessLogger = businessLogger;
    }

    @Threadable
    public SweResponse run(SweRequest sweRequest) {
        LOGGER.info("Request received for timestamp {}", sweRequest.getTargetProcessDateTime());
        SweTaskParameters sweTaskParameters = new SweTaskParameters(sweRequest.getTaskParameterList());
        logSweParameters(sweRequest, sweTaskParameters);
        SweData sweData = filesService.importFiles(sweRequest, sweTaskParameters);
        SweResponse sweResponse = dichotomyParallelization.launchDichotomy(sweData, sweTaskParameters);
        LOGGER.info("Response sent for timestamp {}", sweRequest.getTargetProcessDateTime());
        return sweResponse;
    }

    private void logSweParameters(SweRequest sweRequest, SweTaskParameters sweTaskParameters) {
        if (sweRequest.getTaskParameterList().stream().anyMatch(p -> !Objects.equals(p.getValue(), p.getDefaultValue()))) {
            businessLogger.warn("SWE task parameters: {}", sweTaskParameters.toJsonString());
        } else {
            businessLogger.info("SWE task parameters: {}", sweTaskParameters.toJsonString());
        }
    }
}

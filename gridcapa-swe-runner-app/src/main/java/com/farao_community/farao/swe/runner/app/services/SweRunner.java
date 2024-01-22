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

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Service
public class SweRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(SweRunner.class);
    private final DichotomyParallelization dichotomyParallelization;
    private final FilesService filesService;

    public SweRunner(DichotomyParallelization dichotomyParallelization, FilesService filesService) {
        this.dichotomyParallelization = dichotomyParallelization;
        this.filesService = filesService;
    }

    @Threadable
    public SweResponse run(SweRequest sweRequest) {
        LOGGER.info("Request received for timestamp {}", sweRequest.getTargetProcessDateTime());
        SweTaskParameters sweTaskParameters = new SweTaskParameters(sweRequest.getTaskParameterList());
        SweData sweData = filesService.importFiles(sweRequest, sweTaskParameters);
        SweResponse sweResponse = dichotomyParallelization.launchDichotomy(sweData, sweTaskParameters);
        LOGGER.info("Response sent for timestamp {}", sweRequest.getTargetProcessDateTime());
        return sweResponse;
    }

}

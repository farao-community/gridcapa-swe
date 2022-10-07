/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.swe.runner.api.resource.SweRequest;
import com.farao_community.farao.swe.runner.api.resource.SweResponse;
import com.farao_community.farao.swe.runner.app.utils.ImportedFiles;
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

    private final FilesService filesService;
    private ImportedFiles importedFiles;

    public SweRunner(FilesService filesService) {
        this.filesService = filesService;
    }

    @Threadable
    public SweResponse run(SweRequest sweRequest) {
        LOGGER.info("Request received for timestamp {}", sweRequest.getTargetProcessDateTime());
        importedFiles = filesService.importFiles(sweRequest);
        LOGGER.info("Response sent for timestamp {}", sweRequest.getTargetProcessDateTime());
        return new SweResponse(sweRequest.getId());
    }

}

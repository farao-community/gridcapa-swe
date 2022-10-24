/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.parallelization.ExecutionResult;
import com.farao_community.farao.swe.runner.app.ttc_doc.TtcDocument;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Service
public class OutputService {

    private final FileExporter fileExporter;

    public OutputService(FileExporter fileExporter) {
        this.fileExporter = fileExporter;
    }

    public String buildAndExportTtcDocument(SweData sweData, ExecutionResult result) {
        TtcDocument ttcDoc = new TtcDocument(result);
        InputStream inputStream = ttcDoc.buildTtcDocFile();
        return fileExporter.exportTtcDocument(sweData, inputStream);
    }
}

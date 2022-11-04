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
import java.time.format.DateTimeFormatter;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Service
public class OutputService {

    public static final String TTC_DOC_NAME_REGEX = "'SWE_'yyyyMMdd'_'HHmm'_TTCdoc.xml'";
    private final FileExporter fileExporter;

    public OutputService(FileExporter fileExporter) {
        this.fileExporter = fileExporter;
    }

    public String buildAndExportTtcDocument(SweData sweData, ExecutionResult result) {
        TtcDocument ttcDoc = new TtcDocument(result);
        InputStream inputStream = ttcDoc.buildTtcDocFile();
        return fileExporter.exportTtcDocument(sweData, inputStream, buildTtcDocName(sweData));
    }

    private String buildTtcDocName(SweData sweData) {
        DateTimeFormatter df = DateTimeFormatter.ofPattern(TTC_DOC_NAME_REGEX);
        return df.format(sweData.getTimestamp());
    }
}

/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.swe.runner.api.exception.SweInvalidDataException;
import com.farao_community.farao.swe.runner.api.resource.ProcessType;
import com.farao_community.farao.swe.runner.app.dichotomy.DichotomyDirection;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyResult;
import com.farao_community.farao.swe.runner.app.parallelization.ExecutionResult;
import com.farao_community.farao.swe.runner.app.ttc_doc.TtcDocument;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Service
public class OutputService {

    private final FileExporter fileExporter;

    public OutputService(FileExporter fileExporter) {
        this.fileExporter = fileExporter;
    }

    public String buildAndExportTtcDocument(SweData sweData, ExecutionResult<SweDichotomyResult> result) {
        TtcDocument ttcDoc = new TtcDocument(result);
        InputStream inputStream = ttcDoc.buildTtcDocFile();
        return fileExporter.exportTtcDocument(sweData, inputStream);
    }

    public String buildAndExportEsFrVoltageDoc(SweData sweData, ExecutionResult<SweDichotomyResult> result) {
        return buildAndExportVoltageDoc(DichotomyDirection.ES_FR, sweData, result);
    }

    public String buildAndExportFrEsVoltageDoc(SweData sweData, ExecutionResult<SweDichotomyResult> result) {
        return buildAndExportVoltageDoc(DichotomyDirection.FR_ES, sweData, result);
    }

    private String buildAndExportVoltageDoc(DichotomyDirection direction, SweData sweData, ExecutionResult<SweDichotomyResult> result) {
        OffsetDateTime timestamp = sweData.getTimestamp();
        String zipName = timestamp.getYear()
                + String.format("%02d", timestamp.getMonthValue())
                + String.format("%02d", timestamp.getDayOfMonth()) + "_"
                + String.format("%02d", timestamp.getHour()) + "30_Voltage_"
                + (direction == DichotomyDirection.FR_ES ? "FRES" : "ESFR")
                + ".zip";
        Optional<SweDichotomyResult> esFrResult = result.getResult().stream().filter(res -> {
            return res.getVoltageMonitoringResult().isPresent()
                && res.getDichotomyDirection() == direction;
        }).findFirst();
        if (esFrResult.isPresent()) {
            return fileExporter.saveVoltageMonitoringResultInJsonZip(esFrResult.get().getVoltageMonitoringResult().get(), zipName, timestamp, ProcessType.D2CC);
        }
        throw new SweInvalidDataException("No voltage monitoring result data for file: " + zipName);
    }
}

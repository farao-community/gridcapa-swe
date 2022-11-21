/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.monitoring.voltage_monitoring.VoltageMonitoringResult;
import com.farao_community.farao.swe.runner.api.exception.SweInvalidDataException;
import com.farao_community.farao.swe.runner.api.resource.ProcessType;
import com.farao_community.farao.swe.runner.app.dichotomy.DichotomyDirection;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyResult;
import com.farao_community.farao.swe.runner.app.parallelization.ExecutionResult;
import com.farao_community.farao.swe.runner.app.ttc_doc.TtcDocument;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.OffsetDateTime;
import java.util.Optional;

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

    public String buildAndExportTtcDocument(SweData sweData, ExecutionResult<SweDichotomyResult> result) {
        TtcDocument ttcDoc = new TtcDocument(result);
        InputStream inputStream = ttcDoc.buildTtcDocFile();
        return fileExporter.exportTtcDocument(sweData, inputStream, buildTtcDocName(sweData));
    }

    private String buildTtcDocName(SweData sweData) {
        DateTimeFormatter df = DateTimeFormatter.ofPattern(TTC_DOC_NAME_REGEX);
        OffsetDateTime localTime = OffsetDateTime.ofInstant(sweData.getTimestamp().toInstant(), ZoneId.of(FileExporter.ZONE_ID));
        return df.format(localTime);
    }

    public String buildAndExportVoltageDoc(DichotomyDirection direction, SweData sweData, ExecutionResult<SweDichotomyResult> result) {
        OffsetDateTime timestamp = sweData.getTimestamp();
        String directionString = direction == DichotomyDirection.FR_ES ? "FRES" : "ESFR";
        OffsetDateTime localTime = OffsetDateTime.ofInstant(timestamp.toInstant(), ZoneId.of(FileExporter.ZONE_ID));
        String zipName = localTime.getYear()
                + String.format("%02d", localTime.getMonthValue())
                + String.format("%02d", localTime.getDayOfMonth()) + "_"
                + String.format("%02d", localTime.getHour()) + "30_Voltage_"
                + directionString
                + ".zip";
        Optional<SweDichotomyResult> directionResult = result.getResult().stream().filter(res -> res.getDichotomyDirection() == direction).findFirst();
        if (directionResult.isPresent()) {
            SweDichotomyResult sweResult = directionResult.get();
            Optional<VoltageMonitoringResult> voltageResult = sweResult.getVoltageMonitoringResult();
            if (voltageResult.isPresent()) {
                VoltageMonitoringResult voltageRes = voltageResult.get();
                return fileExporter.saveVoltageMonitoringResultInJsonZip(voltageRes, zipName, timestamp, ProcessType.D2CC, "VOLTAGE_" + directionString);
            }
        }
        throw new SweInvalidDataException("No voltage monitoring result data for file: " + zipName);
    }
}

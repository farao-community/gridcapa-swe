/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.monitoring.voltage_monitoring.VoltageMonitoringResult;
import com.farao_community.farao.swe.runner.app.configurations.ProcessConfiguration;
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
    public static final String VOLTAGE_DOC_NAME_REGEX = "yyyyMMdd'_'HH'30_Voltage_[direction].zip'";
    private final FileExporter fileExporter;
    private final ProcessConfiguration processConfiguration;

    public OutputService(FileExporter fileExporter, ProcessConfiguration processConfiguration) {
        this.fileExporter = fileExporter;
        this.processConfiguration = processConfiguration;
    }

    public String buildAndExportTtcDocument(SweData sweData, ExecutionResult<SweDichotomyResult> result) {
        TtcDocument ttcDoc = new TtcDocument(result);
        InputStream inputStream = ttcDoc.buildTtcDocFile();
        return fileExporter.exportTtcDocument(sweData, inputStream, buildTtcDocName(sweData));
    }

    private String buildTtcDocName(SweData sweData) {
        DateTimeFormatter df = DateTimeFormatter.ofPattern(TTC_DOC_NAME_REGEX);
        OffsetDateTime localTime = OffsetDateTime.ofInstant(sweData.getTimestamp().toInstant(), ZoneId.of(processConfiguration.getZoneId()));
        return df.format(localTime);
    }

    public void buildAndExportVoltageDoc(DichotomyDirection direction, SweData sweData, Optional<VoltageMonitoringResult> voltageMonitoringResult) {
        if (voltageMonitoringResult.isPresent()) {
            OffsetDateTime timestamp = sweData.getTimestamp();
            String directionString = direction == DichotomyDirection.FR_ES ? "FRES" : "ESFR";
            OffsetDateTime localTime = OffsetDateTime.ofInstant(timestamp.toInstant(), ZoneId.of(processConfiguration.getZoneId()));
            DateTimeFormatter df = DateTimeFormatter.ofPattern(VOLTAGE_DOC_NAME_REGEX);
            String zipName = df.format(localTime).replace("[direction]", directionString);
            VoltageMonitoringResult voltageRes = voltageMonitoringResult.get();
            fileExporter.saveVoltageMonitoringResultInJsonZip(voltageRes, zipName, timestamp, sweData.getProcessType(), "VOLTAGE_" + directionString);
        }
    }
}

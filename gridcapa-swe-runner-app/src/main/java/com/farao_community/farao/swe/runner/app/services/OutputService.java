/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.gridcapa_swe_commons.configuration.ProcessConfiguration;
import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyResult;
import com.farao_community.farao.swe.runner.app.domain.SweTaskParameters;
import com.farao_community.farao.swe.runner.app.parallelization.ExecutionResult;
import com.farao_community.farao.swe.runner.app.ttc_doc.TtcDocument;
import com.powsybl.openrao.monitoring.results.RaoResultWithVoltageMonitoring;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Service
@Import(ProcessConfiguration.class)
public class OutputService {

    public static final String TTC_DOC_NAME_REGEX = "'SWE_'yyyyMMdd'_'HHmm'_TTCdoc.xml'";
    public static final String VOLTAGE_DOC_NAME_REGEX = "yyyyMMdd'_'HH'30_Voltage_[direction].zip'";
    private final FileExporter fileExporter;
    private final ProcessConfiguration processConfiguration;

    public OutputService(final FileExporter fileExporter,
                         final ProcessConfiguration processConfiguration) {
        this.fileExporter = fileExporter;
        this.processConfiguration = processConfiguration;
    }

    public String buildAndExportTtcDocument(final SweData sweData,
                                            final ExecutionResult<SweDichotomyResult> result) {
        final TtcDocument ttcDoc = new TtcDocument(result);
        final InputStream inputStream = ttcDoc.buildTtcDocFile();
        return fileExporter.exportTtcDocument(sweData, inputStream, buildTtcDocName(sweData));
    }

    private String buildTtcDocName(final SweData sweData) {
        final DateTimeFormatter df = DateTimeFormatter.ofPattern(TTC_DOC_NAME_REGEX);
        final OffsetDateTime localTime = OffsetDateTime.ofInstant(sweData.getTimestamp().toInstant(), ZoneId.of(processConfiguration.getZoneId()));
        return df.format(localTime);
    }

    public void buildAndExportVoltageDoc(final DichotomyDirection direction,
                                         final SweData sweData,
                                         final Optional<RaoResultWithVoltageMonitoring> voltageMonitoringResult,
                                         final SweTaskParameters sweTaskParameters) {
        if (sweTaskParameters.isRunVoltageCheck() && (direction.equals(DichotomyDirection.ES_FR) || direction.equals(DichotomyDirection.FR_ES))) {
            final OffsetDateTime timestamp = sweData.getTimestamp();
            final String directionString = direction == DichotomyDirection.FR_ES ? "FRES" : "ESFR";
            final OffsetDateTime localTime = OffsetDateTime.ofInstant(timestamp.toInstant(), ZoneId.of(processConfiguration.getZoneId()));
            final DateTimeFormatter df = DateTimeFormatter.ofPattern(VOLTAGE_DOC_NAME_REGEX);
            final String zipName = df.format(localTime).replace("[direction]", directionString);
            final RaoResultWithVoltageMonitoring voltageRes = voltageMonitoringResult.orElse(null);
            fileExporter.saveVoltageMonitoringResultInJsonZip(voltageRes, zipName, timestamp, sweData.getProcessType(), "VOLTAGE_" + directionString, sweData.getCracFrEs().getCrac());
        }
    }
}

/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import com.farao_community.farao.gridcapa_swe_commons.exception.SweInternalException;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyValidationData;
import com.farao_community.farao.swe.runner.app.domain.SweTaskParameters;
import com.farao_community.farao.swe.runner.app.utils.OpenLoadFlowParametersUtil;
import com.farao_community.farao.swe.runner.app.utils.UrlValidationService;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.commons.MinOrMax;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.monitoring.Monitoring;
import com.powsybl.openrao.monitoring.MonitoringInput;
import com.powsybl.openrao.monitoring.results.RaoResultWithVoltageMonitoring;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */

@Service
public class VoltageCheckService {
    private final Logger businessLogger;
    private final UrlValidationService urlValidationService;

    public VoltageCheckService(final Logger businessLogger,
                               final UrlValidationService urlValidationService) {
        this.businessLogger = businessLogger;
        this.urlValidationService = urlValidationService;
    }

    public Optional<RaoResultWithVoltageMonitoring> runVoltageCheck(final SweData sweData,
                                                                    final DichotomyResult<SweDichotomyValidationData> dichotomyResult,
                                                                    final SweTaskParameters sweTaskParameters,
                                                                    final DichotomyDirection direction) {

        if (!sweTaskParameters.isRunVoltageCheck()) {
            businessLogger.info("Voltage check disabled in configuration and will not be run.");
            return Optional.empty();
        }
        if (direction != DichotomyDirection.ES_FR && direction != DichotomyDirection.FR_ES) {
            return Optional.empty();
        }
        if (!dichotomyResult.hasValidStep()) {
            businessLogger.warn("Voltage check is in failure because no valid step is available to run it.");
            return Optional.empty();
        }
        businessLogger.info("Running voltage check");
        try {
            final Crac crac = sweData.getCracFrEs().getCrac();
            final Network network = getNetworkWithPra(dichotomyResult);
            final LoadFlowParameters loadFlowParameters = OpenLoadFlowParametersUtil.getLoadFlowParameters(sweTaskParameters);
            final MonitoringInput input = MonitoringInput.buildWithVoltage(network, crac, dichotomyResult.getHighestValidStep().getRaoResult()).build();
            final RaoResultWithVoltageMonitoring result = (RaoResultWithVoltageMonitoring) Monitoring.runVoltageAndUpdateRaoResult(LoadFlow.find().getName(), loadFlowParameters, 4, input);
            generateHighAndLowVoltageConstraints(result, crac).forEach(businessLogger::warn);
            return Optional.of(result);
        } catch (final Exception e) {
            businessLogger.error("Exception during voltage check : {}", e.getMessage());
            return Optional.empty();
        }
    }

    protected List<String> generateHighAndLowVoltageConstraints(final RaoResultWithVoltageMonitoring result,
                                                                final Crac crac) {
        final List<String> voltageConstraints = new ArrayList<>();
        for (final VoltageCnec voltageCnec : crac.getVoltageCnecs()) {
            final Instant instant = voltageCnec.getState().getInstant();
            if (instant.isCurative()) {
                voltageCnec.getLowerBound(Unit.KILOVOLT).ifPresent(lowerBound -> {
                    final double minVoltage = result.getMinVoltage(instant, voltageCnec, MinOrMax.MIN, Unit.KILOVOLT);
                    if (!Double.isNaN(minVoltage) && Double.compare(lowerBound, minVoltage) > 0) {
                        voltageConstraints.add(String.format(Locale.ENGLISH,
                                "Low Voltage constraint reached - biggest violation on node \"%s\" - Minimum voltage of %f kV for a limit of %f kV",
                                voltageCnec.getNetworkElement().getName(),
                                minVoltage,
                                lowerBound));
                    }
                });
                voltageCnec.getUpperBound(Unit.KILOVOLT).ifPresent(upperBound -> {
                    final double maxVoltage = result.getMaxVoltage(instant, voltageCnec, MinOrMax.MAX, Unit.KILOVOLT);
                    if (Double.compare(maxVoltage, upperBound) > 0) {
                        voltageConstraints.add(String.format(Locale.ENGLISH,
                                "High voltage constraint reached - biggest violation on node \"%s\" - Maximum voltage of %f kV for a limit of %f kV",
                                voltageCnec.getNetworkElement().getName(),
                                maxVoltage,
                                upperBound));
                    }
                });
            }
        }
        return voltageConstraints;
    }

    private Network getNetworkWithPra(final DichotomyResult<SweDichotomyValidationData> dichotomyResult) {
        final String networkWithPraUrl = dichotomyResult.getHighestValidStep().getValidationData().getRaoResponse().getNetworkWithPraFileUrl();
        try (final InputStream networkIs = urlValidationService.openUrlStream(networkWithPraUrl)) {
            return Network.read("networkWithPra.xiidm", networkIs);
        } catch (final IOException e) {
            throw new SweInternalException("Could not read network with PRA from RAO response during voltage check", e);
        }
    }
}

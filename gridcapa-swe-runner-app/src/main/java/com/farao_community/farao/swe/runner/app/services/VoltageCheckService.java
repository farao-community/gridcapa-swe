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
import com.farao_community.farao.swe.runner.app.utils.UrlValidationService;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.cnec.VoltageCnec;
import com.powsybl.openrao.monitoring.voltagemonitoring.VoltageMonitoring;
import com.powsybl.openrao.monitoring.voltagemonitoring.VoltageMonitoringResult;
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

    public VoltageCheckService(Logger businessLogger, UrlValidationService urlValidationService) {
        this.businessLogger = businessLogger;
        this.urlValidationService = urlValidationService;
    }

    public Optional<VoltageMonitoringResult> runVoltageCheck(SweData sweData, DichotomyResult<SweDichotomyValidationData> dichotomyResult, DichotomyDirection direction) {

        if (direction != DichotomyDirection.ES_FR && direction != DichotomyDirection.FR_ES) {
            return Optional.empty();
        }
        if (!dichotomyResult.hasValidStep()) {
            businessLogger.warn("Voltage check is in failure because no valid step is available to run it.");
            return Optional.empty();
        }
        businessLogger.info("Running voltage check");
        try {
            Crac crac = sweData.getCracFrEs().getCrac();
            Network network = getNetworkWithPra(dichotomyResult);
            VoltageMonitoring voltageMonitoring = new VoltageMonitoring(crac, network, dichotomyResult.getHighestValidStep().getRaoResult());
            VoltageMonitoringResult result = voltageMonitoring.run(LoadFlow.find().getName(), LoadFlowParameters.load(), 4);
            generateHighAndLowVoltageConstraints(result).forEach(businessLogger::warn);
            return Optional.of(result);
        } catch (Exception e) {
            businessLogger.error("Exception during voltage check : {}", e.getMessage());
            return Optional.empty();
        }
    }

    protected List<String> generateHighAndLowVoltageConstraints(final VoltageMonitoringResult result) {
        final List<String> voltageConstraints = new ArrayList<>();
        for (final VoltageCnec voltageCnec : result.getConstrainedElements()) {
            voltageCnec.getLowerBound(Unit.KILOVOLT).ifPresent(lowerBound -> {
                final Double minVoltage = result.getMinVoltage(voltageCnec);
                if (Double.compare(lowerBound, minVoltage) > 0) {
                    voltageConstraints.add(String.format(Locale.ENGLISH,
                            "Low Voltage constraint reached due to %s %.1f/%.1f kV",
                            voltageCnec.getNetworkElement().getName(),
                            minVoltage,
                            lowerBound));
                }
            });
            voltageCnec.getUpperBound(Unit.KILOVOLT).ifPresent(upperBound -> {
                final Double maxVoltage = result.getMaxVoltage(voltageCnec);
                if (Double.compare(maxVoltage, upperBound) > 0) {
                    voltageConstraints.add(String.format(Locale.ENGLISH,
                            "High Voltage constraint reached due to %s %.1f/%.1f kV",
                            voltageCnec.getNetworkElement().getName(),
                            maxVoltage,
                            upperBound));
                }
            });
        }
        return voltageConstraints;
    }

    private Network getNetworkWithPra(DichotomyResult<SweDichotomyValidationData> dichotomyResult) {
        String networkWithPraUrl = dichotomyResult.getHighestValidStep().getValidationData().getRaoResponse().getNetworkWithPraFileUrl();
        try (InputStream networkIs = urlValidationService.openUrlStream(networkWithPraUrl)) {
            return Network.read("networkWithPra.xiidm", networkIs);
        } catch (IOException e) {
            throw new SweInternalException("Could not read network with PRA from RAO response during voltage check", e);
        }
    }
}

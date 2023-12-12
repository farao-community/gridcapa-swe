/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import com.farao_community.farao.monitoring.voltage_monitoring.VoltageMonitoring;
import com.farao_community.farao.monitoring.voltage_monitoring.VoltageMonitoringResult;
import com.farao_community.farao.gridcapa_swe_commons.exception.SweInternalException;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyValidationData;
import com.farao_community.farao.swe.runner.app.utils.UrlValidationService;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
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
            return Optional.of(voltageMonitoring.run(LoadFlow.find().getName(), LoadFlowParameters.load(), 4));
        } catch (Exception e) {
            businessLogger.error("Exception during voltage check : {}", e.getMessage());
            return Optional.empty();
        }
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

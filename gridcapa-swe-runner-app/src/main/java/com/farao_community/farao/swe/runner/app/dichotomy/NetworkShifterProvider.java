/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy;

import com.farao_community.farao.dichotomy.api.NetworkShifter;
import com.farao_community.farao.dichotomy.shift.ShiftDispatcher;
import com.farao_community.farao.gridcapa_swe_commons.configuration.ProcessConfiguration;
import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import com.farao_community.farao.gridcapa_swe_commons.exception.SweBaseCaseUnsecureException;
import com.farao_community.farao.gridcapa_swe_commons.resource.ProcessType;
import com.farao_community.farao.gridcapa_swe_commons.shift.*;
import com.farao_community.farao.swe.runner.app.configurations.DichotomyConfiguration;
import com.farao_community.farao.swe.runner.app.configurations.ExportNetworkConfiguration;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.services.FileExporter;
import com.farao_community.farao.swe.runner.app.services.NetworkService;
import com.farao_community.farao.swe.runner.app.services.SweNetworkExporter;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@Service
public class NetworkShifterProvider {

    private final DichotomyConfiguration dichotomyConfiguration;
    private final Logger businessLogger;
    private final ProcessConfiguration processConfiguration;
    private final ExportNetworkConfiguration exportNetworkConfiguration;
    private final FileExporter fileExporter;

    public NetworkShifterProvider(DichotomyConfiguration dichotomyConfiguration, Logger businessLogger, ProcessConfiguration processConfiguration, ExportNetworkConfiguration exportNetworkConfiguration, FileExporter fileExporter) {
        this.dichotomyConfiguration = dichotomyConfiguration;
        this.businessLogger = businessLogger;
        this.processConfiguration = processConfiguration;
        this.exportNetworkConfiguration = exportNetworkConfiguration;
        this.fileExporter = fileExporter;
    }

    public NetworkShifter get(SweData sweData, DichotomyDirection direction, LoadFlowParameters loadFlowParameters) {
        ZonalScalableProvider zonalScalableProvider = new ZonalScalableProvider();
        Network network = NetworkService.getNetworkByDirection(sweData, direction);
        try {
            Map<String, Double> initialNetPositions = CountryBalanceComputation.computeSweCountriesBalances(network, loadFlowParameters);

            businessLogger.info("Base case loadflow is secure");
            SweNetworkExporter sweNetworkExporter = exportNetworkConfiguration.isExportFailedNetwork() ? new SweNetworkExporter(sweData, fileExporter) : null;
            return new SweNetworkShifter(businessLogger, sweData.getProcessType(), direction,
                    zonalScalableProvider.get(sweData.getGlskUrl(), network, sweData.getTimestamp()),
                    getShiftDispatcher(sweData.getProcessType(), direction, initialNetPositions),
                    dichotomyConfiguration.getParameters().get(direction).getToleranceEsPt(),
                    dichotomyConfiguration.getParameters().get(direction).getToleranceEsFr(),
                    initialNetPositions,
                    processConfiguration,
                    loadFlowParameters, sweNetworkExporter);
        } catch (SweBaseCaseUnsecureException baseCaseUnsecureException) {
            businessLogger.error("Base case loadflow is unsecure, the calculation is stopped");
            throw baseCaseUnsecureException;
        }
    }

    ShiftDispatcher getShiftDispatcher(ProcessType processType, DichotomyDirection direction, Map<String, Double> initialNetPositions) {
        return switch (processType) {
            case D2CC -> new SweD2ccShiftDispatcher(direction, initialNetPositions);
            case IDCC, IDCC_IDCF, BTCC -> new SweIdccShiftDispatcher(direction, initialNetPositions);
        };
    }
}


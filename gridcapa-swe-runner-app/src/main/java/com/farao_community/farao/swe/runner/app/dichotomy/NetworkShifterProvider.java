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
import com.farao_community.farao.gridcapa_swe_commons.shift.SweD2ccShiftDispatcher;
import com.farao_community.farao.gridcapa_swe_commons.shift.SweIdccShiftDispatcher;
import com.farao_community.farao.gridcapa_swe_commons.shift.SweNetworkShifter;
import com.farao_community.farao.gridcapa_swe_commons.shift.ZonalScalableProvider;
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

import static com.farao_community.farao.gridcapa_swe_commons.shift.CountryBalanceComputation.computeSweCountriesBalances;

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

    public NetworkShifterProvider(final DichotomyConfiguration dichotomyConfiguration,
                                  final Logger businessLogger,
                                  final ProcessConfiguration processConfiguration,
                                  final ExportNetworkConfiguration exportNetworkConfiguration,
                                  final FileExporter fileExporter) {
        this.dichotomyConfiguration = dichotomyConfiguration;
        this.businessLogger = businessLogger;
        this.processConfiguration = processConfiguration;
        this.exportNetworkConfiguration = exportNetworkConfiguration;
        this.fileExporter = fileExporter;
    }

    public NetworkShifter get(final SweData sweData,
                              final DichotomyDirection direction,
                              final LoadFlowParameters loadFlowParameters,
                              final boolean runGlskChecksFirst) {
        final ZonalScalableProvider zonalScalableProvider = new ZonalScalableProvider();
        final Network network = NetworkService.getNetworkByDirection(sweData, direction);
        try {
            final Map<String, Double> initialNetPositions = computeSweCountriesBalances(network, loadFlowParameters);

            businessLogger.info("Base case loadflow is secure");

            final SweNetworkExporter sweNetworkExporter = exportNetworkConfiguration.isExportFailedNetwork()
                ? new SweNetworkExporter(sweData, fileExporter)
                : null;

            final DichotomyConfiguration.Parameters directionParameters = dichotomyConfiguration.getParameters().get(direction);

            return new SweNetworkShifter(businessLogger,
                                         sweData.getProcessType(),
                                         direction,
                                         zonalScalableProvider.get(sweData.getGlskUrl(), network, sweData.getTimestamp()),
                                         getShiftDispatcher(sweData.getProcessType(), direction, initialNetPositions),
                                         directionParameters.getToleranceEsPt(),
                                         directionParameters.getToleranceEsFr(),
                                         initialNetPositions,
                                         processConfiguration,
                                         loadFlowParameters,
                                         sweNetworkExporter,
                                         runGlskChecksFirst);

        } catch (final SweBaseCaseUnsecureException baseCaseUnsecureException) {
            businessLogger.error("Base case loadflow is unsecure, the calculation is stopped and the first unsecure network cannot be exported because it doesn't exist at this stage of the calculation.");
            throw baseCaseUnsecureException;
        }
    }

    ShiftDispatcher getShiftDispatcher(final ProcessType processType,
                                       final DichotomyDirection direction,
                                       final Map<String, Double> initialNetPositionsByCountry) {
        return switch (processType) {
            case D2CC -> new SweD2ccShiftDispatcher(direction, initialNetPositionsByCountry);
            case IDCC, IDCC_IDCF, BTCC -> new SweIdccShiftDispatcher(direction, initialNetPositionsByCountry);
        };
    }
}


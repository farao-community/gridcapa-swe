/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy;

import com.farao_community.farao.dichotomy.api.DichotomyEngine;
import com.farao_community.farao.dichotomy.api.NetworkValidator;
import com.farao_community.farao.dichotomy.api.index.Index;
import com.farao_community.farao.dichotomy.api.index.RangeDivisionIndexStrategy;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.rao_runner.starter.RaoRunnerClient;
import com.farao_community.farao.swe.runner.app.configurations.DichotomyConfiguration;
import com.farao_community.farao.swe.runner.app.configurations.DichotomyConfiguration.Parameters;
import com.farao_community.farao.swe.runner.app.dichotomy.shift.NetworkUtil;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyValidationData;
import com.farao_community.farao.swe.runner.app.services.FileExporter;
import com.farao_community.farao.swe.runner.app.services.FileImporter;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Service
public class DichotomyRunner {
    private static final RangeDivisionIndexStrategy INDEX_STRATEGY_CONFIGURATION = new RangeDivisionIndexStrategy(false);

    private final DichotomyConfiguration dichotomyConfiguration;
    private final DichotomyLogging dichotomyLogging;
    private final FileExporter fileExporter;
    private final FileImporter fileImporter;
    private final NetworkShifterProvider networkShifterProvider;
    private final RaoRunnerClient raoRunnerClient;

    private final Logger businessLogger;

    public DichotomyRunner(DichotomyConfiguration dichotomyConfiguration,
                           DichotomyLogging dichotomyLogging,
                           FileExporter fileExporter,
                           FileImporter fileImporter,
                           NetworkShifterProvider networkShifterProvider,
                           RaoRunnerClient raoRunnerClient,
                           Logger businessLogger) {
        this.dichotomyConfiguration = dichotomyConfiguration;
        this.dichotomyLogging = dichotomyLogging;
        this.fileExporter = fileExporter;
        this.fileImporter = fileImporter;
        this.networkShifterProvider = networkShifterProvider;
        this.raoRunnerClient = raoRunnerClient;
        this.businessLogger = businessLogger;
    }

    public DichotomyResult<SweDichotomyValidationData> run(SweData sweData, DichotomyDirection direction) {
        Parameters parameters = dichotomyConfiguration.getParameters().get(direction);
        dichotomyLogging.logStartDichotomy(parameters);
        DichotomyEngine<SweDichotomyValidationData> engine = buildDichotomyEngine(sweData, direction, parameters);
        Network network = NetworkUtil.getNetworkByDirection(sweData, direction);
        return engine.run(network);
    }

    DichotomyEngine<SweDichotomyValidationData> buildDichotomyEngine(SweData sweData, DichotomyDirection direction, Parameters parameters) {
        return new DichotomyEngine<>(
                new Index<>(parameters.getMinValue(), parameters.getMaxValue(), parameters.getPrecision()),
                INDEX_STRATEGY_CONFIGURATION,
                networkShifterProvider.get(sweData, direction),
                getNetworkValidator(sweData, direction));
    }

    private NetworkValidator<SweDichotomyValidationData> getNetworkValidator(SweData sweData, DichotomyDirection direction) {
        String raoParametersURL = fileExporter.saveRaoParameters(sweData);
        return new RaoValidator(fileExporter, fileImporter, raoParametersURL, raoRunnerClient, sweData, direction, businessLogger);
    }

}

/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy;

import com.farao_community.farao.dichotomy.api.DichotomyEngine;
import com.farao_community.farao.dichotomy.api.NetworkValidator;
import com.farao_community.farao.dichotomy.api.index.HalfRangeDivisionIndexStrategy;
import com.farao_community.farao.dichotomy.api.index.Index;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import com.farao_community.farao.rao_runner.starter.RaoRunnerClient;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyValidationData;
import com.farao_community.farao.swe.runner.app.domain.SweTaskParameters;
import com.farao_community.farao.swe.runner.app.services.FileExporter;
import com.farao_community.farao.swe.runner.app.services.FileImporter;
import com.farao_community.farao.swe.runner.app.services.NetworkService;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Service
public class DichotomyRunner {
    private static final HalfRangeDivisionIndexStrategy HALF_INDEX_STRATEGY_CONFIGURATION = new HalfRangeDivisionIndexStrategy(false);

    private final DichotomyLogging dichotomyLogging;
    private final FileExporter fileExporter;
    private final FileImporter fileImporter;
    private final NetworkShifterProvider networkShifterProvider;
    private final RaoRunnerClient raoRunnerClient;

    private final Logger businessLogger;

    public DichotomyRunner(DichotomyLogging dichotomyLogging,
                           FileExporter fileExporter,
                           FileImporter fileImporter,
                           NetworkShifterProvider networkShifterProvider,
                           RaoRunnerClient raoRunnerClient,
                           Logger businessLogger) {
        this.dichotomyLogging = dichotomyLogging;
        this.fileExporter = fileExporter;
        this.fileImporter = fileImporter;
        this.networkShifterProvider = networkShifterProvider;
        this.raoRunnerClient = raoRunnerClient;
        this.businessLogger = businessLogger;
    }

    public DichotomyResult<SweDichotomyValidationData> run(SweData sweData, SweTaskParameters sweTaskParameters, DichotomyDirection direction) {
        DichotomyParmaters dichotomyParameters = getDirectionParameters(sweTaskParameters, direction);
        dichotomyLogging.logStartDichotomy(dichotomyParameters);
        DichotomyEngine<SweDichotomyValidationData> engine = buildDichotomyEngine(sweData, direction, dichotomyParameters);
        Network network = NetworkService.getNetworkByDirection(sweData, direction);
        return engine.run(network);
    }

    DichotomyEngine<SweDichotomyValidationData> buildDichotomyEngine(SweData sweData, DichotomyDirection direction, DichotomyParmaters parameters) {
        return new DichotomyEngine<>(
                new Index<>(parameters.getMinValue(), parameters.getMaxValue(), parameters.getPrecision()),
                HALF_INDEX_STRATEGY_CONFIGURATION,
                networkShifterProvider.get(sweData, direction),
                getNetworkValidator(sweData, direction));
    }

    private NetworkValidator<SweDichotomyValidationData> getNetworkValidator(SweData sweData, DichotomyDirection direction) {
        return new RaoValidator(fileExporter, fileImporter, raoRunnerClient, sweData, direction, businessLogger);
    }

    private DichotomyParmaters getDirectionParameters(SweTaskParameters sweTaskParameters, DichotomyDirection direction) {
        DichotomyParmaters result = new DichotomyParmaters();
        switch (direction) {
            case ES_FR -> {
                result.setMinValue(sweTaskParameters.getMinPointEsFr());
                result.setMaxValue(sweTaskParameters.getStartingPointEsFr());
                result.setPrecision(sweTaskParameters.getSensitivityEsFr());
            }
            case ES_PT -> {
                result.setMinValue(sweTaskParameters.getMinPointEsPt());
                result.setMaxValue(sweTaskParameters.getStartingPointEsPt());
                result.setPrecision(sweTaskParameters.getSensitivityEsPt());
            }
            case FR_ES -> {
                result.setMinValue(sweTaskParameters.getMinPointFrEs());
                result.setMaxValue(sweTaskParameters.getStartingPointFrEs());
                result.setPrecision(sweTaskParameters.getSensitivityFrEs());
            }
            case PT_ES -> {
                result.setMinValue(sweTaskParameters.getMinPointPtEs());
                result.setMaxValue(sweTaskParameters.getStartingPointPtEs());
                result.setPrecision(sweTaskParameters.getSensitivityPtEs());
            }
        }
        return result;
    }
}

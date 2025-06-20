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
import com.farao_community.farao.swe.runner.app.utils.OpenLoadFlowParametersUtil;
import com.farao_community.farao.swe.runner.app.services.InterruptionService;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Service
public class DichotomyRunner {
    private static final HalfRangeDivisionIndexStrategy<SweDichotomyValidationData> HALF_INDEX_STRATEGY_CONFIGURATION = new HalfRangeDivisionIndexStrategy<>(false);

    private final DichotomyLogging dichotomyLogging;
    private final FileExporter fileExporter;
    private final FileImporter fileImporter;
    private final NetworkShifterProvider networkShifterProvider;
    private final RaoRunnerClient raoRunnerClient;
    private final InterruptionService interruptionService;

    private final Logger businessLogger;

    public DichotomyRunner(DichotomyLogging dichotomyLogging,
                           FileExporter fileExporter,
                           FileImporter fileImporter,
                           NetworkShifterProvider networkShifterProvider,
                           RaoRunnerClient raoRunnerClient,
                           InterruptionService interruptionService,
                           Logger businessLogger) {
        this.dichotomyLogging = dichotomyLogging;
        this.fileExporter = fileExporter;
        this.fileImporter = fileImporter;
        this.networkShifterProvider = networkShifterProvider;
        this.raoRunnerClient = raoRunnerClient;
        this.interruptionService = interruptionService;
        this.businessLogger = businessLogger;
    }

    public DichotomyResult<SweDichotomyValidationData> run(SweData sweData, SweTaskParameters sweTaskParameters, DichotomyDirection direction) {
        DichotomyParameters dichotomyParameters = getDichotomyParameters(sweTaskParameters, direction);
        LoadFlowParameters loadFlowParameters = OpenLoadFlowParametersUtil.getLoadFlowParameters(sweTaskParameters);
        dichotomyLogging.logStartDichotomy(dichotomyParameters);
        DichotomyEngine<SweDichotomyValidationData> engine = buildDichotomyEngine(sweData, direction, dichotomyParameters, loadFlowParameters);
        Network network = NetworkService.getNetworkByDirection(sweData, direction);
        return engine.run(network);
    }

    DichotomyEngine<SweDichotomyValidationData> buildDichotomyEngine(SweData sweData, DichotomyDirection direction, DichotomyParameters parameters, LoadFlowParameters loadFlowParameters) {
        return DichotomyEngine.<SweDichotomyValidationData>builder()
                .withIndex(new Index<>(parameters.getMinValue(), parameters.getMaxValue(), parameters.getPrecision()))
                .withIndexStrategy(HALF_INDEX_STRATEGY_CONFIGURATION)
                .withInterruptionStrategy(interruptionService)
                .withNetworkShifter(networkShifterProvider.get(sweData, direction, loadFlowParameters))
                .withNetworkValidator(getNetworkValidator(sweData, direction, parameters.isRunAngleCheck(), loadFlowParameters))
                .withRunId(sweData.getId())
                .build();
    }

    private NetworkValidator<SweDichotomyValidationData> getNetworkValidator(SweData sweData, DichotomyDirection direction, boolean runAngleCheck, LoadFlowParameters loadFlowParameters) {
        return new RaoValidator(fileExporter, fileImporter, raoRunnerClient, sweData, direction, runAngleCheck, loadFlowParameters, businessLogger);
    }

    private DichotomyParameters getDichotomyParameters(SweTaskParameters sweTaskParameters, DichotomyDirection direction) {
        DichotomyParameters result = new DichotomyParameters();
        switch (direction) {
            case ES_FR -> {
                result.setMinValue(sweTaskParameters.getMinTtcEsFr());
                result.setMaxValue(sweTaskParameters.getMaxTtcEsFr());
                result.setPrecision(sweTaskParameters.getDichotomyPrecisionEsFr());
            }
            case FR_ES -> {
                result.setMinValue(sweTaskParameters.getMinTtcFrEs());
                result.setMaxValue(sweTaskParameters.getMaxTtcFrEs());
                result.setPrecision(sweTaskParameters.getDichotomyPrecisionFrEs());
            }
            case ES_PT -> {
                result.setMinValue(sweTaskParameters.getMinTtcEsPt());
                result.setMaxValue(sweTaskParameters.getMaxTtcEsPt());
                result.setPrecision(sweTaskParameters.getDichotomyPrecisionEsPt());
            }
            case PT_ES -> {
                result.setMinValue(sweTaskParameters.getMinTtcPtEs());
                result.setMaxValue(sweTaskParameters.getMaxTtcPtEs());
                result.setPrecision(sweTaskParameters.getDichotomyPrecisionPtEs());
            }
        }
        result.setRunAngleCheck(sweTaskParameters.isRunAngleCheck());
        return result;
    }
}

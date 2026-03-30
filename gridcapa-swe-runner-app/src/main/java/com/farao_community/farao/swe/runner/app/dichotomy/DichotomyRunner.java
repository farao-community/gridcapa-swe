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
import com.farao_community.farao.swe.runner.app.services.InterruptionService;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import static com.farao_community.farao.swe.runner.app.services.NetworkService.getNetworkByDirection;
import static com.farao_community.farao.swe.runner.app.utils.OpenLoadFlowParametersUtil.getLoadFlowParameters;

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

    public DichotomyRunner(final DichotomyLogging dichotomyLogging,
                           final FileExporter fileExporter,
                           final FileImporter fileImporter,
                           final NetworkShifterProvider networkShifterProvider,
                           final RaoRunnerClient raoRunnerClient,
                           final InterruptionService interruptionService,
                           final Logger businessLogger) {
        this.dichotomyLogging = dichotomyLogging;
        this.fileExporter = fileExporter;
        this.fileImporter = fileImporter;
        this.networkShifterProvider = networkShifterProvider;
        this.raoRunnerClient = raoRunnerClient;
        this.interruptionService = interruptionService;
        this.businessLogger = businessLogger;
    }

    public DichotomyResult<SweDichotomyValidationData> run(final SweData sweData,
                                                           final SweTaskParameters sweTaskParameters,
                                                           final DichotomyDirection direction) {

        final DichotomyParameters dichotomyParameters = getDichotomyParameters(sweTaskParameters, direction);
        dichotomyLogging.logStartDichotomy(dichotomyParameters);
        final DichotomyEngine<SweDichotomyValidationData> engine =
            buildDichotomyEngine(sweData, direction, dichotomyParameters, getLoadFlowParameters(sweTaskParameters));

        final Network networkForDirection = getNetworkByDirection(sweData, direction);

        return engine.run(networkForDirection);
    }

    DichotomyEngine<SweDichotomyValidationData> buildDichotomyEngine(final SweData sweData,
                                                                     final DichotomyDirection direction,
                                                                     final DichotomyParameters parameters,
                                                                     final LoadFlowParameters loadFlowParameters) {
        return DichotomyEngine.<SweDichotomyValidationData>builder()
            .withIndex(new Index<>(parameters.minValue(), parameters.maxValue(), parameters.precision()))
            .withIndexStrategy(HALF_INDEX_STRATEGY_CONFIGURATION)
            .withInterruptionStrategy(interruptionService)
            .withNetworkShifter(networkShifterProvider.get(sweData, direction, loadFlowParameters, parameters.runGlskChecksBeforeLoadFlow()))
            .withNetworkValidator(getNetworkValidator(sweData, direction, parameters.runAngleCheck(), loadFlowParameters))
            .withRunId(sweData.getId())
            .build();
    }

    private NetworkValidator<SweDichotomyValidationData> getNetworkValidator(final SweData sweData,
                                                                             final DichotomyDirection direction,
                                                                             final boolean runAngleCheck,
                                                                             final LoadFlowParameters loadFlowParameters) {
        return new RaoValidator(
            fileExporter, fileImporter, raoRunnerClient, sweData, direction, runAngleCheck, loadFlowParameters, businessLogger
        );
    }

    private DichotomyParameters getDichotomyParameters(final SweTaskParameters sweTaskParameters, final DichotomyDirection direction) {
        final boolean angleCheck = sweTaskParameters.isRunAngleCheck();
        final boolean glskCheck = sweTaskParameters.shouldRunGlskChecksFirst();

        return switch (direction) {
            case ES_FR -> new DichotomyParameters(sweTaskParameters.getMinTtcEsFr(),
                                                  sweTaskParameters.getMaxTtcEsFr(),
                                                  sweTaskParameters.getDichotomyPrecisionEsFr(),
                                                  angleCheck,
                                                  glskCheck);
            case FR_ES -> new DichotomyParameters(sweTaskParameters.getMinTtcFrEs(),
                                                  sweTaskParameters.getMaxTtcFrEs(),
                                                  sweTaskParameters.getDichotomyPrecisionFrEs(),
                                                  angleCheck,
                                                  glskCheck);
            case ES_PT -> new DichotomyParameters(sweTaskParameters.getMinTtcEsPt(),
                                                  sweTaskParameters.getMaxTtcEsPt(),
                                                  sweTaskParameters.getDichotomyPrecisionEsPt(),
                                                  angleCheck,
                                                  glskCheck);
            case PT_ES -> new DichotomyParameters(sweTaskParameters.getMinTtcPtEs(),
                                                  sweTaskParameters.getMaxTtcPtEs(),
                                                  sweTaskParameters.getDichotomyPrecisionPtEs(),
                                                  angleCheck,
                                                  glskCheck);
        };

    }
}

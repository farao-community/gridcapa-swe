/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy;

import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.dichotomy.api.NetworkValidator;
import com.farao_community.farao.dichotomy.api.exceptions.ValidationException;
import com.farao_community.farao.dichotomy.api.results.DichotomyStepResult;
import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import com.farao_community.farao.monitoring.angle_monitoring.AngleMonitoring;
import com.farao_community.farao.monitoring.angle_monitoring.AngleMonitoringResult;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.rao_runner.starter.RaoRunnerClient;
import com.farao_community.farao.gridcapa_swe_commons.exception.SweInvalidDataException;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyValidationData;
import com.farao_community.farao.swe.runner.app.services.FileExporter;
import com.farao_community.farao.swe.runner.app.services.FileImporter;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
public class RaoValidator implements NetworkValidator<SweDichotomyValidationData> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RaoValidator.class);

    private final Logger businessLogger;

    private final FileExporter fileExporter;
    private final FileImporter fileImporter;
    private final RaoRunnerClient raoRunnerClient;
    private final SweData sweData;
    private final DichotomyDirection direction;
    private int variantCounter = 0;
    private static final String REGION = "SWE";
    private static final String MINIO_SEPARATOR = "/";

    public RaoValidator(FileExporter fileExporter, FileImporter fileImporter, RaoRunnerClient raoRunnerClient, SweData sweData, DichotomyDirection direction, Logger businessLogger) {
        this.fileExporter = fileExporter;
        this.fileImporter = fileImporter;
        this.raoRunnerClient = raoRunnerClient;
        this.sweData = sweData;
        this.direction = direction;
        this.businessLogger = businessLogger;
    }

    @Override
    public DichotomyStepResult<SweDichotomyValidationData> validateNetwork(Network network, DichotomyStepResult<SweDichotomyValidationData> lastDichotomyStepResult) throws ValidationException {
        String scaledNetworkDirPath = generateScaledNetworkDirPath(network);
        String scaledNetworkName = network.getNameOrId().replace(":", "") + ".xiidm";
        String networkPresignedUrl = fileExporter.saveNetworkInArtifact(network, scaledNetworkDirPath + scaledNetworkName, "", sweData.getTimestamp(), sweData.getProcessType());
        RaoRequest raoRequest = buildRaoRequest(networkPresignedUrl, scaledNetworkDirPath);
        try {
            LOGGER.info("[{}] : RAO request sent: {}", direction, raoRequest);
            RaoResponse raoResponse = raoRunnerClient.runRao(raoRequest);
            LOGGER.info("[{}] : RAO response received: {}", direction, raoResponse);
            RaoResult raoResult = fileImporter.importRaoResult(raoResponse.getRaoResultFileUrl(), fileImporter.importCracFromJson(raoResponse.getCracFileUrl()));
            if (isPortugalInDirection() && raoResultIsSecure(raoResult)) {
                AngleMonitoring angleMonitoring = new AngleMonitoring(sweData.getCracEsPt().getCrac(), network, raoResult, fileImporter.importCimGlskDocument(sweData.getGlskUrl()));
                AngleMonitoringResult angleMonitoringResult = angleMonitoring.run(LoadFlow.find().getName(), LoadFlowParameters.load(), 4, sweData.getTimestamp());
                businessLogger.info("Angle monitoring result {}", angleMonitoringResult.getStatus());
                return DichotomyStepResult.fromNetworkValidationResult(raoResult, new SweDichotomyValidationData(raoResponse, angleMonitoringResult), angleMonitoringResult.isSecure());
            }
            return DichotomyStepResult.fromNetworkValidationResult(raoResult, new SweDichotomyValidationData(raoResponse));
        } catch (RuntimeException e) {
            throw new ValidationException("RAO run failed", e);
        }
    }

    private RaoRequest buildRaoRequest(String networkPresignedUrl, String scaledNetworkDirPath) {
        String resultsDestination = REGION + MINIO_SEPARATOR + sweData.getProcessType() + MINIO_SEPARATOR + scaledNetworkDirPath;
        String raoParametersUrl = getMatchingRaoParametersUrl(direction);
        return new RaoRequest.RaoRequestBuilder()
                .withId(sweData.getId())
                .withNetworkFileUrl(networkPresignedUrl)
                .withCracFileUrl(getMatchingCracPath(direction, sweData))
                .withRaoParametersFileUrl(raoParametersUrl)
                .withResultsDestination(resultsDestination)
                .withEventPrefix(direction.getDashName())
                .build();
    }

    private String generateScaledNetworkDirPath(Network network) {
        String basePath = fileExporter.makeDestinationDichotomyPath(sweData.getTimestamp(), FileExporter.FileKind.ARTIFACTS, direction);
        String variantName = network.getVariantManager().getWorkingVariantId();
        return String.format("%s/%s-%s/", basePath, ++variantCounter, variantName);
    }

    private String getMatchingCracPath(DichotomyDirection direction, SweData sweData) {
        if (direction.equals(DichotomyDirection.ES_FR) || direction.equals(DichotomyDirection.FR_ES)) {
            return sweData.getJsonCracPathFrEs();
        } else if (direction.equals(DichotomyDirection.ES_PT) || direction.equals(DichotomyDirection.PT_ES)) {
            return sweData.getJsonCracPathEsPt();
        }
        throw new SweInvalidDataException("Unknown direction");
    }

    private String getMatchingRaoParametersUrl(DichotomyDirection direction) {
        if (direction.equals(DichotomyDirection.ES_FR) || direction.equals(DichotomyDirection.FR_ES)) {
            return sweData.getRaoParametersEsFrUrl();
        } else if (direction.equals(DichotomyDirection.ES_PT) || direction.equals(DichotomyDirection.PT_ES)) {
            return sweData.getRaoParametersEsPtUrl();
        }
        throw new SweInvalidDataException("Unknown direction");
    }

    private boolean isPortugalInDirection() {
        return direction == DichotomyDirection.ES_PT || direction == DichotomyDirection.PT_ES;
    }

    private boolean raoResultIsSecure(RaoResult raoResult) {
        return raoResult.getFunctionalCost(Instant.CURATIVE) <= 0.0;
    }
}

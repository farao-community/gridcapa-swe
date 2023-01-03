/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy;

import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.dichotomy.api.NetworkValidator;
import com.farao_community.farao.dichotomy.api.exceptions.ValidationException;
import com.farao_community.farao.dichotomy.api.results.DichotomyStepResult;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.rao_runner.starter.RaoRunnerClient;
import com.farao_community.farao.swe.runner.api.exception.SweInvalidDataException;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.services.FileExporter;
import com.farao_community.farao.swe.runner.app.services.FileImporter;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
public class RaoValidator implements NetworkValidator<RaoResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RaoValidator.class);

    private final FileExporter fileExporter;
    private final FileImporter fileImporter;
    private final String raoParametersUrl;
    private final RaoRunnerClient raoRunnerClient;
    private final SweData sweData;
    private final DichotomyDirection direction;
    private int variantCounter = 0;
    private static final String REGION = "SWE";
    private static final String MINIO_SEPARATOR = "/";

    public RaoValidator(FileExporter fileExporter, FileImporter fileImporter, String raoParametersUrl, RaoRunnerClient raoRunnerClient, SweData sweData, DichotomyDirection direction) {
        this.fileExporter = fileExporter;
        this.fileImporter = fileImporter;
        this.raoParametersUrl = raoParametersUrl;
        this.raoRunnerClient = raoRunnerClient;
        this.sweData = sweData;
        this.direction = direction;
    }

    @Override
    public DichotomyStepResult<RaoResponse> validateNetwork(Network network, DichotomyStepResult<RaoResponse> dichotomyStepResult) throws ValidationException {
        String scaledNetworkDirPath = generateScaledNetworkDirPath(network);
        String scaledNetworkName = network.getNameOrId().replace(":", "") + ".xiidm";
        String networkPresignedUrl = fileExporter.saveNetworkInArtifact(network, scaledNetworkDirPath + scaledNetworkName, "", sweData.getTimestamp(), sweData.getProcessType());
        RaoRequest raoRequest = buildRaoRequest(networkPresignedUrl, scaledNetworkDirPath);
        try {
            LOGGER.info("[{}] : RAO request sent: {}", direction, raoRequest);
            RaoResponse raoResponse = raoRunnerClient.runRao(raoRequest);
            LOGGER.info("[{}] : RAO response received: {}", direction, raoResponse);
            RaoResult raoResult = fileImporter.importRaoResult(raoResponse.getRaoResultFileUrl(), fileImporter.importCracFromJson(raoResponse.getCracFileUrl()));
            return DichotomyStepResult.fromNetworkValidationResult(raoResult, raoResponse);
        } catch (RuntimeException e) {
            throw new ValidationException("RAO run failed. Nested exception: " + e.getMessage());
        }
    }

    private RaoRequest buildRaoRequest(String networkPresignedUrl, String scaledNetworkDirPath) {
        String resultsDestination = REGION + MINIO_SEPARATOR + sweData.getProcessType() + MINIO_SEPARATOR + scaledNetworkDirPath;
        return new RaoRequest(sweData.getId(), networkPresignedUrl, getMatchingCracPath(direction, sweData), raoParametersUrl, resultsDestination);
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
}

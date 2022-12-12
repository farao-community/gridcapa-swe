/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.dichotomy.api.exceptions.ValidationException;
import com.farao_community.farao.dichotomy.api.results.DichotomyStepResult;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.rao_runner.starter.RaoRunnerClient;
import com.farao_community.farao.swe.runner.api.resource.ProcessType;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyValidationData;
import com.farao_community.farao.swe.runner.app.services.FileExporter;
import com.farao_community.farao.swe.runner.app.services.FileImporter;
import com.powsybl.glsk.cim.CimGlskDocument;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManager;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */

@SpringBootTest
class RaoValidatorTest {

    @MockBean
    private FileExporter fileExporter;
    @MockBean
    private FileImporter fileImporter;
    @MockBean
    private RaoRunnerClient raoRunnerClient;
    @MockBean
    private Logger businessLogger;

    @Mock
    private SweData sweData;
    @Mock
    private Network network;
    @Mock
    private VariantManager variantManager;
    @Mock
    private RaoResponse raoResponse;
    @Mock
    private RaoResult raoResult;
    @Mock
    private Crac crac;
    @Mock
    private CimCracCreationContext cimCracCreationContext;
    @Mock
    private CimGlskDocument cimGlskDocument;

    @Test
    void simpleTestPortugal() {

        RaoValidator raoValidator = new RaoValidator(fileExporter, fileImporter, "", raoRunnerClient, sweData, DichotomyDirection.ES_PT, businessLogger);
        when(network.getVariantManager()).thenReturn(variantManager);
        when(network.getNameOrId()).thenReturn("network-id");
        when(variantManager.getWorkingVariantId()).thenReturn("variant-id");
        when(fileExporter.saveNetworkInArtifact(any(Network.class), anyString(), anyString(), any(OffsetDateTime.class), any(ProcessType.class))).thenReturn("an-url");
        when(raoRunnerClient.runRao(any(RaoRequest.class))).thenReturn(raoResponse);
        when(raoResponse.getRaoResultFileUrl()).thenReturn("result-file-url");
        when(raoResponse.getCracFileUrl()).thenReturn("crac-file-url");
        when(fileImporter.importCracFromJson(anyString())).thenReturn(crac);
        when(fileImporter.importRaoResult(anyString(), any(Crac.class))).thenReturn(raoResult);
        when(raoResult.getFunctionalCost(OptimizationState.AFTER_CRA)).thenReturn(-1.0);
        when(sweData.getCracEsPt()).thenReturn(cimCracCreationContext);
        when(sweData.getGlskUrl()).thenReturn("glsk-url");
        when(cimCracCreationContext.getCrac()).thenReturn(crac);
        when(fileImporter.importCimGlskDocument(anyString())).thenReturn(cimGlskDocument);
        when(sweData.getTimestamp()).thenReturn(OffsetDateTime.now());
        try {
            DichotomyStepResult<SweDichotomyValidationData> result = raoValidator.validateNetwork(network, null);
            assertNotNull(result);
            assertFalse(result.isFailed());
        } catch (ValidationException e) {
            fail("RaoValidator shouldn't throw exception here", e);
        }
    }

    @Test
    void simpleTestPortugal2() {
        RaoValidator raoValidator = new RaoValidator(fileExporter, fileImporter, "", raoRunnerClient, sweData, DichotomyDirection.ES_PT, businessLogger);
        when(network.getVariantManager()).thenReturn(variantManager);
        when(network.getNameOrId()).thenReturn("network-id");
        when(variantManager.getWorkingVariantId()).thenReturn("variant-id");
        when(fileExporter.saveNetworkInArtifact(any(Network.class), anyString(), anyString(), any(OffsetDateTime.class), any(ProcessType.class))).thenReturn("an-url");
        when(raoRunnerClient.runRao(any(RaoRequest.class))).thenReturn(raoResponse);
        when(raoResponse.getRaoResultFileUrl()).thenReturn("result-file-url");
        when(raoResponse.getCracFileUrl()).thenReturn("crac-file-url");
        when(fileImporter.importCracFromJson(anyString())).thenReturn(crac);
        when(fileImporter.importRaoResult(anyString(), any(Crac.class))).thenReturn(raoResult);
        when(raoResult.getFunctionalCost(OptimizationState.AFTER_CRA)).thenReturn(22.0);
        when(sweData.getCracEsPt()).thenReturn(cimCracCreationContext);
        when(sweData.getGlskUrl()).thenReturn("glsk-url");
        when(cimCracCreationContext.getCrac()).thenReturn(crac);
        when(fileImporter.importCimGlskDocument(anyString())).thenReturn(cimGlskDocument);
        when(sweData.getTimestamp()).thenReturn(OffsetDateTime.now());
        try {
            DichotomyStepResult<SweDichotomyValidationData> result = raoValidator.validateNetwork(network, null);
            assertNotNull(result);
            assertFalse(result.isFailed());
        } catch (ValidationException e) {
            fail("RaoValidator shouldn't throw exception here", e);
        }
    }

    @Test
    void simpleTestFrance() {

        RaoValidator raoValidator = new RaoValidator(fileExporter, fileImporter, "", raoRunnerClient, sweData, DichotomyDirection.FR_ES, businessLogger);
        when(network.getVariantManager()).thenReturn(variantManager);
        when(network.getNameOrId()).thenReturn("network-id");
        when(variantManager.getWorkingVariantId()).thenReturn("variant-id");
        when(fileExporter.saveNetworkInArtifact(any(Network.class), anyString(), anyString(), any(OffsetDateTime.class), any(ProcessType.class))).thenReturn("an-url");
        when(raoRunnerClient.runRao(any(RaoRequest.class))).thenReturn(raoResponse);
        when(raoResponse.getRaoResultFileUrl()).thenReturn("result-file-url");
        when(raoResponse.getCracFileUrl()).thenReturn("crac-file-url");
        when(fileImporter.importCracFromJson(anyString())).thenReturn(crac);
        when(fileImporter.importRaoResult(anyString(), any(Crac.class))).thenReturn(raoResult);
        when(raoResult.getFunctionalCost(OptimizationState.AFTER_CRA)).thenReturn(-1.0);
        when(sweData.getCracEsPt()).thenReturn(cimCracCreationContext);
        when(sweData.getGlskUrl()).thenReturn("glsk-url");
        when(cimCracCreationContext.getCrac()).thenReturn(crac);
        when(fileImporter.importCimGlskDocument(anyString())).thenReturn(cimGlskDocument);
        when(sweData.getTimestamp()).thenReturn(OffsetDateTime.now());
        try {
            DichotomyStepResult<SweDichotomyValidationData> result = raoValidator.validateNetwork(network, null);
            assertNotNull(result);
            assertFalse(result.isFailed());
        } catch (ValidationException e) {
            fail("RaoValidator shouldn't throw exception here", e);
        }
    }
}

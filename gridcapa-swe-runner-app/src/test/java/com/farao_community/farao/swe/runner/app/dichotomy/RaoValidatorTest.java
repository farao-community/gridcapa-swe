/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy;

import com.farao_community.farao.dichotomy.api.exceptions.RaoFailureException;
import com.farao_community.farao.dichotomy.api.exceptions.RaoInterruptionException;
import com.farao_community.farao.dichotomy.api.exceptions.ValidationException;
import com.farao_community.farao.dichotomy.api.results.DichotomyStepResult;
import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import com.farao_community.farao.gridcapa_swe_commons.resource.ProcessType;
import com.farao_community.farao.rao_runner.api.resource.RaoFailureResponse;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoSuccessResponse;
import com.farao_community.farao.rao_runner.starter.RaoRunnerClient;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyValidationData;
import com.farao_community.farao.swe.runner.app.services.FileExporter;
import com.farao_community.farao.swe.runner.app.services.FileImporter;
import com.powsybl.glsk.cim.CimGlskDocument;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManager;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.io.cim.craccreator.CimCracCreationContext;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.monitoring.results.RaoResultWithAngleMonitoring;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;
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
    private RaoSuccessResponse raoResponse;
    @Mock
    private RaoResult raoResult;
    @Mock
    private Crac crac;
    @Mock
    private CimCracCreationContext cimCracCreationContext;
    @Mock
    private CimGlskDocument cimGlskDocument;

    private static final Instant CURATIVE_INSTANT = Mockito.mock(Instant.class);

    @Test
    void simpleTestPortugalSecureWithAngleCheckParameterTrue() throws RaoFailureException {
        RaoValidator raoValidator = new RaoValidator(fileExporter, fileImporter, raoRunnerClient, sweData, DichotomyDirection.ES_PT, true, LoadFlowParameters.load(), businessLogger);
        when(network.getVariantManager()).thenReturn(variantManager);
        when(network.getNameOrId()).thenReturn("network-id");
        when(variantManager.getWorkingVariantId()).thenReturn("variant-id");
        when(fileExporter.saveNetworkInArtifact(any(Network.class), anyString(), anyString(), any(OffsetDateTime.class), any(ProcessType.class))).thenReturn("an-url");
        when(raoRunnerClient.runRao(any(RaoRequest.class))).thenReturn(raoResponse);
        when(raoResponse.getRaoResultFileUrl()).thenReturn("http://result-file-url");
        when(raoResponse.getCracFileUrl()).thenReturn("crac-file-url");
        when(fileImporter.importCracFromJson(anyString(), any())).thenReturn(crac);
        when(fileImporter.importRaoResult(anyString(), any(Crac.class))).thenReturn(raoResult);
        when(raoResult.isSecure()).thenReturn(true);
        when(raoResult.isSecure(PhysicalParameter.FLOW)).thenReturn(true);
        when(raoResult.getComputationStatus()).thenReturn(ComputationStatus.DEFAULT);
        when(raoResult.getFunctionalCost(CURATIVE_INSTANT)).thenReturn(-1.0);
        when(sweData.getCracEsPt()).thenReturn(cimCracCreationContext);
        when(sweData.getGlskUrl()).thenReturn("glsk-url");
        when(sweData.getTimestamp()).thenReturn(OffsetDateTime.now());
        when(cimCracCreationContext.getCrac()).thenReturn(crac);
        when(fileImporter.importCimGlskDocument(anyString())).thenReturn(cimGlskDocument);
        ZonalData zonalDataMock = Mockito.mock(ZonalData.class);
        when(cimGlskDocument.getZonalScalable(network)).thenReturn(zonalDataMock);
        DichotomyStepResult<SweDichotomyValidationData> lastStepMock = Mockito.mock(DichotomyStepResult.class);
        when(lastStepMock.getRaoResult()).thenReturn(raoResult);
        try {
            DichotomyStepResult<SweDichotomyValidationData> result = raoValidator.validateNetwork(network, lastStepMock);
            assertNotNull(result);
            assertFalse(result.isFailed());
            assertEquals(SweDichotomyValidationData.AngleMonitoringStatus.SECURE, result.getValidationData().getAngleMonitoringStatus());
        } catch (ValidationException | RaoInterruptionException e) {
            fail("RaoValidator shouldn't throw exception here", e);
        }
    }

    @Test
    void simpleTestPortugalUnsecureWithAngleCheckParameterTrue() throws RaoFailureException {
        RaoValidator raoValidator = new RaoValidator(fileExporter, fileImporter, raoRunnerClient, sweData, DichotomyDirection.ES_PT, true, LoadFlowParameters.load(), businessLogger);
        when(network.getVariantManager()).thenReturn(variantManager);
        when(network.getNameOrId()).thenReturn("network-id");
        when(variantManager.getWorkingVariantId()).thenReturn("variant-id");
        when(fileExporter.saveNetworkInArtifact(any(Network.class), anyString(), anyString(), any(OffsetDateTime.class), any(ProcessType.class))).thenReturn("an-url");
        when(raoRunnerClient.runRao(any(RaoRequest.class))).thenReturn(raoResponse);
        when(raoResponse.getRaoResultFileUrl()).thenReturn("http://result-file-url");
        when(raoResponse.getCracFileUrl()).thenReturn("crac-file-url");
        when(fileImporter.importCracFromJson(anyString(), any())).thenReturn(crac);
        when(fileImporter.importRaoResult(anyString(), any(Crac.class))).thenReturn(raoResult);
        when(raoResult.isSecure()).thenReturn(false);
        when(sweData.getCracEsPt()).thenReturn(cimCracCreationContext);
        when(sweData.getGlskUrl()).thenReturn("glsk-url");
        when(cimCracCreationContext.getCrac()).thenReturn(crac);
        when(fileImporter.importCimGlskDocument(anyString())).thenReturn(cimGlskDocument);
        when(sweData.getTimestamp()).thenReturn(OffsetDateTime.now());
        try {
            DichotomyStepResult<SweDichotomyValidationData> result = raoValidator.validateNetwork(network, null);
            assertNotNull(result);
            assertFalse(result.isFailed());
            assertEquals(SweDichotomyValidationData.AngleMonitoringStatus.NONE, result.getValidationData().getAngleMonitoringStatus());
        } catch (ValidationException | RaoInterruptionException e) {
            fail("RaoValidator shouldn't throw exception here", e);
        }
    }

    @Test
    void simpleTestPortugal3() throws RaoFailureException {
        RaoValidator raoValidator = new RaoValidator(fileExporter, fileImporter, raoRunnerClient, sweData, DichotomyDirection.PT_ES, true, LoadFlowParameters.load(), businessLogger);
        when(network.getVariantManager()).thenReturn(variantManager);
        when(network.getNameOrId()).thenReturn("network-id");
        when(variantManager.getWorkingVariantId()).thenReturn("variant-id");
        when(fileExporter.saveNetworkInArtifact(any(Network.class), anyString(), anyString(), any(OffsetDateTime.class), any(ProcessType.class))).thenReturn("an-url");
        when(raoRunnerClient.runRao(any(RaoRequest.class))).thenReturn(raoResponse);
        when(raoResponse.getRaoResultFileUrl()).thenReturn("http://result-file-url");
        when(raoResponse.getCracFileUrl()).thenReturn("crac-file-url");
        when(fileImporter.importCracFromJson(anyString(), any())).thenReturn(crac);
        when(fileImporter.importRaoResult(anyString(), any(Crac.class))).thenReturn(raoResult);
        when(raoResult.isSecure()).thenReturn(true);
        when(raoResult.isSecure(PhysicalParameter.FLOW)).thenReturn(true);
        when(raoResult.getComputationStatus()).thenReturn(ComputationStatus.DEFAULT);
        when(sweData.getCracEsPt()).thenReturn(cimCracCreationContext);
        when(sweData.getGlskUrl()).thenReturn("glsk-url");
        when(cimCracCreationContext.getCrac()).thenReturn(crac);
        when(fileImporter.importCimGlskDocument(anyString())).thenReturn(cimGlskDocument);
        when(sweData.getTimestamp()).thenReturn(OffsetDateTime.now());
        ZonalData zonalDataMock = Mockito.mock(ZonalData.class);
        when(cimGlskDocument.getZonalScalable(network)).thenReturn(zonalDataMock);
        DichotomyStepResult<SweDichotomyValidationData> lastStepMock = Mockito.mock(DichotomyStepResult.class);
        when(lastStepMock.getRaoResult()).thenReturn(raoResult);
        try {
            DichotomyStepResult<SweDichotomyValidationData> result = raoValidator.validateNetwork(network, lastStepMock);
            assertNotNull(result);
            assertFalse(result.isFailed());
            assertEquals(SweDichotomyValidationData.AngleMonitoringStatus.SECURE, result.getValidationData().getAngleMonitoringStatus());
        } catch (ValidationException | RaoInterruptionException e) {
            fail("RaoValidator shouldn't throw exception here", e);
        }
    }

    @Test
    void simpleTestFranceWithAngleCheckParameterTrue() throws RaoFailureException {

        RaoValidator raoValidator = new RaoValidator(fileExporter, fileImporter, raoRunnerClient, sweData, DichotomyDirection.FR_ES, true, LoadFlowParameters.load(), businessLogger);
        when(network.getVariantManager()).thenReturn(variantManager);
        when(network.getNameOrId()).thenReturn("network-id");
        when(variantManager.getWorkingVariantId()).thenReturn("variant-id");
        when(fileExporter.saveNetworkInArtifact(any(Network.class), anyString(), anyString(), any(OffsetDateTime.class), any(ProcessType.class))).thenReturn("an-url");
        when(raoRunnerClient.runRao(any(RaoRequest.class))).thenReturn(raoResponse);
        when(raoResponse.getRaoResultFileUrl()).thenReturn("http://result-file-url");
        when(raoResponse.getCracFileUrl()).thenReturn("crac-file-url");
        when(fileImporter.importCracFromJson(anyString(), any())).thenReturn(crac);
        when(fileImporter.importRaoResult(anyString(), any(Crac.class))).thenReturn(raoResult);
        when(raoResult.getFunctionalCost(Mockito.any())).thenReturn(-1.0);
        when(sweData.getCracEsPt()).thenReturn(cimCracCreationContext);
        when(sweData.getGlskUrl()).thenReturn("glsk-url");
        when(cimCracCreationContext.getCrac()).thenReturn(crac);
        when(fileImporter.importCimGlskDocument(anyString())).thenReturn(cimGlskDocument);
        when(sweData.getTimestamp()).thenReturn(OffsetDateTime.now());
        try {
            DichotomyStepResult<SweDichotomyValidationData> result = raoValidator.validateNetwork(network, null);
            assertNotNull(result);
            assertFalse(result.isFailed());
            assertFalse(result.getRaoResult() instanceof RaoResultWithAngleMonitoring);
            assertEquals(SweDichotomyValidationData.AngleMonitoringStatus.NONE, result.getValidationData().getAngleMonitoringStatus());
        } catch (ValidationException | RaoInterruptionException e) {
            fail("RaoValidator shouldn't throw exception here", e);
        }
    }

    @Test
    void simpleTestPortugalWithAngleCheckParameterFalse() throws RaoFailureException {
        RaoValidator raoValidator = new RaoValidator(fileExporter, fileImporter, raoRunnerClient, sweData, DichotomyDirection.ES_PT, false, LoadFlowParameters.load(), businessLogger);
        when(network.getVariantManager()).thenReturn(variantManager);
        when(network.getNameOrId()).thenReturn("network-id");
        when(variantManager.getWorkingVariantId()).thenReturn("variant-id");
        when(fileExporter.saveNetworkInArtifact(any(Network.class), anyString(), anyString(), any(OffsetDateTime.class), any(ProcessType.class))).thenReturn("an-url");
        when(raoRunnerClient.runRao(any(RaoRequest.class))).thenReturn(raoResponse);
        when(raoResponse.getRaoResultFileUrl()).thenReturn("http://result-file-url");
        when(raoResponse.getCracFileUrl()).thenReturn("crac-file-url");
        when(fileImporter.importCracFromJson(anyString(), any())).thenReturn(crac);
        when(fileImporter.importRaoResult(anyString(), any(Crac.class))).thenReturn(raoResult);
        when(raoResult.getFunctionalCost(CURATIVE_INSTANT)).thenReturn(-1.0);
        when(sweData.getCracEsPt()).thenReturn(cimCracCreationContext);
        when(sweData.getGlskUrl()).thenReturn("glsk-url");
        when(cimCracCreationContext.getCrac()).thenReturn(crac);
        when(fileImporter.importCimGlskDocument(anyString())).thenReturn(cimGlskDocument);
        when(sweData.getTimestamp()).thenReturn(OffsetDateTime.now());
        try {
            DichotomyStepResult<SweDichotomyValidationData> result = raoValidator.validateNetwork(network, null);
            assertNotNull(result);
            assertFalse(result.isFailed());
            assertFalse(result.getRaoResult() instanceof RaoResultWithAngleMonitoring);
            assertEquals(SweDichotomyValidationData.AngleMonitoringStatus.NONE, result.getValidationData().getAngleMonitoringStatus());
        } catch (ValidationException | RaoInterruptionException e) {
            fail("RaoValidator shouldn't throw exception here", e);
        }
    }

    @Test
    void simpleTestSoftInterruption() {
        RaoValidator raoValidator = new RaoValidator(fileExporter, fileImporter, raoRunnerClient, sweData, DichotomyDirection.ES_PT, false, LoadFlowParameters.load(), businessLogger);
        when(network.getVariantManager()).thenReturn(variantManager);
        when(network.getNameOrId()).thenReturn("network-id");
        when(variantManager.getWorkingVariantId()).thenReturn("variant-id");
        when(fileExporter.saveNetworkInArtifact(any(Network.class), anyString(), anyString(), any(OffsetDateTime.class), any(ProcessType.class))).thenReturn("an-url");
        when(raoRunnerClient.runRao(any(RaoRequest.class))).thenReturn(raoResponse);
        when(raoResponse.isInterrupted()).thenReturn(true);
        when(sweData.getTimestamp()).thenReturn(OffsetDateTime.now());

        assertThrows(RaoInterruptionException.class, () -> raoValidator.validateNetwork(network, null));
    }

    @Test
    void simpleTestRaoFailure() {
        final RaoFailureResponse raoFailureResponse = new RaoFailureResponse.Builder().withId("id").withErrorMessage("error").build();
        RaoValidator raoValidator = new RaoValidator(fileExporter, fileImporter, raoRunnerClient, sweData, DichotomyDirection.ES_PT, false, LoadFlowParameters.load(), businessLogger);
        when(network.getVariantManager()).thenReturn(variantManager);
        when(network.getNameOrId()).thenReturn("network-id");
        when(variantManager.getWorkingVariantId()).thenReturn("variant-id");
        when(fileExporter.saveNetworkInArtifact(any(Network.class), anyString(), anyString(), any(OffsetDateTime.class), any(ProcessType.class))).thenReturn("an-url");
        when(raoRunnerClient.runRao(any(RaoRequest.class))).thenReturn(raoFailureResponse);
        when(sweData.getTimestamp()).thenReturn(OffsetDateTime.now());

        assertThrows(RaoFailureException.class, () -> raoValidator.validateNetwork(network, null));
    }

    @Test
    void simpleTestRunRaoThrowsException() {
        RaoValidator raoValidator = new RaoValidator(fileExporter, fileImporter, raoRunnerClient, sweData, DichotomyDirection.ES_PT, false, LoadFlowParameters.load(), businessLogger);
        when(network.getVariantManager()).thenReturn(variantManager);
        when(network.getNameOrId()).thenReturn("network-id");
        when(variantManager.getWorkingVariantId()).thenReturn("variant-id");
        when(fileExporter.saveNetworkInArtifact(any(Network.class), anyString(), anyString(), any(OffsetDateTime.class), any(ProcessType.class))).thenReturn("an-url");
        when(raoRunnerClient.runRao(any(RaoRequest.class))).thenThrow(new RuntimeException());
        when(raoResponse.isInterrupted()).thenReturn(true);
        when(sweData.getTimestamp()).thenReturn(OffsetDateTime.now());

        assertThrows(ValidationException.class, () -> raoValidator.validateNetwork(network, null));
    }
}

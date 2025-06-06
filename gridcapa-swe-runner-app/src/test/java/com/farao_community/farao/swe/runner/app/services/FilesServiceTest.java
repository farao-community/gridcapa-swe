/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.gridcapa_swe_commons.resource.ProcessType;
import com.farao_community.farao.swe.runner.api.resource.SweFileResource;
import com.farao_community.farao.swe.runner.api.resource.SweRequest;
import com.farao_community.farao.swe.runner.app.SweTaskParametersTestUtil;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.domain.SweTaskParameters;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.io.cim.craccreator.CimCracCreationContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@SpringBootTest
class FilesServiceTest {

    @Autowired
    private FilesService filesService;

    @MockitoBean
    private NetworkService networkService;

    @MockitoBean
    private RemoveRemoteVoltageRegulationInFranceService removeRemoteVoltageRegulationInFranceService;

    @MockitoBean
    private FileImporter fileImporter;

    @MockitoBean
    private FileExporter fileExporter;

    @Test
    void simpleImport() {
        when(networkService.importMergedNetwork(any(SweRequest.class))).thenReturn(mock(Network.class));
        when(networkService.loadNetworkFromMinio(any(OffsetDateTime.class))).thenReturn(mock(Network.class));
        when(fileImporter.importCracFromCimCracAndNetwork(any(), any(OffsetDateTime.class), any(Network.class), anyString(), any(SweTaskParameters.class))).thenReturn(mock(CimCracCreationContext.class));
        when(fileExporter.saveCracInJsonFormat(any(Crac.class), anyString(), any(OffsetDateTime.class), any(ProcessType.class))).thenReturn("Crac");
        when(fileImporter.importCgmesFiles(anyString())).thenReturn(InputStream.nullInputStream());
        SweRequest sweRequest = new SweRequest("id", "runId", ProcessType.D2CC, OffsetDateTime.now(), new SweFileResource("name", "url"), new SweFileResource("name", "url"), new SweFileResource("name", "url"), new SweFileResource("name", "url"), new SweFileResource("name", "url"), new SweFileResource("name", "url"), new SweFileResource("name", "url"), new SweFileResource("name", "url"), new SweFileResource("name", "url"), new SweFileResource("name", "url"), new SweFileResource("name", "url"), null, null, new SweFileResource("name", "url"), new ArrayList<>());
        SweData sweData = filesService.importFiles(sweRequest, SweTaskParametersTestUtil.getSweTaskParameters());
        assertNotNull(sweData);
    }

}

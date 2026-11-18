/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.gridcapa_swe_commons.exception.SweInvalidDataException;
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.farao_community.farao.swe.runner.app.services.FilesService.createTemporaryBoundariesDirectory;
import static com.farao_community.farao.swe.runner.app.services.FilesService.deleteTemporaryBoundariesDirectoryIfExists;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

        final SweRequest sweRequest = new SweRequest("id", "runId", ProcessType.D2CC, OffsetDateTime.now(), new SweFileResource("name", "url"), new SweFileResource("name", "url"), new SweFileResource("name", "url"), new SweFileResource("name", "url"), new SweFileResource("name", "url"), new SweFileResource("name", "url"), new SweFileResource("name", "url"), new SweFileResource("name", "url"), new SweFileResource("name", "url"), new SweFileResource("name", "url"), new SweFileResource("name", "url"), new SweFileResource("boundaryEqName", "boundaryEqUrl"), new SweFileResource("boundaryTpName", "boundaryTpUrl"), new SweFileResource("name", "url"), new ArrayList<>());
        final SweData sweData = filesService.importFiles(sweRequest, SweTaskParametersTestUtil.getSweTaskParameters());
        assertNotNull(sweData);
    }

    @Test
    void createTemporaryBoundariesDirectoryTest() {
        final SweFileResource reeEq = new SweFileResource("REE_EQ.xml", Objects.requireNonNull(getClass().getResource("/network/MicroGrid_SWE/network_ES_EQ.xml")).toExternalForm());
        final SweFileResource reeTp = new SweFileResource("REE_TP.xml", Objects.requireNonNull(getClass().getResource("/network/MicroGrid_SWE/network_ES_TP.xml")).toExternalForm());
        final Path pathTempBoundaries = createTemporaryBoundariesDirectory(List.of(reeEq, reeTp));
        assertNotNull(pathTempBoundaries);
    }

    @Test
    void createTemporaryBoundariesDirectoryErrorTest() {
        final SweFileResource sweFileResource = new SweFileResource("test", "url_test");
        assertThrows(SweInvalidDataException.class, () -> createTemporaryBoundariesDirectory(List.of(sweFileResource)));
    }

    @Test
    void deleteTemporaryBoundariesDirectoryIfExistsTest() throws IOException {
        final SweFileResource reeEq = new SweFileResource("REE_EQ.xml", Objects.requireNonNull(getClass().getResource("/network/MicroGrid_SWE/network_ES_EQ.xml")).toExternalForm());
        final Path pathTempBoundaries = createTemporaryBoundariesDirectory(List.of(reeEq));
        assertThat(pathTempBoundaries).exists();
        deleteTemporaryBoundariesDirectoryIfExists(pathTempBoundaries);
        assertThat(pathTempBoundaries).doesNotExist();
    }

    @Test
    void deleteTemporaryBoundariesDirectoryNullTest() {
        assertThatCode(() -> deleteTemporaryBoundariesDirectoryIfExists(null)).doesNotThrowAnyException();
    }
}

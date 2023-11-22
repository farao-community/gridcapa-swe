/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.api;

import com.farao_community.farao.gridcapa_swe_commons.exception.SweInternalException;
import com.farao_community.farao.swe.runner.api.resource.SweRequest;
import com.farao_community.farao.swe.runner.api.resource.SweResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
class JsonApiConverterTest {

    @Test
    void checkSweRequestJsonConversion() throws IOException {
        JsonApiConverter jsonApiConverter = new JsonApiConverter();
        byte[] requestBytes = getClass().getResourceAsStream("/sweRequestMessage.json").readAllBytes();
        SweRequest request = jsonApiConverter.fromJsonMessage(requestBytes, SweRequest.class);
        assertEquals("id", request.getId());
    }

    @Test
    void checkSweResponseJsonConversion() throws IOException {
        JsonApiConverter jsonApiConverter = new JsonApiConverter();
        byte[] responseBytes = getClass().getResourceAsStream("/sweResponseMessage.json").readAllBytes();
        SweResponse response = jsonApiConverter.fromJsonMessage(responseBytes, SweResponse.class);
        assertEquals("id", response.getId());
    }

    @Test
    void checkExceptionJsonConversion() throws URISyntaxException, IOException {
        JsonApiConverter jsonApiConverter = new JsonApiConverter();
        SweInternalException exception = new SweInternalException("Something really bad happened");
        String expectedExceptionMessage = Files.readString(Paths.get(getClass().getResource("/errorMessage.json").toURI()));
        assertEquals(expectedExceptionMessage, new String(jsonApiConverter.toJsonMessage(exception)));
    }
}

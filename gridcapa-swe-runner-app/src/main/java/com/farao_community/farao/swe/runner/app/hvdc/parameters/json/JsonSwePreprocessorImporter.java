/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.swe.runner.app.hvdc.parameters.json;

import com.farao_community.farao.swe.runner.app.hvdc.parameters.SwePreprocessorParameters;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * JSON deserializer for SwePreprocessorParameters
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class JsonSwePreprocessorImporter {
    private JsonSwePreprocessorImporter() {
        // should not be instantiated
    }

    public static SwePreprocessorParameters read(Path jsonFile) {
        Objects.requireNonNull(jsonFile);

        try (InputStream is = Files.newInputStream(jsonFile)) {
            return read(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static SwePreprocessorParameters read(InputStream jsonStream) {
        try {
            ObjectMapper objectMapper = JsonUtil.createObjectMapper();
            SimpleModule module = new SimpleModule();
            module.addDeserializer(SwePreprocessorParameters.class, new SwePreprocessorParametersDeserializer());
            objectMapper.registerModule(module);
            return objectMapper.readValue(jsonStream, SwePreprocessorParameters.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

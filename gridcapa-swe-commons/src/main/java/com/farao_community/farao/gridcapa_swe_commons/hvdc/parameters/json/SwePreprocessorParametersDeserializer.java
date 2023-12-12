/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_swe_commons.hvdc.parameters.json;

import com.farao_community.farao.gridcapa_swe_commons.hvdc.parameters.HvdcCreationParameters;
import com.farao_community.farao.gridcapa_swe_commons.hvdc.parameters.SwePreprocessorParameters;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.Set;

/**
 * @author Peter Mitri {@literal <peter.mitri@rte-france.com>}
 */
public class SwePreprocessorParametersDeserializer extends StdDeserializer<SwePreprocessorParameters> {
    SwePreprocessorParametersDeserializer() {
        super(SwePreprocessorParameters.class);
    }

    @Override
    public SwePreprocessorParameters deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        Set<HvdcCreationParameters> hvdcCreationParametersSet = null;
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            if (jsonParser.getCurrentName().equals("hvdcCreationParameters")) {
                jsonParser.nextToken();
                try {
                    hvdcCreationParametersSet = HvdcCreationParametersArrayDeserializer.deserialize(jsonParser);
                } catch (NoSuchFieldException e) {
                    throw new IOException("Could not deserialize SwePreprocessorParameters", e);
                }
            } else {
                throw new IOException("Unexpected field in SwePreprocessorParameters: " + jsonParser.getCurrentName());
            }
        }
        return new SwePreprocessorParameters(hvdcCreationParametersSet);
    }
}

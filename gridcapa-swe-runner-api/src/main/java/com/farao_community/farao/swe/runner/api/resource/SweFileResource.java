/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.api.resource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * References one external file exchanged in the SWE broker contract by pairing a logical file name with a downloadable URL.
 * {@link SweRequest} aggregates these resources for CGMES, CRAC and GLSK inputs, and services such as
 * {@code NetworkService} or {@code FileImporter} dereference the URL to load the actual content.
 *
 * @see SweRequest request carrying the input file references
 * @see com.farao_community.farao.swe.runner.app.services.NetworkService consumer assembling CGMES inputs from these resources
 * @see com.farao_community.farao.swe.runner.app.services.FileImporter consumer opening the referenced files
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
public class SweFileResource {
    private final String filename;
    private final String url;

    @JsonCreator
    public SweFileResource(@JsonProperty("filename") String filename,
                           @JsonProperty("url") String url) {
        this.filename = Objects.requireNonNull(filename);
        this.url = Objects.requireNonNull(url);
    }

    public String getFilename() {
        return filename;
    }

    public String getUrl() {
        return url;
    }

}

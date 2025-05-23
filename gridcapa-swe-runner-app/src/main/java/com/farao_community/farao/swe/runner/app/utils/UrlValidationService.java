/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.swe.runner.app.utils;

import com.farao_community.farao.gridcapa_swe_commons.exception.SweInvalidDataException;
import com.farao_community.farao.swe.runner.app.configurations.UrlConfiguration;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.StringJoiner;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */
@Component
public class UrlValidationService {
    private final UrlConfiguration urlConfiguration;

    public UrlValidationService(UrlConfiguration urlConfiguration) {
        this.urlConfiguration = urlConfiguration;
    }

    public InputStream openUrlStream(String urlString) {
        if (urlConfiguration.getWhitelist().stream().noneMatch(urlString::startsWith)) {
            StringJoiner sj = new StringJoiner(", ", "Whitelist: ", ".");
            urlConfiguration.getWhitelist().forEach(sj::add);
            throw new SweInvalidDataException(String.format("URL '%s' is not part of application's whitelisted url's %s", urlString, sj));
        }
        try {
            URL url = new URI(urlString).toURL();
            return url.openStream();
        } catch (IOException | URISyntaxException | IllegalArgumentException e) {
            throw new SweInvalidDataException(String.format("Cannot download FileResource file from URL '%s'", urlString), e);
        }
    }
}

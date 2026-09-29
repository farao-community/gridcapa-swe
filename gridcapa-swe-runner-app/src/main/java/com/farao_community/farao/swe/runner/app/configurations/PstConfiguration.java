/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.configurations;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Binds the list of phase-shifting transformer identifiers configured for SWE preprocessing.
 * {@link com.farao_community.farao.swe.runner.app.services.NetworkService} uses these identifiers when preparing
 * imported networks and disabling the targeted PST regulation behavior.
 *
 * <p>The {@code pstIds} list selects the transformers to which that PST-specific treatment applies.</p>
 *
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Configuration
@ConfigurationProperties(prefix = "swe-runner.pst")
public class PstConfiguration {
    private List<String> pstIds;

    public List<String> getPstIds() {
        return pstIds;
    }

    public void setPstIds(List<String> pstIds) {
        this.pstIds = pstIds;
    }
}

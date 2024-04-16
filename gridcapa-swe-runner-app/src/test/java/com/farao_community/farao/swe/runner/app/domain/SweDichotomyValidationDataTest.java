/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.domain;

import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import org.junit.jupiter.api.Test;

import static com.farao_community.farao.swe.runner.app.domain.SweDichotomyValidationData.AngleMonitoringStatus.SECURE;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */
class SweDichotomyValidationDataTest {

    @Test
    void testConstructor() {
        RaoResponse raoResponse = new RaoResponse.RaoResponseBuilder().build();
        SweDichotomyValidationData.AngleMonitoringStatus status = SECURE;
        SweDichotomyValidationData constructedData = new SweDichotomyValidationData(raoResponse, status);
        assertEquals(raoResponse, constructedData.getRaoResponse());
        assertEquals(SECURE, constructedData.getAngleMonitoringStatus());
    }
}

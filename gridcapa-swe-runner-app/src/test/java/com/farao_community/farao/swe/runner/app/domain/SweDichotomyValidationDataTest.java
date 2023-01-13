/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.domain;

import com.farao_community.farao.monitoring.angle_monitoring.AngleMonitoringResult;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */

class SweDichotomyValidationDataTest {

    @Test
    void simpleTest() {
        RaoResponse raoResponse = new RaoResponse("ID", null, null, null, null, null, null);
        AngleMonitoringResult angleMonitoringResult = new AngleMonitoringResult(null, null, AngleMonitoringResult.Status.DIVERGENT);
        SweDichotomyValidationData data = new SweDichotomyValidationData(raoResponse, angleMonitoringResult);
        assertEquals(raoResponse, data.getRaoResponse());
        assertEquals(angleMonitoringResult, data.getAngleMonitoringResult());
    }

    @Test
    void simpleWithoutAngleMonitoringResultTest() {
        RaoResponse raoResponse = new RaoResponse("ID", null, null, null, null, null, null);
        SweDichotomyValidationData data = new SweDichotomyValidationData(raoResponse);
        assertEquals(raoResponse, data.getRaoResponse());
        assertNull(data.getAngleMonitoringResult());
    }

}

/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.swe.runner.app.service;

import com.farao_community.farao.swe.api.resource.SweRequest;
import com.farao_community.farao.swe.api.resource.SweResponse;
import com.farao_community.farao.swe.runner.app.util.Threadable;
import org.springframework.stereotype.Service;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@Service
public class SweRunner {

    @Threadable
    public SweResponse run(SweRequest sweRequest) {
        return new SweResponse(sweRequest.getId());
    }

}

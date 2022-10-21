/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.domain;

import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.swe.runner.app.dichotomy.DichotomyDirection;

import java.util.Map;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
public class SweDichotomyResult {

    private final Map<DichotomyDirection, DichotomyResult<RaoResponse>> mapDichotomyResult;

    public SweDichotomyResult(Map<DichotomyDirection, DichotomyResult<RaoResponse>> mapDichotomyResult) {
        this.mapDichotomyResult = mapDichotomyResult;
    }

    public Map<DichotomyDirection, DichotomyResult<RaoResponse>> getMapDichotomyResult() {
        return mapDichotomyResult;
    }
}

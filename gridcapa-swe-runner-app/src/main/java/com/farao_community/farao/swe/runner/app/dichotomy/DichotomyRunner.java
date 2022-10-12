/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy;

import com.farao_community.farao.swe.runner.app.configurations.DichotomyConfiguration;
import com.farao_community.farao.swe.runner.app.configurations.DichotomyConfiguration.Parameters;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.farao_community.farao.swe.runner.app.utils.Direction;
import org.springframework.stereotype.Service;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Service
public class DichotomyRunner {

    private final DichotomyConfiguration dichotomyConfiguration;
    private final DichotomyLogging dichotomyLogging;

    public DichotomyRunner(DichotomyConfiguration dichotomyConfiguration, DichotomyLogging dichotomyLogging) {
        this.dichotomyConfiguration = dichotomyConfiguration;
        this.dichotomyLogging = dichotomyLogging;
    }

    public String run(SweData sweData, Direction direction) {
        Parameters parameters = dichotomyConfiguration.getParameters().get(direction);
        dichotomyLogging.logStartDichotomy(direction, parameters);
        return null;
    }

}

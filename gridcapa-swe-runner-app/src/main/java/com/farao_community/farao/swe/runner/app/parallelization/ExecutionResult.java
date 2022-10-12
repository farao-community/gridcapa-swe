/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.parallelization;

import java.util.List;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
public class ExecutionResult {

    private final List<?> result;

    ExecutionResult(final List<?> result) {
        this.result = result;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(final int index) {
        return (T) result.get(index);
    }
}

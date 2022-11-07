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
public class ExecutionResult<T> {

    private final List<T> result;

    public ExecutionResult(final List<T> result) {
        this.result = result;
    }

    public List<T> getResult() {
        return result;
    }

    public T get(final int index) {
        return result.get(index);
    }

}

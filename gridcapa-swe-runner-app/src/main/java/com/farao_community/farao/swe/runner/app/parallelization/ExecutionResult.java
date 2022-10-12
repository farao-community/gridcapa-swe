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
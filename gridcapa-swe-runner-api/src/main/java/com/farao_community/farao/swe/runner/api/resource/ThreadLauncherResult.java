/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.api.resource;

import java.util.Optional;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
public class ThreadLauncherResult<U> {

    private final Optional<U> result;
    private final boolean hasError;
    private final Exception exception;

    public ThreadLauncherResult(Optional<U> result, boolean hasError, Exception exception) {
        this.result = result;
        this.hasError = hasError;
        this.exception = exception;
    }

    public static <U> ThreadLauncherResult<U> success(U result) {
        return new ThreadLauncherResult<>(Optional.of(result), false, null);
    }

    public static <U> ThreadLauncherResult<U> interrupt() {
        return new ThreadLauncherResult<>(Optional.empty(), false, null);
    }

    public static <U> ThreadLauncherResult<U> error(Exception e) {
        return new ThreadLauncherResult<>(Optional.empty(), true, e);
    }

    public Optional<U> getResult() {
        return result;
    }

    public boolean hasError() {
        return hasError;
    }

    public Exception getException() {
        return exception;
    }
}

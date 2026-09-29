/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_swe_commons.exception;

/**
 * Base runtime exception for SWE business and technical failures so callers can expose a uniform JSON-API status, code, title and details payload.
 * Extended by {@link SweInternalException}, {@link SweInvalidDataException}, {@link SweInvalidDataNoDetailsException} and {@link SweBaseCaseUnsecureException}; {@link #getDetails()} also flattens nested causes and stops on {@link SweInvalidDataNoDetailsException} when details must stay compact.
 *
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
public abstract class AbstractSweException extends RuntimeException {
    protected AbstractSweException(String message) {
        super(message);
    }

    protected AbstractSweException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public abstract int getStatus();

    public abstract String getCode();

    public final String getTitle() {
        return getMessage();
    }

    public final String getDetails() {
        String message = getMessage();
        if (message != null) {
            StringBuilder sb = new StringBuilder(message);
            Throwable cause = getCause();
            while (cause != null) {
                sb.append("; nested exception is ").append(cause);
                cause = cause.getCause();
                if (cause instanceof SweInvalidDataNoDetailsException) {
                    return cause.getMessage();
                }
            }
            return sb.toString();
        } else {
            return "No details";
        }
    }
}

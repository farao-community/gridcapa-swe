/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_swe_commons.exception;

/**
 * Signals that the base-case network cannot be secured because the prerequisite load flow diverged during SWE balance evaluation.
 * Extends {@link AbstractSweException} with a dedicated 500 error code and is thrown by {@link com.farao_community.farao.gridcapa_swe_commons.shift.CountryBalanceComputation} so orchestration can distinguish an unsecure base case from other internal failures.
 *
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */
public class SweBaseCaseUnsecureException extends AbstractSweException {
    private static final int STATUS = 500;
    private static final String CODE = "501-swe-base-case-unsecure-Internal-Server-Exception";

    public SweBaseCaseUnsecureException(String message) {
        super(message);
    }

    public SweBaseCaseUnsecureException(String message, Throwable throwable) {
        super(message, throwable);
    }

    @Override
    public int getStatus() {
        return STATUS;
    }

    @Override
    public String getCode() {
        return CODE;
    }
}

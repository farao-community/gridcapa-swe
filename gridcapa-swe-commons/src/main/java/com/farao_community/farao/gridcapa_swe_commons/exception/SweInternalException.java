/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_swe_commons.exception;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
public class SweInternalException extends AbstractSweException {
    private static final int STATUS = 500;
    private static final String CODE = "500-swe-Internal-Server-Exception";

    public SweInternalException(String message) {
        super(message);
    }

    public SweInternalException(String message, Throwable throwable) {
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

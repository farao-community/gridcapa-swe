/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_swe_commons.exception;

/**
 */
public class SweInvalidDataNoDetailsException extends AbstractSweException {

    private static final int STATUS = 400;
    private static final String CODE = "400-InvalidDataException";

    public SweInvalidDataNoDetailsException(String message) {
        super(message);
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

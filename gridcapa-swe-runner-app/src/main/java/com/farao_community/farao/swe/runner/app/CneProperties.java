/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.swe.runner.app;

public enum CneProperties {
    DOCUMENT_ID("document-id"),
    REVISION_NUMBER("revision-number"),
    DOMAIN_ID("domain-id"),
    PROCESS_TYPE("process-type"),
    SENDER_ID("sender-id"),
    SENDER_ROLE("sender-role"),
    RECEIVER_ID("receiver-id"),
    RECEIVER_ROLE("receiver-role"),
    TIME_INTERVAL("time-interval");

    private static final String PROPERTIES_PREFIX = "rao-result.export.swe-cne.";
    private final String key;

    CneProperties(final String key) {
        this.key = key;
    }

    public String getPrefixedKey() {
        return PROPERTIES_PREFIX + key;
    }
}

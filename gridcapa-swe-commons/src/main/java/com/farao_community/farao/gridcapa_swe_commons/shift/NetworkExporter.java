/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_swe_commons.shift;

import com.powsybl.iidm.network.Network;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
public interface NetworkExporter {
    void export(Network network);
}

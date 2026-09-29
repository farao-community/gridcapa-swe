/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_swe_commons.shift;

import com.powsybl.iidm.network.Network;

/**
 * Extension point used by SWE shifting code to persist or publish a network snapshot when a balancing run fails and diagnostics are needed.
 * Called by {@link SweNetworkShifter} and implemented by runner-side exporters such as {@link com.farao_community.farao.swe.runner.app.services.SweNetworkExporter}.
 *
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
public interface NetworkExporter {
    void export(Network network);
}

/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.domain;

import com.powsybl.iidm.network.Network;

import java.util.Map;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
public class MergingViewData {
    private final Network mergedNetwork;
    private final Map<String, String> subnetworkIdByCountry;

    public MergingViewData(Network mergedNetwork, Map<String, String> subnetworkIdByCountry) {
        this.mergedNetwork = mergedNetwork;
        this.subnetworkIdByCountry = subnetworkIdByCountry;
    }

    public Network getMergedNetwork() {
        return mergedNetwork;
    }

    public Map<String, String> getSubnetworkIdByCountry() {
        return subnetworkIdByCountry;
    }
}

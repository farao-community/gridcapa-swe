/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.domain;

import com.powsybl.iidm.mergingview.MergingView;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@SpringBootTest
class MergingViewDataTest {

    @Mock
    private Network networkFr;

    @Mock
    private Network networkEs;

    @Mock
    private Network networkPt;

    @Mock
    private MergingView mergingView;

    @Test
    void simpleTest() {
        MergingViewData mergingViewData = new MergingViewData(networkFr, networkEs, networkPt, mergingView);
        assertEquals(this.networkFr, mergingViewData.getNetworkFr());
        assertEquals(this.networkEs, mergingViewData.getNetworkEs());
        assertEquals(this.networkPt, mergingViewData.getNetworkPt());
        assertEquals(this.mergingView, mergingViewData.getMergingView());
    }
}

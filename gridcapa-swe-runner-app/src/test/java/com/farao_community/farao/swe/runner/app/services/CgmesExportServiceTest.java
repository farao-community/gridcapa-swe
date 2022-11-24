/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.powsybl.iidm.mergingview.MergingView;
import com.powsybl.iidm.network.Network;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@SpringBootTest
public class CgmesExportServiceTest {

    @Autowired
    private CgmesExportService cgmesExportService;

    @MockBean
    private DichotomyResult<RaoResponse> dichotomyResult;

    @Mock
    private Network networkAll;

    @Mock
    private MergingView mergingView;

    @Mock
    private Crac cracFrEs;

    @Mock
    private Crac cracEsPt;

}

/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.dichotomy.api.results.DichotomyStepResult;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.swe.runner.api.resource.ProcessType;
import com.farao_community.farao.swe.runner.app.dichotomy.DichotomyDirection;
import com.farao_community.farao.swe.runner.app.domain.MergingViewData;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.powsybl.iidm.mergingview.MergingView;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.OffsetDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    void testBuildAndExport() {
        MergingViewData mergingViewData = new MergingViewData(networkAll, networkAll, networkAll, mergingView);
        SweData sweData = new SweData("id", OffsetDateTime.now(), ProcessType.D2CC, networkAll, mergingViewData, cracEsPt, cracFrEs, "", "", "");
     //   when(dichotomyResult.getHighestValidStep().getRaoResult().getActivatedNetworkActionsDuringState(any())).thenReturn(Collections.emptySet());
       // cgmesExportService.buildAndExportCgmesFiles(DichotomyDirection.ES_FR, sweData, dichotomyResult);
    }

}

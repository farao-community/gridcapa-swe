/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.pstregulation.reports;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import org.slf4j.Logger;

import java.util.Set;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
public final class PstRegulationReports {
    private PstRegulationReports() {
        // Utility class should not be instantiated
    }

    public static void reportContingencyScenariosToRegulate(final Logger businessLogger, final Set<Contingency> contingencies) {
        final int nbContingencies = contingencies.size();
        final String contingenciesList = String.join(", ", contingencies.stream().map(contingency -> contingency.getName().orElse(contingency.getId())).sorted().toList());
        businessLogger.info("{} contingency scenario(s) to regulate: {}", nbContingencies, contingenciesList);
    }

    public static void reportPstsToRegulate(final Logger businessLogger, final Set<PstRangeAction> rangeActionsToRegulate) {
        final int nbPsts = rangeActionsToRegulate.size();
        final String pstsList = String.join(", ", rangeActionsToRegulate.stream().map(PstRangeAction::getName).sorted().toList());
        businessLogger.info("{} PST(s) to regulate: {}", nbPsts, pstsList);
    }

}

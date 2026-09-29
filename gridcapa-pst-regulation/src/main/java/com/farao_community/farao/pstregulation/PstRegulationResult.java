/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.pstregulation;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;

import java.util.Map;

/**
 * Carries the tap positions obtained after one PST-regulation run, optionally attached to the
 * {@link Contingency} scenario that was simulated.
 *
 * <p>{@link PstRegulation} uses these results to report tap movements and to merge regulated
 * {@link PstRangeAction} setpoints back into the final RAO result. The {@code regulatedTapByPst} map stores solved
 * integer tap positions keyed by the corresponding PST range action; {@code contingency} is {@code null} for the
 * preventive case.
 *
 * @see PstRegulation produces and merges these results
 * @see PstRegulator computes the regulated tap positions stored here
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public record PstRegulationResult(Contingency contingency, Map<PstRangeAction, Integer> regulatedTapByPst) {
}

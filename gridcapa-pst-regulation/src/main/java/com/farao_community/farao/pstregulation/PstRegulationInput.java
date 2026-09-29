/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.pstregulation;

import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;

import java.util.Set;

/**
 * Groups the data needed to regulate PSTs for one RAO state: the {@link State} being processed, the overloaded
 * {@link FlowCnec} that triggered the regulation, and the per-PST instructions required to configure the load flow.
 *
 * <p>{@link PstRegulation} builds this record before delegating the actual tap computation to {@link PstRegulator};
 * {@link ElementaryPstRegulationInput} entries describe each regulated PST individually.
 *
 * @see PstRegulation selects the limiting element and creates these inputs
 * @see PstRegulator consumes the elementary inputs to run PST regulation
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public record PstRegulationInput(State curativeState, FlowCnec limitingElement, Set<ElementaryPstRegulationInput> elementaryPstRegulationInputs) {
}

/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy;

/**
 * Immutable set of per-direction parameters used to configure one dichotomy execution. It is built by
 * {@link DichotomyRunner} from {@link com.farao_community.farao.swe.runner.app.domain.SweTaskParameters} and then
 * consumed by {@link DichotomyLogging} and the constructed dichotomy engine.
 *
 * <p>{@code minValue} and {@code maxValue} define the TTC search interval, {@code precision} is the stopping step
 * for the dichotomy, {@code runAngleCheck} enables post-RAO angle monitoring, and
 * {@code runGlskChecksBeforeLoadFlow} enables GLSK consistency checks before load-flow execution.</p>
 */
public record DichotomyParameters(double minValue,
                                  double maxValue,
                                  double precision,
                                  boolean runAngleCheck,
                                  boolean runGlskChecksBeforeLoadFlow) {
}

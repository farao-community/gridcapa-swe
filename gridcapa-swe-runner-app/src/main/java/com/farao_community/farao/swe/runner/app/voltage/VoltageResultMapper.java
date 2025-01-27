/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.voltage;

import com.farao_community.farao.swe.runner.app.voltage.json.VoltageCheckConstraintElement;
import com.farao_community.farao.swe.runner.app.voltage.json.VoltageCheckResult;
import com.powsybl.openrao.commons.MinOrMax;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.monitoring.results.RaoResultWithVoltageMonitoring;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.powsybl.openrao.commons.Unit.KILOVOLT;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */
@Component
public class VoltageResultMapper {

    public VoltageCheckResult mapVoltageResult(final RaoResultWithVoltageMonitoring voltageMonitoringResult,
                                               final Crac crac) {
        final List<VoltageCheckConstraintElement> constraintElements = crac.getVoltageCnecs()
                .stream()
                .filter(voltageCnec -> voltageCnec.getState().getInstant().isCurative())
                .filter(voltageCnec -> voltageMonitoringResult.getMargin(voltageCnec.getState().getInstant(), voltageCnec, KILOVOLT) < 0)
                .map(voltageCnec -> {
                    final Instant instant = voltageCnec.getState().getInstant();
                    final String contingency = voltageCnec.getState().getContingency().get().getId();
                    return new VoltageCheckConstraintElement(
                            voltageCnec.getNetworkElement().getId(),
                            instant.getKind().toString(),
                            contingency,
                            voltageMonitoringResult.getMinVoltage(instant, voltageCnec, MinOrMax.MIN, KILOVOLT),
                            voltageMonitoringResult.getMaxVoltage(instant, voltageCnec, MinOrMax.MAX, KILOVOLT),
                            voltageCnec.getLowerBound(KILOVOLT).orElse(null),
                            voltageCnec.getUpperBound(KILOVOLT).orElse(null));
                })
                .toList();
        return new VoltageCheckResult(voltageMonitoringResult.getSecurityStatus(), constraintElements);
    }
}

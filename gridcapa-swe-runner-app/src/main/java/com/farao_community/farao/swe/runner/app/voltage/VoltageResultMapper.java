/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.voltage;

import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.monitoring.voltage_monitoring.VoltageMonitoringResult;
import com.farao_community.farao.swe.runner.app.voltage.json.VoltageCheckConstraintElement;
import com.farao_community.farao.swe.runner.app.voltage.json.VoltageCheckResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static com.farao_community.farao.commons.Unit.KILOVOLT;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */
@Component
public class VoltageResultMapper {

    public VoltageCheckResult mapVoltageResult(VoltageMonitoringResult voltageMonitoringResult) {
        List<VoltageCheckConstraintElement> constraintElements = voltageMonitoringResult.getConstrainedElements().stream().map(voltageCnec -> {
            Instant instant = voltageCnec.getState().getInstant();
            String contingency = instant == Instant.PREVENTIVE ? null : voltageCnec.getState().getContingency().get().getId();
            return new VoltageCheckConstraintElement(
                    voltageCnec.getNetworkElement().getId(),
                    instant,
                    contingency,
                    voltageMonitoringResult.getMinVoltage(voltageCnec),
                    voltageMonitoringResult.getMaxVoltage(voltageCnec),
                    voltageCnec.getLowerBound(KILOVOLT).orElse(null),
                    voltageCnec.getUpperBound(KILOVOLT).orElse(null));
        }).collect(Collectors.toList());
        return new VoltageCheckResult(voltageMonitoringResult.getStatus(), constraintElements);
    }
}

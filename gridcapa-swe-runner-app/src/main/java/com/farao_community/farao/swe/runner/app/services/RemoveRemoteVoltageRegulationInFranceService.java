/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.gridcapa_swe_commons.exception.SweInternalException;
import com.farao_community.farao.swe.runner.app.configurations.DataFixConfiguration;
import com.powsybl.iidm.network.*;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@Service
public class RemoveRemoteVoltageRegulationInFranceService {
    public record ReplacedVoltageRegulation(double value, String regulatingTerminalConnectableId, ThreeSides regulatingTerminalConnectableSide) {
    }

    private final DataFixConfiguration dataFixConfiguration;
    private final Logger businessLogger;

    public RemoveRemoteVoltageRegulationInFranceService(DataFixConfiguration dataFixConfiguration, Logger businessLogger) {
        this.dataFixConfiguration = dataFixConfiguration;
        this.businessLogger = businessLogger;
    }

    public Map<String, ReplacedVoltageRegulation> removeRemoteVoltageRegulationInFrance(Network network) {
        if (!dataFixConfiguration.removeRemoteVoltageRegulationInFrance()) {
            return Collections.emptyMap();
        }
        businessLogger.info("Removing remote voltage regulations in France.");
        return network.getGeneratorStream()
                .filter(RemoveRemoteVoltageRegulationInFranceService::isRemoteVoltageRegulationOn)
                .filter(RemoveRemoteVoltageRegulationInFranceService::isInFrance)
                .collect(Collectors.toMap(
                        Generator::getId,
                        this::removeRemoteVoltageRegulation
                ));
    }

    public void resetRemoteVoltageRegulationInFrance(Network network, Map<String, ReplacedVoltageRegulation> generatorsExtension) {
        generatorsExtension.forEach((generatorId, replacedVoltageRegulation) -> {
            Generator generator = network.getGenerator(generatorId);
            Terminal regulatingTerminal = network.getConnectable(replacedVoltageRegulation.regulatingTerminalConnectableId).getTerminals().stream()
                    .filter(terminal -> terminal.getSide() == replacedVoltageRegulation.regulatingTerminalConnectableSide)
                    .findAny()
                    .orElseThrow(() -> new SweInternalException("Could not find regulating terminal for resetting remote voltage regulation of generator " + generatorId));
            generator.setRegulatingTerminal(regulatingTerminal);
            generator.setTargetV(replacedVoltageRegulation.value());
        });
    }

    private ReplacedVoltageRegulation removeRemoteVoltageRegulation(Generator generator) {
        double oldVoltageTarget = generator.getTargetV();
        ReplacedVoltageRegulation replacedVoltageRegulation = new ReplacedVoltageRegulation(oldVoltageTarget, generator.getRegulatingTerminal().getConnectable().getId(), generator.getRegulatingTerminal().getSide());
        double oldVoltageTargetInPu = oldVoltageTarget / generator.getRegulatingTerminal().getVoltageLevel().getNominalV();
        double newVoltageTarget = oldVoltageTargetInPu * generator.getTerminal().getVoltageLevel().getNominalV();
        generator.setTargetV(newVoltageTarget);
        generator.setRegulatingTerminal(generator.getTerminal());
        businessLogger.info("Deactivating remote voltage regulation of generator '{}'. Local target set to {} kV", generator.getId(), newVoltageTarget);
        return replacedVoltageRegulation;
    }

    private static boolean isInFrance(Generator generator) {
        Optional<Substation> substation = generator.getTerminal().getVoltageLevel().getSubstation();
        if (substation.isEmpty()) {
            return false;
        }
        Optional<Country> country = substation.get().getCountry();
        if (country.isEmpty()) {
            return false;
        }
        return country.get() == Country.FR;
    }

    private static boolean isRemoteVoltageRegulationOn(Generator generator) {
        return generator.isVoltageRegulatorOn() && generator.getTerminal() != generator.getRegulatingTerminal();
    }
}

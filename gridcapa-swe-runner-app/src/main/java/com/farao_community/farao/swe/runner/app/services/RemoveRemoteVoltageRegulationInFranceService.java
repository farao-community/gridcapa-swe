/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.swe.runner.app.configurations.DataFixConfiguration;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@Service
public class RemoveRemoteVoltageRegulationInFranceService {

    private final DataFixConfiguration dataFixConfiguration;
    private final Logger businessLogger;

    public RemoveRemoteVoltageRegulationInFranceService(DataFixConfiguration dataFixConfiguration, Logger businessLogger) {
        this.dataFixConfiguration = dataFixConfiguration;
        this.businessLogger = businessLogger;
    }

    public void removeRemoteVoltageRegulationInFrance(Network network) {
        if (!dataFixConfiguration.removeRemoteVoltageRegulationInFrance()) {
            return;
        }
        businessLogger.info("Removing remote voltage regulations in France.");
        network.getGeneratorStream()
                .filter(RemoveRemoteVoltageRegulationInFranceService::isRemoteVoltageRegulationOn)
                .filter(RemoveRemoteVoltageRegulationInFranceService::isInFrance)
                .forEach(this::removeRemoteVoltageRegulation);
    }

    private void removeRemoteVoltageRegulation(Generator generator) {
        double oldVoltageTargetInPu = generator.getTargetV() / generator.getRegulatingTerminal().getVoltageLevel().getNominalV();
        double newVoltageTarget = oldVoltageTargetInPu * generator.getTerminal().getVoltageLevel().getNominalV();
        generator.setTargetV(newVoltageTarget);
        generator.setRegulatingTerminal(generator.getTerminal());
        businessLogger.info("Deactivating remote voltage regulation of generator '{}'. Local target set to {} kV", generator.getId(), newVoltageTarget);
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

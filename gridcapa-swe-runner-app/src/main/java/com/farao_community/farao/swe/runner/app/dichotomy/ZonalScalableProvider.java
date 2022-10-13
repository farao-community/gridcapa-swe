/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.dichotomy;

import com.farao_community.farao.swe.runner.api.resource.ProcessType;
import com.farao_community.farao.swe.runner.app.services.FileImporter;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Network;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.OffsetDateTime;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@Service
public class ZonalScalableProvider {
    private final FileImporter fileImporter;

    public ZonalScalableProvider(FileImporter fileImporter) {
        this.fileImporter = fileImporter;
    }

    public ZonalData<Scalable> get(String glskUrl, Network network, ProcessType processType, OffsetDateTime timestamp) throws IOException {
        ZonalData<Scalable> zonalScalable = fileImporter.importGlsk(glskUrl, network, timestamp.toInstant());
        //todo check scalable for FR
        return zonalScalable;
    }
}


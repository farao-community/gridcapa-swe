/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.gridcapa_swe_commons.resource.ProcessType;
import com.farao_community.farao.gridcapa_swe_commons.shift.NetworkExporter;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.powsybl.iidm.network.Network;

import java.time.OffsetDateTime;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
public class SweNetworkExporter implements NetworkExporter {
    private static final String XIIDM_EXTENSION = "xiidm";

    private final SweData sweData;
    private final FileExporter fileExporter;

    public SweNetworkExporter(final SweData sweData, final FileExporter fileExporter) {
        this.sweData = sweData;
        this.fileExporter = fileExporter;
    }

    @Override
    public void export(final Network network) {
        final OffsetDateTime targetProcessDateTime = sweData.getTimestamp();
        final ProcessType processType = sweData.getProcessType();
        final String destinationMinioPath = fileExporter.makeDestinationMinioPath(targetProcessDateTime, FileExporter.FileKind.ARTIFACTS);
        final String networkFilename = network.getNameOrId() + "-with-failed-balancing-adjustment." + XIIDM_EXTENSION;
        fileExporter.saveNetworkInArtifact(network,
                destinationMinioPath + networkFilename,
                "",
                targetProcessDateTime,
                processType);
    }
}

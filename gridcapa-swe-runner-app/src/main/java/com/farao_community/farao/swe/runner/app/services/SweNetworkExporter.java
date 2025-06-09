/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.gridcapa_swe_commons.resource.ProcessType;
import com.farao_community.farao.gridcapa_swe_commons.shift.NetworkExporter;
import com.farao_community.farao.swe.runner.api.resource.SweRequest;
import com.powsybl.iidm.network.Network;

import java.time.OffsetDateTime;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
public class SweNetworkExporter implements NetworkExporter {
    private static final String XIIDM_EXTENSION = "xiidm";

    private final SweRequest sweRequest;
    private final FileExporter fileExporter;

    public SweNetworkExporter(final SweRequest sweRequest, final FileExporter fileExporter) {
        this.sweRequest = sweRequest;
        this.fileExporter = fileExporter;
    }

    @Override
    public void export(final Network network) {
        final OffsetDateTime targetProcessDateTime = sweRequest.getTargetProcessDateTime();
        final ProcessType processType = sweRequest.getProcessType();
        final String destinationMinioPath = fileExporter.makeDestinationMinioPath(targetProcessDateTime, FileExporter.FileKind.ARTIFACTS);
        final String networkFilename = network.getNameOrId() + "-with-failed-balancing-adjustment." + XIIDM_EXTENSION;
        fileExporter.saveNetworkInArtifact(network,
                destinationMinioPath + networkFilename,
                "",
                targetProcessDateTime,
                processType);
    }
}

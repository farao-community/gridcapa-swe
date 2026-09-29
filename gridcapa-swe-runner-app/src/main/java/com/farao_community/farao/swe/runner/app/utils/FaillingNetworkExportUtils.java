/*
/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.utils;

import com.farao_community.farao.gridcapa_swe_commons.resource.ProcessType;
import com.farao_community.farao.swe.runner.app.services.FileExporter;
import com.powsybl.iidm.network.Network;

import java.time.OffsetDateTime;

/**
 * Utility methods for exporting an XIIDM snapshot of a network that triggered a failure condition. It centralizes the
 * artifact naming convention used when voltage checks or balancing adjustments want to keep the failing state.
 *
 * @see FileExporter performs the actual artifact upload
 * @see com.farao_community.farao.swe.runner.app.services.SweNetworkExporter adapts this helper to the shared network-export API
 * @see com.farao_community.farao.swe.runner.app.services.VoltageCheckService uses this helper when a voltage violation must be exported
 */
public final class FaillingNetworkExportUtils {
    private static final String XIIDM_EXTENSION = "xiidm";

    private FaillingNetworkExportUtils() {

    }

    public static void exportNetwork(Network network,
                                     OffsetDateTime targetProcessDateTime,
                                     ProcessType processType,
                                     FileExporter fileExporter,
                                     String filenameSuffix) {
        final String destinationMinioPath = fileExporter.makeDestinationMinioPath(targetProcessDateTime, FileExporter.FileKind.ARTIFACTS);
        final String filename = network.getNameOrId() + filenameSuffix + "." + XIIDM_EXTENSION;
        fileExporter.saveNetworkInArtifact(network, destinationMinioPath + filename, "", targetProcessDateTime, processType);
    }
}

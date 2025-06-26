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

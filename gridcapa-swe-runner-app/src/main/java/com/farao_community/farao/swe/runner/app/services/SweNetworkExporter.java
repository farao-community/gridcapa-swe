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
import com.farao_community.farao.swe.runner.app.utils.FaillingNetworkExportUtils;
import com.powsybl.iidm.network.Network;

import java.time.OffsetDateTime;

/**
 * Adapter from the shared SWE shifting API to this application's artifact-export mechanism. When the balancing phase
 * asks for a failing network export, this implementation stores the network snapshot with the SWE naming convention.
 *
 * @see FaillingNetworkExportUtils performs the actual artifact naming and export
 * @see FileExporter uploads the exported XIIDM snapshot
 * @see SweData provides the process timestamp and type associated with the current run
 *
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
public class SweNetworkExporter implements NetworkExporter {

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
        FaillingNetworkExportUtils.exportNetwork(network,
                targetProcessDateTime,
                processType,
                fileExporter,
                "-with-failed-balancing-adjustment");
    }
}

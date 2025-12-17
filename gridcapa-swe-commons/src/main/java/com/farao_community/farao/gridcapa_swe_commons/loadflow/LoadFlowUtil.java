/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_swe_commons.loadflow;

import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationConfig;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.loadflow.LoadFlowRunParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executor;

public final class LoadFlowUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoadFlowUtil.class);

    private LoadFlowUtil() {
        // No instantiation needed
    }

    public static LoadFlowResult runLoadFlowWithMdc(final Network network,
                                                    final String workingVariantId,
                                                    final LoadFlowParameters loadFlowParameters) {
        final ComputationManager computationManager = LoadFlowUtil.getMdcCompliantComputationManager();
        final LoadFlowRunParameters runParameters = new LoadFlowRunParameters()
            .setComputationManager(computationManager)
            .setParameters(loadFlowParameters);
        return LoadFlow.run(network, workingVariantId, runParameters);
    }

    public static ComputationManager getMdcCompliantComputationManager() {
        try {
            final Map<String, String> mdc = MDC.getCopyOfContextMap();

            return new LocalComputationManager(LocalComputationConfig.load()) {
                @Override
                public Executor getExecutor() {
                    final Executor delegate = super.getExecutor();
                    return command ->
                            delegate.execute(() -> {
                                final Map<String, String> originalMdc = MDC.getCopyOfContextMap();
                                try {
                                    // Set executor thread's MDC to the one of the calling thread or clear executor thread's MDC to avoid reusing MDC from a previous task
                                    if (mdc != null) {
                                        MDC.setContextMap(mdc);
                                    } else {
                                        MDC.clear();
                                    }
                                    command.run();
                                } finally {
                                    // Restore executor thread's MDC to its previous value or clear it to ensure new tasks won't reuse an old context
                                    if (originalMdc != null) {
                                        MDC.setContextMap(originalMdc);
                                    } else {
                                        MDC.clear();
                                    }
                                }
                            });
                }
            };
        } catch (IOException e) {
            LOGGER.error("Failed to build custom LocalComputationManager", e);
            return getDefaultComputationManager();
        }
    }

    public static ComputationManager getDefaultComputationManager() {
        return LocalComputationManager.getDefault();
    }
}

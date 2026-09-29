/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.swe.runner.app.parallelization;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

/**
 * Creates Fork/Join worker threads with the application context class loader propagated to each worker. This avoids
 * class-loading issues when parallel tasks rely on libraries discovered through the current thread context.
 *
 * @see java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory SPI implemented by this factory
 * @see java.util.concurrent.ForkJoinWorkerThread worker type customized here
 *
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */

public class SweForkJoinWorkerThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {

    @Override
    public final ForkJoinWorkerThread newThread(ForkJoinPool pool) {
        return new SweForkJoinWorkerThread(pool);
    }

    private static final class SweForkJoinWorkerThread extends ForkJoinWorkerThread {

        private SweForkJoinWorkerThread(final ForkJoinPool pool) {
            super(pool);
            // set the correct classloader here
            setContextClassLoader(Thread.currentThread().getContextClassLoader());
        }
    }
}

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
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */

public class SweForkJoinWorkerThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {
    @Override
    public final ForkJoinWorkerThread newThread(ForkJoinPool pool) {
        return new SweForkJoinWorkerThread(pool);
    }

    private final static class SweForkJoinWorkerThread extends ForkJoinWorkerThread {

        private SweForkJoinWorkerThread(final ForkJoinPool pool) {
            super(pool);
            // set the correct classloader here
            setContextClassLoader(Thread.currentThread().getContextClassLoader());
        }
    }
}

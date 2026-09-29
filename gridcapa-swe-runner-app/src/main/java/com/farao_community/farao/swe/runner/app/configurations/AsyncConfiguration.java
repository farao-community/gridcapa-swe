/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.swe.runner.app.configurations;

import com.farao_community.farao.swe.runner.app.parallelization.SweForkJoinWorkerThreadFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Declares the asynchronous executor used by the SWE runner to process per-direction computations in parallel.
 * The {@code threadPoolTaskExecutor} bean is consumed by
 * {@link com.farao_community.farao.swe.runner.app.parallelization.DichotomyParallelizationWorker} through
 * {@link org.springframework.scheduling.annotation.Async @Async}, and uses
 * {@link com.farao_community.farao.swe.runner.app.parallelization.SweForkJoinWorkerThreadFactory} to create
 * runner-specific worker threads.
 *
 * <p>The pool parallelism is sized to {@code min(32767, availableProcessors())}, so execution scales with the host
 * CPU count while staying within {@link java.util.concurrent.ForkJoinPool} limits.</p>
 *
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */

@Configuration
@EnableAsync
public class AsyncConfiguration  {

    @Bean(name = "threadPoolTaskExecutor")
    public Executor asyncExecutor() {
        SweForkJoinWorkerThreadFactory factory = new SweForkJoinWorkerThreadFactory();
        return new ForkJoinPool(Math.min(32767, Runtime.getRuntime().availableProcessors()), factory, null, false);
    }

}

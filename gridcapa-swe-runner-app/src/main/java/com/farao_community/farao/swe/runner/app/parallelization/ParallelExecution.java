/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.parallelization;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
public final class ParallelExecution<V> {

    private final Stream<CompletableFuture<V>> futureStream;
    private static SweForkJoinWorkerThreadFactory factory = new SweForkJoinWorkerThreadFactory();
    private static ForkJoinPool sweCommonPool = new ForkJoinPool(Math.min(32767, Runtime.getRuntime().availableProcessors()), factory, null, false);

    private ParallelExecution(final CompletableFuture<V> completableFuture) {
        this(Stream.of(completableFuture));
    }

    private ParallelExecution(final Stream<CompletableFuture<V>> futureStream) {
        this.futureStream = futureStream;
    }

    public static <V> ParallelExecution<V> of(final Supplier<V> supplier) {
        return new ParallelExecution<>(CompletableFuture.supplyAsync(supplier, sweCommonPool));
    }

    public static <V> ParallelExecution<V>  of(final Supplier<V> supplier, final BiFunction<V, Throwable, V> errorHandler) {
        return new ParallelExecution<>(CompletableFuture.supplyAsync(supplier, sweCommonPool).handle(errorHandler));
    }

    public ParallelExecution<V> and(final Supplier<V> supplier) {
        final Stream<CompletableFuture<V>> stream = Stream.of(CompletableFuture.supplyAsync(supplier, sweCommonPool));
        return new ParallelExecution<>(Stream.concat(futureStream, stream));
    }

    public ParallelExecution<V> and(final Supplier<V> supplier, final BiFunction<V, Throwable, V> errorHandler) {
        final Stream<CompletableFuture<V>> stream = Stream.of(CompletableFuture.supplyAsync(supplier, sweCommonPool).handle(errorHandler));
        return new ParallelExecution<>(Stream.concat(futureStream, stream));
    }

    public ExecutionResult<V> close() {
        return new ExecutionResult<>(futureStream
                .map(CompletableFuture<V>::join)
                .collect(Collectors.toList()));
    }
}

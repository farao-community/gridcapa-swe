/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.parallelization;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
public final class ParallelExecution {

    private final Stream<CompletableFuture<?>> futureStream;

    private ParallelExecution(final CompletableFuture<?> completableFuture) {
        this(Stream.of(completableFuture));
    }

    private ParallelExecution(final Stream<CompletableFuture<?>> futureStream) {
        this.futureStream = futureStream;
    }

    public static <V> ParallelExecution of(final Supplier<V> supplier) {
        return new ParallelExecution(CompletableFuture.supplyAsync(supplier));
    }

    public static <V> ParallelExecution of(final Supplier<V> supplier, final BiFunction<V, Throwable, V> errorHandler) {
        return new ParallelExecution(CompletableFuture.supplyAsync(supplier).handle(errorHandler));
    }

    public <T> ParallelExecution and(final Supplier<T> supplier) {
        final Stream<CompletableFuture<?>> stream = Stream.of(CompletableFuture.supplyAsync(supplier));
        return new ParallelExecution(Stream.concat(futureStream, stream));
    }

    public <T> ParallelExecution and(final Supplier<T> supplier, final BiFunction<T, Throwable, T> errorHandler) {
        final Stream<CompletableFuture<?>> stream = Stream.of(CompletableFuture.supplyAsync(supplier).handle(errorHandler));
        return new ParallelExecution(Stream.concat(futureStream, stream));
    }

    public ExecutionResult close() {
        return new ExecutionResult(futureStream
                .map(CompletableFuture::join)
                .collect(Collectors.toList()));
    }
}
/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_swe_commons.loadflow;

import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationConfig;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

class LoadFlowUtilTest {
    @BeforeEach
    void setUp() {
        MDC.clear();
    }

    private static void executeAndWait(final ComputationManager computationManager, final AtomicReference<String> value) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        computationManager.getExecutor().execute(() -> {
                value.set(MDC.get("testKey"));
                latch.countDown();
            }
        );
        Assertions.assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void emptyMdcValueWithMdcCompliantComputationManager() throws InterruptedException {
        final ComputationManager computationManager = LoadFlowUtil.getMdcCompliantComputationManager();
        final AtomicReference<String> value = new AtomicReference<>();
        executeAndWait(computationManager, value);
        Assertions.assertThat(value.get()).isNull();
    }

    @Test
    void mdcValueWithMdcCompliantComputationManager() throws InterruptedException {
        MDC.put("testKey", "testValue");
        final ComputationManager computationManager = LoadFlowUtil.getMdcCompliantComputationManager();
        final AtomicReference<String> value = new AtomicReference<>();
        executeAndWait(computationManager, value);
        Assertions.assertThat(value.get()).isEqualTo("testValue");
    }

    @Test
    void emptyMdcValueWithDefaultComputationManager() throws InterruptedException {
        final ComputationManager computationManager = LoadFlowUtil.getDefaultComputationManager();
        final AtomicReference<String> value = new AtomicReference<>();
        executeAndWait(computationManager, value);
        Assertions.assertThat(value.get()).isNull();
    }

    @Test
    void mdcValueWithDefaultComputationManager() throws InterruptedException {
        MDC.put("testKey", "testValue");
        final ComputationManager computationManager = LoadFlowUtil.getDefaultComputationManager();
        final AtomicReference<String> value = new AtomicReference<>();
        executeAndWait(computationManager, value);
        Assertions.assertThat(value.get()).isNull();
    }

    @Test
    void defaultComputationManagerInCaseOfException() {
        try (final MockedStatic<Files> filesMockedStatic = Mockito.mockStatic(Files.class);
             final MockedStatic<LoggerFactory> loggerFactoryMockedStatic = Mockito.mockStatic(LoggerFactory.class);
             final MockedStatic<LocalComputationConfig> localComputationConfigMockedStatic = Mockito.mockStatic(LocalComputationConfig.class)) {

            filesMockedStatic.when(() -> Files.createDirectories(Mockito.any()))
                    .thenThrow(IOException.class)
                    .thenReturn(null);
            loggerFactoryMockedStatic.when(() -> LoggerFactory.getLogger(Mockito.any(Class.class)))
                    .thenReturn(Mockito.mock(Logger.class));
            localComputationConfigMockedStatic.when(LocalComputationConfig::load)
                    .thenReturn(Mockito.mock(LocalComputationConfig.class));

            final ComputationManager computationManager = LoadFlowUtil.getMdcCompliantComputationManager();
            Assertions.assertThat(computationManager.getExecutor()).isEqualTo(ForkJoinPool.commonPool());
        }
    }
}

/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.utils;

import com.farao_community.farao.swe.runner.api.exception.SweInternalException;
import com.farao_community.farao.swe.runner.api.resource.ThreadLauncherResult;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@SpringBootTest
class GenericThreadLauncherTest {

    private static class LaunchWithoutThreadableAnnotation {

        public Integer run(int steps) {
            int result = 1;
            for (int i = 1; i < steps; i++) {
                result *= i * i;
            }
            return result;
        }

    }

    private static class LaunchWithMultipleThreadableAnnotation {

        @Threadable
        public Integer run(int steps) {
            int result = 1;
            for (int i = 1; i < steps; i++) {
                result *= i * i;
            }
            return result;
        }

        @Threadable
        public Integer run2(int steps) {
            int result = 1;
            for (int i = 1; i < steps; i++) {
                result += i * i;
            }
            return result;
        }

    }

    private static class LaunchWithThreadableAnnotationThrowsException {

        @Threadable
        public Integer run(int steps) {
            throw new SweInternalException("throwing exception");
        }

    }

    private static class LaunchWithThreadableAnnotationThrowsInterruptionException {

        @Threadable
        public Integer run(int steps) {
            throw new SweInternalException("throwing from interrupt", new InterruptedException());
        }

    }

    private static class LaunchWithThreadableAnnotation {

        @Threadable
        public Integer run(int steps) {
            int result = 1;
            for (int i = 1; i < steps; i++) {
                result *= i;
            }
            return result;
        }

    }

    @Test
    void launchGenericThread() {
        GenericThreadLauncher<LaunchWithThreadableAnnotation, Integer> gtl = new GenericThreadLauncher<>(
                new LaunchWithThreadableAnnotation(),
                "withThreadable",
                10);

        gtl.start();
        Optional<Thread> th = Thread.getAllStackTraces()
                .keySet()
                .stream()
                .filter(t -> t.getName().equals("withThreadable"))
                .findFirst();
        assertTrue(th.isPresent());
        ThreadLauncherResult<Integer> result = gtl.getResult();

        assertTrue(result.getResult().isPresent());
        assertEquals(362880, result.getResult().get());
    }

    @Test
    void testNotAnnotatedClass() {
        int exception = 0;
        try {
            GenericThreadLauncher<LaunchWithoutThreadableAnnotation, Integer> gtl = new GenericThreadLauncher<>(
                    new LaunchWithoutThreadableAnnotation(),
                "withThreadable",
                10);
        } catch (Exception e) {
            exception++;
            assertEquals(e.getClass(), SweInternalException.class);
            assertEquals("the class com.farao_community.farao.swe.runner.app.utils.GenericThreadLauncherTest.LaunchWithoutThreadableAnnotation does not have his running method annotated with @Threadable", e.getMessage());
        }
        assertEquals(1, exception);

    }

    @Test
    void testMultipleAnnotatedClass() {
        int exception = 0;
        try {
            GenericThreadLauncher<LaunchWithMultipleThreadableAnnotation, Integer> gtl = new GenericThreadLauncher<>(
                    new LaunchWithMultipleThreadableAnnotation(),
                    "withThreadable",
                    10);
        } catch (Exception e) {
            exception++;
            assertEquals(e.getClass(), SweInternalException.class);
            assertEquals("the class com.farao_community.farao.swe.runner.app.utils.GenericThreadLauncherTest.LaunchWithMultipleThreadableAnnotation must have only one method annotated with @Threadable", e.getMessage());
        }
        assertEquals(1, exception);
    }

    @Test
    void launchGenericThreadThrowsInterruptedException() {
        GenericThreadLauncher<LaunchWithThreadableAnnotationThrowsInterruptionException, Integer> gtl = new GenericThreadLauncher<>(
                new LaunchWithThreadableAnnotationThrowsInterruptionException(),
                "withThreadable",
                10);

        gtl.start();
        ThreadLauncherResult<Integer> result = gtl.getResult();

        assertTrue(result.getResult().isEmpty());
        assertFalse(result.hasError());
        assertNull(result.getException());
    }

    @Test
    void launchGenericThreadThrowsException() {
        GenericThreadLauncher<LaunchWithThreadableAnnotationThrowsException, Integer> gtl = new GenericThreadLauncher<>(
                new LaunchWithThreadableAnnotationThrowsException(),
                "withThreadable",
                10);

        gtl.start();
        ThreadLauncherResult<Integer> result = gtl.getResult();

        assertTrue(result.getResult().isEmpty());
        assertTrue(result.hasError());
        assertNotNull(result.getException());
    }

}

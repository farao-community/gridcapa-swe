/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@SpringBootTest
class InterruptionServiceTest {

    @Autowired
    InterruptionService interruptionService;

    private class MyThread extends Thread {

        public MyThread(String id) {
            super(id);
        }

        @Override
        public void run() {
            int count = 0;
            for (int i = 0; i < 10; i++) {
                count += i;
                await().atMost(i, SECONDS);
            }
        }
    }

    @Test
    void threadInterruption() {
        MyThread th = new MyThread("myThread");
        assertEquals(false,  isRunning("myThread").isPresent());

        th.start();
        assertEquals(true,  isRunning("myThread").isPresent());

        interruptionService.interruption("myThread");
        assertEquals(false,  isRunning("myThread").isPresent());

    }

    private Optional<Thread> isRunning(String id) {
        return Thread.getAllStackTraces()
                .keySet()
                .stream()
                .filter(t -> t.getName().equals(id))
                .findFirst();
    }

}

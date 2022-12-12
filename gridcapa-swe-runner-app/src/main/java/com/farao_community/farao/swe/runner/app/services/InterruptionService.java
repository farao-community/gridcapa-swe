/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.swe.runner.app.services;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Service
public class InterruptionService {

    public void interruption(String taskId) {
        List<Thread> threads = isRunning(taskId);
        while (threads != null && !threads.isEmpty()) {
            threads.stream().forEach(Thread::interrupt);
            threads = isRunning(taskId);
        }
    }

    private List<Thread> isRunning(String id) {
        return Thread.getAllStackTraces()
                .keySet()
                .stream()
                .filter(t -> t.getName().equals(id))
                .collect(Collectors.toUnmodifiableList());
    }
}

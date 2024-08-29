/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatusUpdate;
import com.farao_community.farao.gridcapa_swe_commons.exception.AbstractSweException;
import com.farao_community.farao.gridcapa_swe_commons.exception.SweInternalException;
import com.farao_community.farao.swe.runner.api.JsonApiConverter;
import com.farao_community.farao.swe.runner.api.resource.SweRequest;
import com.farao_community.farao.swe.runner.api.resource.SweResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@Service
public class RequestService {
    private static final String TASK_STATUS_UPDATE = "task-status-update";
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestService.class);
    private final SweRunner sweRunner;
    private final Logger businessLogger;
    private final JsonApiConverter jsonApiConverter = new JsonApiConverter();
    private final StreamBridge streamBridge;

    public RequestService(SweRunner sweRunner, Logger businessLogger, StreamBridge streamBridge) {
        this.sweRunner = sweRunner;
        this.businessLogger = businessLogger;
        this.streamBridge = streamBridge;
    }

    @Bean
    public Consumer<Flux<byte[]>> request() {
        return sweRequestFlux -> sweRequestFlux
                .doOnNext(this::launchSweRequest)
                .subscribe();
    }

    protected void launchSweRequest(byte[] req) {
        SweRequest sweRequest = jsonApiConverter.fromJsonMessage(req, SweRequest.class);
        final String sweRequestId = sweRequest.getId();
        // propagate in logs MDC the task id as an extra field to be able to match microservices logs with calculation tasks.
        // This should be done only once, as soon as the information to add in mdc is available.
        MDC.put("gridcapa-task-id", sweRequestId);
        try {
            sendTaskStatusUpdate(sweRequestId, TaskStatus.RUNNING);
            LOGGER.info("Swe request received : {}", sweRequest);
            SweResponse sweResponse = sweRunner.run(sweRequest);
            sendSweResponse(sweResponse);
            LOGGER.info("Swe response sent: {}", sweResponse);
        } catch (Exception e) {
            handleError(e, sweRequestId);
        }
    }

    private void sendSweResponse(SweResponse sweResponse) {
        if (sweResponse.isInterrupted()) {
            businessLogger.warn("SWE run has been interrupted");
            sendTaskStatusUpdate(sweResponse.getId(), TaskStatus.INTERRUPTED);
        } else {
            sendTaskStatusUpdate(sweResponse.getId(), TaskStatus.SUCCESS);
        }
    }

    private void handleError(Exception e, String requestId) {
        AbstractSweException sweException = new SweInternalException("SWE run failed", e);
        LOGGER.error(sweException.getDetails(), sweException);
        businessLogger.error(sweException.getDetails());
        sendTaskStatusUpdate(requestId, TaskStatus.ERROR);
    }

    private void sendTaskStatusUpdate(String requestId,
                                      TaskStatus targetStatus) {
        streamBridge.send(TASK_STATUS_UPDATE, new TaskStatusUpdate(UUID.fromString(requestId), targetStatus));
    }

}

/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatusUpdate;
import com.farao_community.farao.swe.runner.api.JsonApiConverter;
import com.farao_community.farao.swe.runner.api.exception.AbstractSweException;
import com.farao_community.farao.swe.runner.api.exception.SweInternalException;
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
import java.util.function.Function;

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
    public Function<Flux<byte[]>, Flux<byte[]>> request() {
        return sweRequestFlux -> sweRequestFlux
            .map(this::launchSweRequest)
            .log();
    }

    protected byte[] launchSweRequest(byte[] req) {
        byte[] result;
        SweRequest sweRequest = jsonApiConverter.fromJsonMessage(req, SweRequest.class);
        // propagate in logs MDC the task id as an extra field to be able to match microservices logs with calculation tasks.
        // This should be done only once, as soon as the information to add in mdc is available.
        final String sweRequestId = sweRequest.getId();
        MDC.put("gridcapa-task-id", sweRequestId);
        try {
            streamBridge.send(TASK_STATUS_UPDATE, new TaskStatusUpdate(UUID.fromString(sweRequest.getId()), TaskStatus.RUNNING));
            LOGGER.info("Swe request received : {}", sweRequest);
            SweResponse sweResponse = sweRunner.run(sweRequest);
            result = sendSweResponse(sweResponse);
            LOGGER.info("Swe response sent: {}", sweResponse);
        } catch (Exception e) {
            result = handleError(e, sweRequestId);
        }
        return result;
    }

    private byte[] sendSweResponse(SweResponse sweResponse) {
        if (sweResponse.isInterrupted()) {
            businessLogger.info("SWE run has been interrupted");
            streamBridge.send(TASK_STATUS_UPDATE, new TaskStatusUpdate(UUID.fromString(sweResponse.getId()), TaskStatus.INTERRUPTED));
        } else {
            streamBridge.send(TASK_STATUS_UPDATE, new TaskStatusUpdate(UUID.fromString(sweResponse.getId()), TaskStatus.SUCCESS));
        }
        return jsonApiConverter.toJsonMessage(sweResponse, SweResponse.class);
    }

    private byte[] handleError(Exception e, String requestId) {
        AbstractSweException sweException = new SweInternalException("SWE run failed", e);
        LOGGER.error(sweException.getDetails(), sweException);
        businessLogger.error(sweException.getDetails());
        return sendErrorResponse(requestId, sweException);
    }

    private byte[] sendErrorResponse(String requestId, AbstractSweException exception) {
        streamBridge.send(TASK_STATUS_UPDATE, new TaskStatusUpdate(UUID.fromString(requestId), TaskStatus.ERROR));
        return exceptionToJsonMessage(exception);
    }

    private byte[] exceptionToJsonMessage(AbstractSweException e) {
        return jsonApiConverter.toJsonMessage(e);
    }
}

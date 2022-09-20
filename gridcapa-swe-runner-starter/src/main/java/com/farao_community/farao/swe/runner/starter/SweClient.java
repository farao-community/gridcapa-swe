/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.starter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
public class SweClient {
    private static final int DEFAULT_PRIORITY = 1;
    private static final Logger LOGGER = LoggerFactory.getLogger(SweClient.class);

    private final AmqpTemplate amqpTemplate;
    private final SweClientProperties sweClientProperties;
    private final SweMessageHandler sweMessageHandler;

    public SweClient(AmqpTemplate amqpTemplate, SweClientProperties sweClientProperties) {
        this.amqpTemplate = amqpTemplate;
        this.sweClientProperties = sweClientProperties;
        this.sweMessageHandler = new SweMessageHandler(sweClientProperties);
    }

    public <I, J> J run(I request, Class<I> requestClass, Class<J> responseClass, int priority) {
        LOGGER.info("Request sent: {}", request);
        LOGGER.info("Request sent to : {}", sweClientProperties.getBinding().getDestination());
        LOGGER.info("Request sent to : {}", sweClientProperties.getBinding().getRoutingKey());
        Message responseMessage = amqpTemplate.sendAndReceive(
                sweClientProperties.getBinding().getDestination(),
                sweClientProperties.getBinding().getRoutingKey(),
                sweMessageHandler.buildMessage(request, requestClass, priority)
        );
        J response = sweMessageHandler.readMessage(responseMessage, responseClass);
        LOGGER.info("Response received: {}", response);
        return response;
    }

    public <I, J> J run(I request, Class<I> requestClass, Class<J> responseClass) {
        return run(request, requestClass, responseClass, DEFAULT_PRIORITY);
    }
}

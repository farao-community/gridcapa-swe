/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.starter;

import com.farao_community.farao.gridcapa_swe_commons.exception.SweInternalException;
import com.farao_community.farao.swe.runner.api.JsonApiConverter;
import org.springframework.amqp.core.*;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
public class SweMessageHandler {
    private static final String CONTENT_ENCODING = "UTF-8";
    private static final String CONTENT_TYPE = "application/vnd.api+json";

    private final SweClientProperties sweClientProperties;
    private final JsonApiConverter jsonConverter;

    public SweMessageHandler(SweClientProperties sweClientProperties) {
        this.sweClientProperties = sweClientProperties;
        this.jsonConverter = new JsonApiConverter();
    }

    public <I> Message buildMessage(I request, Class<I> requestClass, int priority) {
        return MessageBuilder.withBody(jsonConverter.toJsonMessage(request, requestClass))
                .andProperties(buildMessageProperties(priority))
                .build();
    }

    private MessageProperties buildMessageProperties(int priority) {
        return MessagePropertiesBuilder.newInstance()
                .setAppId(sweClientProperties.getBinding().getApplicationId())
                .setContentEncoding(CONTENT_ENCODING)
                .setContentType(CONTENT_TYPE)
                .setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT)
                .setExpiration(sweClientProperties.getBinding().getExpiration())
                .setPriority(priority)
                .build();
    }

    public <J> J readMessage(Message message, Class<J> clazz) {
        if (message != null) {
            return jsonConverter.fromJsonMessage(message.getBody(), clazz);
        } else {
            throw new SweInternalException("Swe server did not respond");
        }
    }
}

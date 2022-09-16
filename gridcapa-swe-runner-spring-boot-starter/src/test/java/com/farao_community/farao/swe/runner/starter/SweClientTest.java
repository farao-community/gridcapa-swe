/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.starter;

import com.farao_community.farao.swe.api.JsonApiConverter;
import com.farao_community.farao.swe.api.resource.SweRequest;
import com.farao_community.farao.swe.api.resource.SweResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;

import java.io.IOException;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
public class SweClientTest {
    private final JsonApiConverter jsonApiConverter = new JsonApiConverter();

    @Test
    void checkThatSweClientHandlesSweImportMessagesCorrectly() throws IOException {
        AmqpTemplate amqpTemplate = Mockito.mock(AmqpTemplate.class);
        SweClient sweClient = new SweClient(amqpTemplate, buildProperties());
        SweRequest sweRequest = jsonApiConverter.fromJsonMessage(getClass().getResourceAsStream("/sweRequestMessage.json").readAllBytes(), SweRequest.class);
        Message responseMessage = Mockito.mock(Message.class);

        Mockito.when(responseMessage.getBody()).thenReturn(getClass().getResourceAsStream("/sweResponseMessage.json").readAllBytes());
        Mockito.when(amqpTemplate.sendAndReceive(Mockito.same("my-exchange"), Mockito.same("#"), Mockito.any())).thenReturn(responseMessage);
        SweResponse sweResponse = sweClient.run(sweRequest, SweRequest.class, SweResponse.class);

    }

    private SweClientProperties buildProperties() {
        SweClientProperties properties = new SweClientProperties();
        SweClientProperties.BindingConfiguration bindingConfiguration = new SweClientProperties.BindingConfiguration();
        bindingConfiguration.setDestination("my-exchange");
        bindingConfiguration.setRoutingKey("#");
        bindingConfiguration.setExpiration("60000");
        bindingConfiguration.setApplicationId("application-id");
        properties.setBinding(bindingConfiguration);
        return properties;
    }
}

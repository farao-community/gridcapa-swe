/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
import com.farao_community.farao.swe.runner.api.JsonApiConverter;
import com.farao_community.farao.swe.runner.api.resource.SweRequest;
import com.farao_community.farao.swe.runner.api.resource.SweResponse;
import com.farao_community.farao.swe.runner.starter.SweClient;
import com.farao_community.farao.swe.runner.starter.SweClientProperties;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
class SweClientTest {
    private final JsonApiConverter jsonApiConverter = new JsonApiConverter();

    @Test
    void checkThatSweClientHandlesMessagesCorrectly() throws IOException {
        AmqpTemplate amqpTemplate = Mockito.mock(AmqpTemplate.class);
        SweClient sweClient = new SweClient(amqpTemplate, buildProperties());
        SweRequest sweRequest = jsonApiConverter.fromJsonMessage(getClass().getResourceAsStream("/sweRequestMessage.json").readAllBytes(), SweRequest.class);
        Message responseMessage = Mockito.mock(Message.class);

        Mockito.when(responseMessage.getBody()).thenReturn(getClass().getResourceAsStream("/sweResponseMessage.json").readAllBytes());
        Mockito.when(amqpTemplate.sendAndReceive(Mockito.same("my-exchange"), Mockito.same("#"), Mockito.any())).thenReturn(responseMessage);
        SweResponse sweResponse = sweClient.run(sweRequest, SweRequest.class, SweResponse.class);

        assertEquals("id", sweResponse.getId());
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

/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.function.StreamBridge;

@SpringBootTest
class RequestServiceTest {

    @MockBean
    private SweRunner sweRunner;

    @MockBean
    private StreamBridge streamBridge;

    @Autowired
    private RequestService requestService;

  /*  @Test
    void testRequestService() {
        String id = UUID.randomUUID().toString();
        SweRequest sweRequest = new SweRequest(id, OffsetDateTime.now(), null);
        JsonApiConverter jsonApiConverter = new JsonApiConverter();
        SweResponse sweResponse = new SweResponse(sweRequest.getId());
        byte[] req = jsonApiConverter.toJsonMessage(sweRequest, SweRequest.class);
        byte[] resp = jsonApiConverter.toJsonMessage(sweResponse, SweResponse.class);
        when(sweRunner.run(any())).thenReturn(sweResponse);
        when(streamBridge.send(any(), any())).thenReturn(true);
        byte[] result = requestService.launchSweRequest(req);
        assertArrayEquals(resp, result);
    }
*/
}

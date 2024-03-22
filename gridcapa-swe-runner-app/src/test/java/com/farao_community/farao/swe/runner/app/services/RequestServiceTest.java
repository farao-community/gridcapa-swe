/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatusUpdate;
import com.farao_community.farao.gridcapa_swe_commons.exception.SweInternalException;
import com.farao_community.farao.swe.runner.api.JsonApiConverter;
import com.farao_community.farao.swe.runner.api.resource.SweRequest;
import com.farao_community.farao.swe.runner.api.resource.SweResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.function.StreamBridge;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@SpringBootTest
class RequestServiceTest {

    @Autowired
    private RequestService requestService;

    @MockBean
    private SweRunner sweRunner;

    @MockBean
    private StreamBridge streamBridge;

    private final JsonApiConverter jsonApiConverter = new JsonApiConverter();

    @Test
    void testRequestService() {
        String id = UUID.randomUUID().toString();
        SweRequest cseRequest = new SweRequest(id, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        SweResponse cseResponse = new SweResponse(cseRequest.getId(), "null", false);
        byte[] req = jsonApiConverter.toJsonMessage(cseRequest, SweRequest.class);
        byte[] resp = jsonApiConverter.toJsonMessage(cseResponse, SweResponse.class);
        when(sweRunner.run(any())).thenReturn(cseResponse);

        byte[] result = requestService.launchSweRequest(req);

        ArgumentCaptor<TaskStatusUpdate> captor = ArgumentCaptor.forClass(TaskStatusUpdate.class);
        verify(streamBridge, times(2)).send(any(), captor.capture());
        assertEquals(TaskStatus.RUNNING, captor.getAllValues().get(0).getTaskStatus());
        assertEquals(TaskStatus.SUCCESS, captor.getAllValues().get(1).getTaskStatus());

        assertArrayEquals(resp, result);
    }

    @Test
    void testInterruptedRequestService() {
        String id = UUID.randomUUID().toString();
        SweRequest cseRequest = new SweRequest(id, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        SweResponse cseResponse = new SweResponse(cseRequest.getId(), "null", true);
        byte[] req = jsonApiConverter.toJsonMessage(cseRequest, SweRequest.class);
        byte[] resp = jsonApiConverter.toJsonMessage(cseResponse, SweResponse.class);
        when(sweRunner.run(any())).thenReturn(cseResponse);

        byte[] result = requestService.launchSweRequest(req);

        ArgumentCaptor<TaskStatusUpdate> captor = ArgumentCaptor.forClass(TaskStatusUpdate.class);
        verify(streamBridge, times(2)).send(any(), captor.capture());
        assertEquals(TaskStatus.RUNNING, captor.getAllValues().get(0).getTaskStatus());
        assertEquals(TaskStatus.INTERRUPTED, captor.getAllValues().get(1).getTaskStatus());

        assertArrayEquals(resp, result);
    }

    @Test
    void testErrorRequestService() {
        String id = UUID.randomUUID().toString();
        RuntimeException except = new RuntimeException("Mocked exception");
        SweRequest cseRequest = new SweRequest(id, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        byte[] req = jsonApiConverter.toJsonMessage(cseRequest, SweRequest.class);
        when(sweRunner.run(any())).thenThrow(except);
        byte[] expectedResult = jsonApiConverter.toJsonMessage(new SweInternalException("SWE run failed", except));

        byte[] result = requestService.launchSweRequest(req);

        ArgumentCaptor<TaskStatusUpdate> captor = ArgumentCaptor.forClass(TaskStatusUpdate.class);
        verify(streamBridge, times(2)).send(any(), captor.capture());
        assertEquals(TaskStatus.RUNNING, captor.getAllValues().get(0).getTaskStatus());
        assertEquals(TaskStatus.ERROR, captor.getAllValues().get(1).getTaskStatus());

        assertArrayEquals(expectedResult, result);
    }
}

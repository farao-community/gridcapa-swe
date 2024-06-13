/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileStatus;
import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import com.farao_community.farao.swe.runner.app.exception.FtpClientAdapterException;
import com.farao_community.farao.swe.runner.app.utils.FtpClientAdapter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;

import java.io.ByteArrayInputStream;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author Mohamed Benrejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */
@Service
public class FtpFileExportService {

    public static final String TASKS_PATH = "/tasks/";
    private final RestTemplate restTemplate;
    private final FtpClientAdapter ftpClientAdapter;
    private final Logger businessLogger;
    private static final Logger LOGGER = LoggerFactory.getLogger(FtpFileExportService.class);

    @Value("${task-manager.base-url}")
    private String taskManagerBaseUrl;
    @Value("${task-manager.fetch-task.retries-number}")
    private int fetchTaskRetriesNumber;
    @Value("${task-manager.fetch-task.interval-in-seconds}")
    private int fetchTaskIntervalInSeconds;

    public FtpFileExportService(RestTemplate restTemplate, FtpClientAdapter ftpClientAdapter, Logger businessLogger) {
        this.restTemplate = restTemplate;
        this.ftpClientAdapter = ftpClientAdapter;
        this.businessLogger = businessLogger;
    }

    @Bean
    public Consumer<Flux<TaskDto>> consumeTaskDtoUpdate() {
        return f -> f
                .onErrorContinue((t, r) -> LOGGER.error(t.getMessage(), t))
                .subscribe(this::exportOutputsForTask);
    }

    void exportOutputsForTask(TaskDto taskDto) {
        try {
            MDC.put("gridcapa-task-id", taskDto.getId().toString());
            boolean isTaskFinished = taskDto.getStatus().equals(TaskStatus.SUCCESS) || taskDto.getStatus().equals(TaskStatus.ERROR);
            if (isTaskFinished) {
                LOGGER.info("Received a task status {} event for timestamp: {}, trying to export result within the configured interval.", taskDto.getStatus(), taskDto.getTimestamp());
                TaskDto taskDtoUpdated = fetchOutputsAvailable(taskDto);
                exportValidatedOutputsAndLog(taskDtoUpdated);
            }
        } catch (Exception e) {
            //this exeption block avoids gridcapa export from deconnecting from spring cloud stream !
            LOGGER.error(e.getMessage(), e);
        }
    }

    private void exportValidatedOutputsAndLog(TaskDto taskDto) {
        businessLogger.info("Task status {}, exporting results for timestamp: {}", taskDto.getStatus(), taskDto.getTimestamp());
        taskDto.getOutputs().stream().filter(processFileDto -> processFileDto.getProcessFileStatus().equals(ProcessFileStatus.VALIDATED))
                .forEach(processFileDto -> {
                    ResponseEntity<byte[]> responseEntity = getResponseEntityByFileType(taskDto.getTimestamp(), processFileDto.getFileType());
                    uploadToFtpFromResponseEntity(responseEntity, mustUnzip(processFileDto.getFileType()));
                });
        ResponseEntity<byte[]> responseEntity = getResponseEntityByFileType(taskDto.getTimestamp(), "LOGS");
        uploadToFtpFromResponseEntity(responseEntity, false);
    }

    private boolean mustUnzip(String fileType) {
        return StringUtils.startsWith(fileType, "CGM_");
    }

    private void uploadToFtpFromResponseEntity(ResponseEntity<byte[]> responseEntity, boolean unzip) {
        String fileOutputName = getFileNameFromResponseEntity(responseEntity);
        try {
            LOGGER.info("Uploading file {} to ftp", fileOutputName);
            ftpClientAdapter.upload(fileOutputName, unzip, new ByteArrayInputStream(Objects.requireNonNull(responseEntity.getBody())));
        } catch (FtpClientAdapterException e) {
            businessLogger.error("Exception occurred while uploading generated results to server, details: {}", e.getMessage());
        }
    }

    /**
     * Sometimes the files are not validated immediately with task status update, we retry to fetch task
     */
    private TaskDto fetchOutputsAvailable(TaskDto taskDto) {
        LOGGER.info("Received a task status {} event for timestamp: {}, trying to fetch result within the configured interval.", taskDto.getStatus(), taskDto.getTimestamp());
        TaskDto updatedTaskDto;
        boolean allOutputsAvailable = checkAllOutputFileValidated(taskDto);
        int retryCounter = 0;
        do {
            try {
                TimeUnit.SECONDS.sleep(fetchTaskIntervalInSeconds);
                LOGGER.info("Fetching outputs for iteration number {}", retryCounter);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.error("Couldn't interrupt thread : {}", e.getMessage());
            }
            updatedTaskDto = getUpdatedTaskForTimestamp(taskDto.getTimestamp());
            if (updatedTaskDto != null) {
                allOutputsAvailable = checkAllOutputFileValidated(updatedTaskDto);
            }
            retryCounter++;
        } while (retryCounter < fetchTaskRetriesNumber && !allOutputsAvailable);
        return updatedTaskDto != null ? updatedTaskDto : taskDto;
    }

    ResponseEntity<byte[]> getResponseEntityByFileType(OffsetDateTime timestamp, String fileType) {
        String outputsRestLocation = UriComponentsBuilder.fromHttpUrl(taskManagerBaseUrl + TASKS_PATH + timestamp + "/file/" + fileType).toUriString();
        return restTemplate.getForEntity(outputsRestLocation, byte[].class);
    }

    String getFileNameFromResponseEntity(ResponseEntity<byte[]> responseEntity) {
        String rawFileName = Optional.ofNullable(responseEntity.getHeaders().get("Content-Disposition")).map(at -> at.get(0)).orElse("outputs.zip");
        // filename coming from response entity header is formatted with double-quotes such as "filename="---real_filename---""
        String fileNameHeaderIdentifier = "filename=";
        return rawFileName.substring(rawFileName.lastIndexOf(fileNameHeaderIdentifier) + fileNameHeaderIdentifier.length() + 1, rawFileName.length() - 1);
    }

    private boolean checkAllOutputFileValidated(TaskDto taskDtoUpdated) {
        return taskDtoUpdated.getOutputs().stream().allMatch(output -> output.getProcessFileStatus().equals(ProcessFileStatus.VALIDATED));
    }

    private TaskDto getUpdatedTaskForTimestamp(OffsetDateTime timestamp) {
        String restLocation = UriComponentsBuilder.fromHttpUrl(taskManagerBaseUrl + TASKS_PATH + timestamp).toUriString();
        return restTemplate.getForEntity(restLocation, TaskDto.class).getBody();
    }
}

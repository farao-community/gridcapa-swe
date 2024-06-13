/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.configurations;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Mohamed Benrejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@Configuration
@ConfigurationProperties("ftp")
public class FtpConfiguration {
    private String host;
    private int port;
    private String accessKey;
    private String secretKey;
    private String remoteRelativeDestinationDirectory;
    private int retryCount;
    private int retrySleep;

    public void setRetrySleep(int retrySleep) {
        this.retrySleep = retrySleep;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public void setRemoteRelativeDestinationDirectory(String remoteRelativeDestinationDirectory) {
        this.remoteRelativeDestinationDirectory = remoteRelativeDestinationDirectory;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getRemoteRelativeDestinationDirectory() {
        return remoteRelativeDestinationDirectory;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public int getRetrySleep() {
        return retrySleep;
    }
}

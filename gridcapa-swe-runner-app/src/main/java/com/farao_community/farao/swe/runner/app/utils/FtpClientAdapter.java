/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.utils;

import com.farao_community.farao.swe.runner.app.configurations.FtpConfiguration;
import com.farao_community.farao.swe.runner.app.exception.FtpClientAdapterException;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 * @author Marc Schwitzguebel {@literal <marc.schwitzguebel at rte-france.com>}
 */

@Component
public class FtpClientAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(FtpClientAdapter.class);
    public static final String ATTEMPT_TO_COPY_FILE_TO_FTP_SERVER = "Attempt to copy {} file to FTP server";

    private final FtpConfiguration ftpConfiguration;

    public FtpClientAdapter(FtpConfiguration ftpConfiguration) {
        this.ftpConfiguration = ftpConfiguration;
    }

    public void upload(String fileName, boolean unzip, InputStream inputStream) throws FtpClientAdapterException {
        int performedRetries = 0;
        final int maxRetryCount = ftpConfiguration.getRetryCount();
        final int retrySleep = ftpConfiguration.getRetrySleep();
        boolean successfulFtpSend = false;
        while (performedRetries <= maxRetryCount && !successfulFtpSend) {
            try {
                TimeUnit.SECONDS.sleep((long) performedRetries * retrySleep);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            performedRetries++;
            successfulFtpSend = performSingleUploadAttempt(fileName, unzip, inputStream);
        }
        if (!successfulFtpSend) {
            throw new FtpClientAdapterException(String.format("Upload of file %s failed after %d retries", fileName, maxRetryCount));
        }
    }

    private boolean performSingleUploadAttempt(String fileName, boolean unzip, InputStream inputStream) {
        boolean successFlag = false;
        try {
            FTPClient ftp = new FTPClient(); // NOSONAR
            LOGGER.info("Attempt to connect to FTP server");
            ftp.connect(ftpConfiguration.getHost(), ftpConfiguration.getPort());

            int reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                return false;
            }
            ftp.login(ftpConfiguration.getAccessKey(), ftpConfiguration.getSecretKey());
            LOGGER.info("Connection established");

            // if ftp working dir is /home/farao/upload and you want to upload files under /home/farao/upload/outputs, then the remote relative destination dir should be 'outputs', FTPClient will append it itself
            ftp.changeWorkingDirectory(ftpConfiguration.getRemoteRelativeDestinationDirectory());
            ftp.enterLocalPassiveMode();
            ftp.setFileType(FTP.BINARY_FILE_TYPE);  // required because ASCII is the default file type, otherwise zip will be corrupted
            if (unzip) {
                final String directory = fileName.replace(".zip", "");
                ftp.makeDirectory(directory);
                ftp.changeWorkingDirectory(directory);
                try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
                    ZipEntry zipEntry = zipInputStream.getNextEntry();
                    successFlag = true;
                    while (zipEntry != null) {
                        final String zippedFileName = zipEntry.getName();
                        LOGGER.info(ATTEMPT_TO_COPY_FILE_TO_FTP_SERVER, zippedFileName);
                        successFlag  = ftp.storeFile(zippedFileName, zipInputStream) && successFlag;
                        logSuccces(successFlag, zippedFileName);
                        zipEntry = zipInputStream.getNextEntry();
                    }
                }
            } else {
                LOGGER.info(ATTEMPT_TO_COPY_FILE_TO_FTP_SERVER, fileName);
                successFlag = ftp.storeFile(fileName, inputStream);
                logSuccces(successFlag, fileName);
            }
            ftp.disconnect();
            LOGGER.info("Connection closed");
            return successFlag;
        } catch (IOException e) {
            LOGGER.error("Fail during upload", e);
            return successFlag;
        }
    }

    private static void logSuccces(boolean isSuccessful, String fileName) {
        if (isSuccessful) {
            LOGGER.info("File {} copied successfully to FTP server", fileName);
        } else {
            LOGGER.error("File {} couldn't be copied successfully to FTP server", fileName);
        }
    }
}

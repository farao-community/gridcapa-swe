/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.utils;

import com.farao_community.farao.swe.runner.app.exception.FtpClientAdapterException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author Mohamed Benrejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */

@SpringBootTest
class FtpClientAdapterTest {

    private FakeFtpServer fakeFtpServer;

    @Autowired
    private FtpClientAdapter ftpClientAdapter;

    @Test
    void checkFileTransferredToRemoteDestination() throws FtpClientAdapterException {
        fakeFtpServer = new FakeFtpServer();
        fakeFtpServer.addUserAccount(new UserAccount("user", "password", "/data"));
        FileSystem fileSystem = new UnixFakeFileSystem();
        fileSystem.add(new DirectoryEntry("/data/swe/d2cc/outputs"));
        fakeFtpServer.setFileSystem(fileSystem);
        fakeFtpServer.setServerControlPort(3030);
        fakeFtpServer.start();
        ftpClientAdapter.upload("test.txt", false, new ByteArrayInputStream("test content".getBytes(StandardCharsets.UTF_8)));
        Assertions.assertTrue(fakeFtpServer.getFileSystem().exists("/data/swe/d2cc/outputs/test.txt"));
        fakeFtpServer.stop();
    }

    @Test
    void checkZippedFileTransferredToRemoteDestination() throws FtpClientAdapterException {
        fakeFtpServer = new FakeFtpServer();
        fakeFtpServer.addUserAccount(new UserAccount("user", "password", "/data"));
        FileSystem fileSystem = new UnixFakeFileSystem();
        fileSystem.add(new DirectoryEntry("/data/swe/d2cc/outputs"));
        fakeFtpServer.setFileSystem(fileSystem);
        fakeFtpServer.setServerControlPort(3030);
        fakeFtpServer.start();
        ftpClientAdapter.upload("CGM_MicroGrid.zip", true, getClass().getResourceAsStream("CGM_MicroGrid.zip"));
        Assertions.assertTrue(fakeFtpServer.getFileSystem().exists("/data/swe/d2cc/outputs/CGM_MicroGrid"));
        Assertions.assertTrue(fakeFtpServer.getFileSystem().isDirectory("/data/swe/d2cc/outputs/CGM_MicroGrid"));
        Assertions.assertTrue(fakeFtpServer.getFileSystem().exists("/data/swe/d2cc/outputs/CGM_MicroGrid/20210209T1930Z_1D_ASSEMBLED_DL_9.zip"));
        Assertions.assertTrue(fakeFtpServer.getFileSystem().exists("/data/swe/d2cc/outputs/CGM_MicroGrid/20210209T1930Z_1D_ASSEMBLED_SV_9.zip"));
        Assertions.assertTrue(fakeFtpServer.getFileSystem().exists("/data/swe/d2cc/outputs/CGM_MicroGrid/20210209T1930Z_1D_BE_EQ_9.zip"));
        Assertions.assertTrue(fakeFtpServer.getFileSystem().exists("/data/swe/d2cc/outputs/CGM_MicroGrid/20210209T1930Z_1D_BE_GL_9.zip"));
        fakeFtpServer.stop();
    }

    @Test
    void checkFileTransferredToRemoteDestinationKo() {
        Assertions.assertThrows(FtpClientAdapterException.class, () -> ftpClientAdapter.upload("test.txt", false, new ByteArrayInputStream("test content".getBytes(StandardCharsets.UTF_8))));
    }
}

/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.swe.runner.api.exception.SweInvalidDataException;
import com.farao_community.farao.swe.runner.api.resource.SweFileResource;
import com.farao_community.farao.swe.runner.api.resource.SweRequest;
import com.farao_community.farao.swe.runner.app.domain.MergingViewData;
import com.powsybl.iidm.mergingview.MergingView;
import com.powsybl.iidm.network.Network;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Service
public class MergingViewService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MergingViewService.class);

    private final NetworkService networkService;

    public MergingViewService(NetworkService networkService) {
        this.networkService = networkService;
    }

    public MergingViewData importMergingView(SweRequest sweRequest) {
        Network networkFr = networkService.importFromZip(buildZipFile(sweRequest, Country.FR));
        Network networkEs = networkService.importFromZip(buildZipFile(sweRequest, Country.ES));
        Network networkPt = networkService.importFromZip(buildZipFile(sweRequest, Country.PT));

        MergingView mergingView = MergingView.create("MergingViewService", "iidm");
        mergingView.merge(networkFr, networkEs, networkPt);
        return new MergingViewData(networkFr, networkEs, networkPt, mergingView);
    }

    private String buildZipFile(SweRequest sweRequest, Country country) {
        List<SweFileResource> listFiles = getFiles(sweRequest, country);
        return buildZipFromListOfFiles(listFiles, country);
    }

    private List<SweFileResource> getFiles(SweRequest sweRequest, Country country) {
        List<SweFileResource> listFiles = new ArrayList<>();
        listFiles.add(sweRequest.getCoresoSv());
        listFiles.add(sweRequest.getBoundaryEq());
        listFiles.add(sweRequest.getBoundaryTp());

        if (country.equals(Country.FR)) {
            listFiles.add(sweRequest.getRteEq());
            listFiles.add(sweRequest.getRteSsh());
            listFiles.add(sweRequest.getRteTp());
        } else if (country.equals(Country.ES)) {
            listFiles.add(sweRequest.getReeEq());
            listFiles.add(sweRequest.getReeSsh());
            listFiles.add(sweRequest.getReeTp());
        } else if (country.equals(Country.PT)) {
            listFiles.add(sweRequest.getRenEq());
            listFiles.add(sweRequest.getRenSsh());
            listFiles.add(sweRequest.getRenTp());
        }

        return listFiles;
    }

    private String buildZipFromListOfFiles(List<SweFileResource> listFiles, Country country) {
        try {
            Path tmp = Files.createTempDirectory("pref_");
            byte[] buffer = new byte[1024];
            String zipPath = tmp.toAbsolutePath() + "/network_" + country.toString() + ".zip";
            FileOutputStream fos = new FileOutputStream(zipPath);
            ZipOutputStream zos = new ZipOutputStream(fos);

            for (SweFileResource file : listFiles) {
                InputStream inputStream = new URL(file.getUrl()).openStream();
                File srcFile = new File(tmp.toAbsolutePath() + File.separator + file.getFilename());
                FileUtils.copyInputStreamToFile(inputStream, srcFile);
                FileInputStream fis = new FileInputStream(srcFile);
                zos.putNextEntry(new ZipEntry(srcFile.getName()));
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }
                zos.closeEntry();
                fis.close();
                deleteFile(srcFile);
            }
            zos.close();
            return zipPath;
        } catch (IOException ioe) {
            throw new SweInvalidDataException("Error creating netowrk zip file: " + ioe);
        }
    }

    private void deleteFile(File srcFile) {
        try {
            Files.delete(srcFile.toPath());
        } catch (IOException e) {
            LOGGER.warn("Temporary file could not be deleted, check for full storage error");
        }
    }

    enum Country {
        FR,
        ES,
        PT
    }
}

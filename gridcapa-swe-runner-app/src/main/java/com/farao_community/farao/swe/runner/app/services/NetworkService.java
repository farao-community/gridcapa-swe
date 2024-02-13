/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import com.farao_community.farao.gridcapa_swe_commons.hvdc.SweHvdcPreprocessor;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.gridcapa_swe_commons.exception.SweInternalException;
import com.farao_community.farao.gridcapa_swe_commons.exception.SweInvalidDataException;
import com.farao_community.farao.swe.runner.api.resource.SweFileResource;
import com.farao_community.farao.swe.runner.api.resource.SweRequest;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.google.common.base.Suppliers;
import com.powsybl.cgmes.conversion.CgmesImport;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.ImportConfig;
import com.powsybl.iidm.network.Network;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@Service
public class NetworkService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkService.class);
    public static final String PST_1 = "_e071a1d4-fef5-1bd9-5278-d195c5597b6e";
    public static final String PST_2 = "_7824bc48-fc86-51db-8f9c-01b44933839e";

    private final MinioAdapter minioAdapter;

    private final DateTimeFormatter networkFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm_'network.xiidm'");
    private final Logger businessLogger;

    static final Map<Country, String> TSO_BY_COUNTRY = Map.of(Country.FR, "RTEFRANCE", Country.ES, "REE", Country.PT, "REN");

    public NetworkService(MinioAdapter minioAdapter, Logger businessLogger) {
        this.minioAdapter = minioAdapter;
        this.businessLogger = businessLogger;
    }

    public Network loadNetworkFromMinio(OffsetDateTime targetDateTime) {
        String fileName = networkFormatter.format(targetDateTime);
        try (InputStream xiidm = minioAdapter.getFile("XIIDM/" + fileName)) {
            return Network.read(fileName, xiidm);
        } catch (IOException e) {
            throw new SweInternalException("Could not load network from XIIDM file", e);
        }
    }

    public Network importMergedNetwork(SweRequest sweRequest) {
        try {
            businessLogger.info("Start import of input CGMES files");
            String zipPath = buildZipFile(sweRequest);
            Network mergedNetwork = importFromZip(zipPath);
            Files.deleteIfExists(Path.of(zipPath));
            return mergedNetwork;
        } catch (Exception e) {
            throw new SweInternalException("Exception occurred during input CGM import", e);
        }
    }

    private String buildZipFile(SweRequest sweRequest) {
        List<SweFileResource> listFiles = getFiles(sweRequest);
        return buildZipFromListOfFiles(listFiles);
    }

    public void addHvdcAndPstToNetwork(Network network) {
        addhvdc(network);
        addPst(network);
    }

    private List<SweFileResource> getFiles(SweRequest sweRequest) {
        List<SweFileResource> listFiles = new ArrayList<>();
        listFiles.add(sweRequest.getCoresoSv());
        listFiles.add(sweRequest.getBoundaryEq());
        listFiles.add(sweRequest.getBoundaryTp());
        listFiles.add(sweRequest.getRteEq());
        listFiles.add(sweRequest.getRteSsh());
        listFiles.add(sweRequest.getRteTp());
        listFiles.add(sweRequest.getReeEq());
        listFiles.add(sweRequest.getReeSsh());
        listFiles.add(sweRequest.getReeTp());
        listFiles.add(sweRequest.getRenEq());
        listFiles.add(sweRequest.getRenSsh());
        listFiles.add(sweRequest.getRenTp());
        return listFiles;
    }

    Network importFromZip(String zipPath) {
        Properties importParams = new Properties();
        importParams.put(CgmesImport.SOURCE_FOR_IIDM_ID, CgmesImport.SOURCE_FOR_IIDM_ID_RDFID);
        return Network.read(Paths.get(zipPath), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);
    }

    private void deleteFile(File srcFile) {
        try {
            Files.delete(srcFile.toPath());
        } catch (IOException e) {
            LOGGER.warn("Temporary file could not be deleted, check for full storage error");
        }
    }

    private void addhvdc(Network network) {
        SweHvdcPreprocessor sweHvdcPreprocessor = new SweHvdcPreprocessor();
        sweHvdcPreprocessor.applyParametersToNetwork(getClass().getResourceAsStream("/hvdc/SwePreprocessorParameters.json"), network);
    }

    private void addPst(Network network) {
        try {
            network.getTwoWindingsTransformer(PST_1).getPhaseTapChanger().setRegulating(false);
            network.getTwoWindingsTransformer(PST_2).getPhaseTapChanger().setRegulating(false);
            businessLogger.info("Regulation mode of the PSTs modified");
        } catch (NullPointerException e) {
            businessLogger.warn("The PST mode could not be changed because it was not found");
        }
    }

    private String buildZipFromListOfFiles(List<SweFileResource> listFiles) {
        try {
            Path tmp = Files.createTempDirectory("pref_");
            byte[] buffer = new byte[1024];
            String zipPath = tmp.toAbsolutePath() + "/network.zip";
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
            throw new SweInvalidDataException("Error creating network zip file", ioe);
        }
    }

    public static Network getNetworkByDirection(SweData sweData, DichotomyDirection direction) {
        Network network = null;
        switch (direction) {
            case ES_FR:
                network = sweData.getNetworkEsFr();
                break;
            case ES_PT:
                network =  sweData.getNetworkEsPt();
                break;
            case FR_ES:
                network =  sweData.getNetworkFrEs();
                break;
            case PT_ES:
                network =  sweData.getNetworkPtEs();
                break;
        }
        return network;
    }
}

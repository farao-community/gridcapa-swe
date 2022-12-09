/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.swe.runner.api.exception.SweInternalException;
import com.farao_community.farao.swe.runner.api.exception.SweInvalidDataException;
import com.farao_community.farao.swe.runner.api.resource.SweFileResource;
import com.farao_community.farao.swe.runner.api.resource.SweRequest;
import com.farao_community.farao.swe.runner.app.hvdc.HvdcLinkProcessor;
import com.farao_community.farao.swe.runner.app.hvdc.parameters.HvdcCreationParameters;
import com.farao_community.farao.swe.runner.app.hvdc.parameters.SwePreprocessorParameters;
import com.farao_community.farao.swe.runner.app.hvdc.parameters.json.JsonSwePreprocessorImporter;
import com.google.common.base.Suppliers;
import com.powsybl.cgmes.conversion.CgmesImport;
import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.ImportConfig;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
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
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Service
public class NetworkService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkService.class);
    public static final String PST_1 = "_e071a1d4-fef5-1bd9-5278-d195c5597b6e";
    public static final String PST_2 = "_7824bc48-fc86-51db-8f9c-01b44933839e";

    private final MinioAdapter minioAdapter;

    private final DateTimeFormatter networkFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm_'network.xiidm'");
    private final Logger businessLogger;

    public NetworkService(MinioAdapter minioAdapter, Logger businessLogger) {
        this.minioAdapter = minioAdapter;
        this.businessLogger = businessLogger;
    }

    public Network loadNetworkFromMinio(OffsetDateTime targetDateTime) {
        String fileName = networkFormatter.format(targetDateTime);
        try (InputStream xiidm = minioAdapter.getFile("XIIDM/" + fileName)) {
            return Importers.loadNetwork(fileName, xiidm);
        } catch (IOException e) {
            throw new SweInternalException("Could not load network from XIIDM file");
        }
    }

    public Network importNetwork(SweRequest sweRequest) {
        LOGGER.info("Importing CGMES network");
        List<SweFileResource> listCgms = getCgmAndBoundaryFilesFromRequest(sweRequest);
        String zipPath = buildZipFromCgms(listCgms);
        Network network = importFromZip(zipPath);
        deleteFile(new File(zipPath));
        addHvdcAndPstToNetwork(network);
        exportToMinio(network, sweRequest.getTargetProcessDateTime());
        return network;
    }

    List<SweFileResource> getCgmAndBoundaryFilesFromRequest(SweRequest sweRequest) {
        List<SweFileResource> listCgms = new ArrayList<>();
        listCgms.add(sweRequest.getCoresoSv());
        listCgms.add(sweRequest.getReeEq());
        listCgms.add(sweRequest.getReeTp());
        listCgms.add(sweRequest.getReeSsh());
        listCgms.add(sweRequest.getRenEq());
        listCgms.add(sweRequest.getRenTp());
        listCgms.add(sweRequest.getRenSsh());
        listCgms.add(sweRequest.getRteEq());
        listCgms.add(sweRequest.getRteTp());
        listCgms.add(sweRequest.getRteSsh());
        listCgms.add(sweRequest.getBoundaryEq());
        listCgms.add(sweRequest.getBoundaryTp());
        return listCgms;
    }

    String buildZipFromCgms(List<SweFileResource> listCgmFiles) {
        try {
            Path tmp = Files.createTempDirectory("pref_");
            byte[] buffer = new byte[1024];
            String zipPath = tmp.toAbsolutePath() + "/networktmp.zip";
            FileOutputStream fos = new FileOutputStream(zipPath);
            ZipOutputStream zos = new ZipOutputStream(fos);

            for (SweFileResource file : listCgmFiles) {
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

    public void addHvdcAndPstToNetwork(Network network) {
        addhvdc(network);
        addPst(network);
    }

    void addhvdc(Network network) {
        SwePreprocessorParameters params = JsonSwePreprocessorImporter.read(getClass().getResourceAsStream("/hvdc/SwePreprocessorParameters.json"));
        HvdcLinkProcessor.replaceEquivalentModelByHvdc(network, params.getHvdcCreationParametersSet());
        List<String> hvdcIds = params.getHvdcCreationParametersSet().stream().map(HvdcCreationParameters::getId).collect(Collectors.toList());
        LOGGER.info("HVDC {} added to network", hvdcIds);
        businessLogger.info("HVDC {} added to network", hvdcIds);
    }

    private void addPst(Network network) {
        try {
            network.getTwoWindingsTransformer(PST_1).getPhaseTapChanger().setRegulationMode(PhaseTapChanger.RegulationMode.FIXED_TAP);
            network.getTwoWindingsTransformer(PST_2).getPhaseTapChanger().setRegulationMode(PhaseTapChanger.RegulationMode.FIXED_TAP);
            businessLogger.info("Regulation mode of the PSTs modified");
        } catch (NullPointerException e) {
            businessLogger.warn("The PST mode could not be changed because it was not found");
        }
    }

    private void exportToMinio(Network network, OffsetDateTime targetDateTime) {
        MemDataSource memDataSource = new MemDataSource();
        network.write("XIIDM", new Properties(), memDataSource);
        InputStream xiidm;
        try {
            xiidm = memDataSource.newInputStream("", "xiidm");
        } catch (IOException e) {
            throw new SweInternalException("Could not export XIIDM file");
        }
        minioAdapter.uploadArtifactForTimestamp("XIIDM/" + networkFormatter.format(targetDateTime), xiidm, "SWE", "", targetDateTime);
    }

}

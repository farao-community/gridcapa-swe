/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import com.farao_community.farao.gridcapa_swe_commons.exception.SweInvalidDataNoDetailsException;
import com.farao_community.farao.gridcapa_swe_commons.hvdc.SweHvdcPreprocessor;
import com.farao_community.farao.gridcapa_swe_commons.hvdc.parameters.HvdcCreationParameters;
import com.farao_community.farao.gridcapa_swe_commons.hvdc.parameters.SwePreprocessorParameters;
import com.farao_community.farao.gridcapa_swe_commons.hvdc.parameters.json.JsonSwePreprocessorImporter;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.gridcapa_swe_commons.exception.SweInternalException;
import com.farao_community.farao.gridcapa_swe_commons.exception.SweInvalidDataException;
import com.farao_community.farao.swe.runner.api.resource.SweFileResource;
import com.farao_community.farao.swe.runner.api.resource.SweRequest;
import com.farao_community.farao.swe.runner.app.configurations.PstConfiguration;
import com.farao_community.farao.gridcapa_swe_commons.hvdc.HvdcInformation;
import com.farao_community.farao.swe.runner.app.domain.SweData;
import com.google.common.base.Suppliers;
import com.powsybl.cgmes.conversion.CgmesImport;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.*;
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
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Service
public class NetworkService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkService.class);

    private final MinioAdapter minioAdapter;

    private final DateTimeFormatter networkFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm_'network.xiidm'");
    private final Logger businessLogger;
    private final PstConfiguration pstConfiguration;

    static final Map<Country, String> TSO_BY_COUNTRY = Map.of(Country.FR, "RTEFRANCE", Country.ES, "REE", Country.PT, "REN");

    public NetworkService(MinioAdapter minioAdapter, Logger businessLogger, PstConfiguration pstConfiguration) {
        this.minioAdapter = minioAdapter;
        this.businessLogger = businessLogger;
        this.pstConfiguration = pstConfiguration;
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
        disablePstRegulation(network);
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
        try {
            sweHvdcPreprocessor.applyParametersToNetwork(getClass().getResourceAsStream("/hvdc/SwePreprocessorParameters.json"), network);
        } catch (SweInvalidDataNoDetailsException e) {
            businessLogger.warn(e.getMessage());
        }
    }

    private void disablePstRegulation(Network network) {
        pstConfiguration.getPstIds().forEach(id -> {
            if (network.getIdentifiable(id) == null) {
                businessLogger.warn("PST with ID {} is not available in network. Cannot be put in fixed setpoint", id);
            } else {
                TwoWindingsTransformer twt = network.getTwoWindingsTransformer(id);
                if (twt == null || twt.getPhaseTapChanger() == null) {
                    businessLogger.error("Element with ID {} does not correspond to an actual PST. Cannot be put in fixed setpoint", id);
                } else {
                    twt.getPhaseTapChanger().setRegulating(false);
                    businessLogger.info("PST with ID {} has been set to a fixed setpoint", id);
                }
            }
        });
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

    List<HvdcInformation> getHvdcInformationFromNetwork(Network network) {
        List<HvdcInformation> hvdcInformationList = new ArrayList<>();
        SwePreprocessorParameters params = JsonSwePreprocessorImporter.read(getClass().getResourceAsStream("/hvdc/SwePreprocessorParameters.json"));

        List<HvdcCreationParameters> sortedHvdcCreationParameters = params.getHvdcCreationParametersSet().stream()
                .sorted(Comparator.comparing(HvdcCreationParameters::getId)).toList();

        for (HvdcCreationParameters parameter : sortedHvdcCreationParameters) {
            HvdcInformation hvdcInformation = new HvdcInformation(parameter.getId());
            Optional<Line> line = Optional.ofNullable(network.getLine(parameter.getEquivalentAcLineId()));
            Optional<Generator> genSide1 = Optional.ofNullable(network.getGenerator(parameter.getEquivalentGeneratorId(TwoSides.ONE)));
            Optional<Generator> genSide2 = Optional.ofNullable(network.getGenerator(parameter.getEquivalentGeneratorId(TwoSides.TWO)));
            Optional<Load> loadSide1 = Optional.ofNullable(network.getLoad(parameter.getEquivalentLoadId(TwoSides.ONE).get(1)));
            Optional<Load> loadSide2 = Optional.ofNullable(network.getLoad(parameter.getEquivalentLoadId(TwoSides.TWO).get(1)));

            line.ifPresent(line1 -> {
                hvdcInformation.setAcLineTerminal1Connected(line1.getTerminal1().isConnected());
                hvdcInformation.setAcLineTerminal2Connected(line1.getTerminal2().isConnected());
            });

            genSide1.ifPresent(generator -> {
                hvdcInformation.setSide1GeneratorConnected(generator.getTerminal().isConnected());
                hvdcInformation.setSide1GeneratorTargetP(generator.getTargetP());
            });
            genSide2.ifPresent(generator -> {
                hvdcInformation.setSide2GeneratorConnected(generator.getTerminal().isConnected());
                hvdcInformation.setSide2GeneratorTargetP(generator.getTargetP());
            });

            loadSide1.ifPresentOrElse(
                load -> {
                    hvdcInformation.setSide1LoadConnected(load.getTerminal().isConnected());
                    hvdcInformation.setSide1LoadP(load.getP0());
                },
                () -> Optional.ofNullable(network.getLoad(parameter.getEquivalentLoadId(TwoSides.ONE).get(2)))
                    .ifPresent(loadWithSecondOptionId -> {
                        hvdcInformation.setSide1LoadConnected(loadWithSecondOptionId.getTerminal().isConnected());
                        hvdcInformation.setSide1LoadP(loadWithSecondOptionId.getP0());
                    })
            );

            loadSide2.ifPresent(load -> {
                hvdcInformation.setSide2LoadConnected(load.getTerminal().isConnected());
                hvdcInformation.setSide2LoadP(load.getP0());
            });

            hvdcInformationList.add(hvdcInformation);
        }

        return hvdcInformationList;
    }
}

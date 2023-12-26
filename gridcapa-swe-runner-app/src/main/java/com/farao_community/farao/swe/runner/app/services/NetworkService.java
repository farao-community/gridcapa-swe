/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import com.farao_community.farao.gridcapa_swe_commons.hvdc.SweHvdcPreprocessor;
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
import java.util.ArrayList;
import java.util.Arrays;
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

    static final String ES_FR_VARIANT_ID = "EsFr_variant";
    static final String FR_ES_VARIANT_ID = "FrEs_variant";
    static final String ES_PT_VARIANT_ID = "EsPt_variant";
    static final String PT_ES_VARIANT_ID = "PtEs_variant";
    private final Logger businessLogger;

    static final Map<Country, String> TSO_BY_COUNTRY = Map.of(Country.FR, "RTE", Country.ES, "REE", Country.PT, "REN");

    public NetworkService(Logger businessLogger) {
        this.businessLogger = businessLogger;
    }

    public Network importMergedNetwork(SweRequest sweRequest) {
        try {
            businessLogger.info("Start import of input CGMES files");
            List<Network> networks = TSO_BY_COUNTRY.keySet().stream().map(country -> getNetworkForCountry(sweRequest, country)).toList();
            return Network.merge("network_merged", networks.toArray(new Network[0]));
        } catch (Exception e) {
            throw new SweInternalException("Exception occurred during input CGM import", e);
        }
    }

    private Network getNetworkForCountry(SweRequest sweRequest, Country country) {
        return importFromZip(buildZipFile(sweRequest, country));
    }

    public void addHvdcAndPstToNetwork(Network network) {
        addhvdc(network);
        addPst(network);
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
            network.getTwoWindingsTransformer(PST_1).getPhaseTapChanger().setRegulationMode(PhaseTapChanger.RegulationMode.FIXED_TAP);
            network.getTwoWindingsTransformer(PST_2).getPhaseTapChanger().setRegulationMode(PhaseTapChanger.RegulationMode.FIXED_TAP);
            businessLogger.info("Regulation mode of the PSTs modified");
        } catch (NullPointerException e) {
            businessLogger.warn("The PST mode could not be changed because it was not found");
        }
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
            throw new SweInvalidDataException("Error creating netowrk zip file", ioe);
        }
    }

    public void initClones(Network mergedNetwork) {
        mergedNetwork.getVariantManager().allowVariantMultiThreadAccess(true);
        mergedNetwork.getVariantManager().cloneVariant(mergedNetwork.getVariantManager().getWorkingVariantId(), Arrays.asList(ES_FR_VARIANT_ID, ES_PT_VARIANT_ID, FR_ES_VARIANT_ID, PT_ES_VARIANT_ID), true);
    }

    public static Network getNetworkByDirection(SweData sweData, DichotomyDirection direction) {
        switch (direction) {
            case ES_FR:
                sweData.getMergedNetwork().getVariantManager().setWorkingVariant(ES_FR_VARIANT_ID);
                break;
            case ES_PT:
                sweData.getMergedNetwork().getVariantManager().setWorkingVariant(ES_PT_VARIANT_ID);
                break;
            case FR_ES:
                sweData.getMergedNetwork().getVariantManager().setWorkingVariant(FR_ES_VARIANT_ID);
                break;
            case PT_ES:
                sweData.getMergedNetwork().getVariantManager().setWorkingVariant(PT_ES_VARIANT_ID);
                break;
        }
        return sweData.getMergedNetwork();
    }
}

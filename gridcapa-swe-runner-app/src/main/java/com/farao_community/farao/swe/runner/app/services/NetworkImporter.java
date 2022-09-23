/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.swe.runner.api.resource.SweFileResource;
import com.farao_community.farao.swe.runner.api.resource.SweRequest;
import com.farao_community.farao.swe.runner.app.hvdc.HvdcLinkProcessor;
import com.farao_community.farao.swe.runner.app.hvdc.parameters.SwePreprocessorParameters;
import com.farao_community.farao.swe.runner.app.hvdc.parameters.json.JsonSwePreprocessorImporter;
import com.google.common.base.Suppliers;
import com.powsybl.cgmes.conversion.CgmesImport;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.import_.ImportConfig;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Service
public class NetworkImporter {

    private final MinioAdapter minioAdapter;

    public NetworkImporter(MinioAdapter minioAdapter) {
        this.minioAdapter = minioAdapter;
    }

    public Network importNetwork(SweRequest sweRequest) {
        List<SweFileResource> listCgms = getCgmFilesFromRequest(sweRequest);
        String zipPath = buildZipFromCgms(listCgms);
        Network network = importFromZip(zipPath);
        addhvdc(network);
        addPst(network);
        return network;
    }

    private List<SweFileResource> getCgmFilesFromRequest(SweRequest sweRequest) {
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
        return listCgms;
    }

    private String buildZipFromCgms(List<SweFileResource> listCgmFiles) {
        try {
            Path tmp = Files.createTempDirectory(null);
            byte[] buffer = new byte[1024];
            String zipPath = tmp.toAbsolutePath() + "/network.zip";
            FileOutputStream fos = new FileOutputStream(zipPath);
            ZipOutputStream zos = new ZipOutputStream(fos);

            for (SweFileResource file : listCgmFiles) {
                InputStream inputStream = new URL(file.getUrl()).openStream();

                File srcFile = new File(tmp.toAbsolutePath() + "/" + file.getFilename());
                FileUtils.copyInputStreamToFile(inputStream, srcFile);
                FileInputStream fis = new FileInputStream(srcFile);
                zos.putNextEntry(new ZipEntry(srcFile.getName()));
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }

                zos.closeEntry();
                fis.close();
            }
            zos.close();
            return zipPath;
        } catch (IOException ioe) {
            System.out.println("Error creating zip file: " + ioe);
            return null;
        }
    }

    private Network importFromZip(String zipPath) {
        Properties importParams = new Properties();
        importParams.put(CgmesImport.SOURCE_FOR_IIDM_ID, CgmesImport.SOURCE_FOR_IIDM_ID_RDFID);
        Network network = Importers.loadNetwork(Paths.get(zipPath), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);
        deleteZip(zipPath);
        return network;
    }

    private static void deleteZip(String zipPath) {
        try {
            FileUtils.deleteDirectory(new File(zipPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addhvdc(Network network) {
        SwePreprocessorParameters params = JsonSwePreprocessorImporter.read(getClass().getResourceAsStream("/hvdc/SwePreprocessorParameters.json"));
        HvdcLinkProcessor.replaceEquivalentModelByHvdc(network, params.getHvdcCreationParametersSet());
    }

    public void addPst(Network network) {
        network.getTwoWindingsTransformer("_e071a1d4-fef5-1bd9-5278-d195c5597b6e").getPhaseTapChanger().setRegulationMode(PhaseTapChanger.RegulationMode.FIXED_TAP);
        network.getTwoWindingsTransformer("_7824bc48-fc86-51db-8f9c-01b44933839e").getPhaseTapChanger().setRegulationMode(PhaseTapChanger.RegulationMode.FIXED_TAP);
    }
}

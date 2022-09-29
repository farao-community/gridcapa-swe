package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.cim.CimCrac;
import com.google.common.base.Suppliers;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.import_.ImportConfig;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class FileImporterTest {

    @Autowired
    private FileImporter fileImporter;
    private final String testDirectory = "/20210401/";
    private final String cimCracFilename = "CIM_21_1_1.xml";
    private final String jsonCracFilename = "cracCimJson.json";
    private final String networkFileName = "MicroGrid.zip";
    private final OffsetDateTime dateTime = OffsetDateTime.parse("2021-04-01T22:00Z");

    @Test
    void testImportCimCrac() {
        CimCrac cimCrac = fileImporter.importCimCrac(
                getClass().getResource(testDirectory + cimCracFilename).toExternalForm()
                );
        Assertions.assertNotNull(cimCrac);
    }

    @Test
    void testImportCrac() {
        Properties importParams = new Properties();
        importParams.put("iidm.import.cgmes.source-for-iidm-id", "rdfID");
        Network network = Importers.loadNetwork(
                Paths.get(new File(getClass().getResource(testDirectory + networkFileName).getFile()).toString()),
                LocalComputationManager.getDefault(),
                Suppliers.memoize(ImportConfig::load).get(),
                importParams);
        CimCrac cimCrac = fileImporter.importCimCrac(
                getClass().getResource(testDirectory + cimCracFilename).toExternalForm()
        );
        Crac crac = fileImporter.importCrac(cimCrac, dateTime, network);
        Assertions.assertNotNull(crac);
    }

    @Test
    void testImportCimCracFromUrlWithNetwork() {
        Properties importParams = new Properties();
        importParams.put("iidm.import.cgmes.source-for-iidm-id", "rdfID");
        Network network = Importers.loadNetwork(
                Paths.get(new File(getClass().getResource(testDirectory + networkFileName).getFile()).toString()),
                LocalComputationManager.getDefault(),
                Suppliers.memoize(ImportConfig::load).get(),
                importParams
        );
        Crac crac = fileImporter.importCimCracFromUrlWithNetwork(
                getClass().getResource(testDirectory + cimCracFilename).toExternalForm(),
                dateTime,
                network
        );
        Assertions.assertNotNull(crac);
    }

    @Test
    void testImportCracFromJson() {
        Crac cracFromJson = fileImporter.importCracFromJson(Objects.requireNonNull(getClass().getResource(testDirectory + jsonCracFilename)).toString());
        assertNotNull(cracFromJson);
    }
}

package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.api.CracCreators;
import com.farao_community.farao.data.crac_creation.creator.cim.CimCrac;
import com.farao_community.farao.data.crac_creation.creator.cim.importer.CimCracImporter;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.farao_community.farao.swe.runner.api.exception.SweInvalidDataException;
import com.powsybl.iidm.network.Network;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.OffsetDateTime;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */
@Service
public class FileImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileImporter.class);
    private final UrlValidationService urlValidationService;

    public FileImporter(UrlValidationService urlValidationService) {
        this.urlValidationService = urlValidationService;
    }

    public CimCrac importCimCrac(String cracUrl) {
        LOGGER.info("Importing Cim Crac file from url");
        InputStream cracInputStream = urlValidationService.openUrlStream(cracUrl);
        CimCracImporter cimCracImporter = new CimCracImporter();
        return cimCracImporter.importNativeCrac(cracInputStream);
    }

    public Crac importCrac(CimCrac cimCrac, OffsetDateTime targetProcessDateTime, Network network) {
        LOGGER.info("Importing native Crac from Cim Crac and Network for process date: {}", targetProcessDateTime);
        return CracCreators.createCrac(cimCrac, network, targetProcessDateTime).getCrac();
    }

    public Crac importCimCracFromUrlWithNetwork(String cracUrl, OffsetDateTime targetProcessDateTime, Network network) {
        return importCrac(importCimCrac(cracUrl), targetProcessDateTime, network);
    }

    public Crac importCracFromJson(String cracUrl) {
        LOGGER.info("Importing Crac from json file url");
        try (InputStream cracResultStream = urlValidationService.openUrlStream(cracUrl)) {
            return CracImporters.importCrac(FilenameUtils.getName(new URL(cracUrl).getPath()), cracResultStream);
        } catch (IOException e) {
            throw new SweInvalidDataException(String.format("Cannot import crac from JSON : %s", cracUrl));
        }
    }
}

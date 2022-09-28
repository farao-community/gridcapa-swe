package com.farao_community.farao.swe.runner.app.services;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.api.CracCreators;
import com.farao_community.farao.data.crac_creation.creator.cim.CimCrac;
import com.farao_community.farao.data.crac_creation.creator.cim.importer.CimCracImporter;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.farao_community.farao.swe.runner.api.exception.SweInvalidDataException;
import com.powsybl.iidm.network.Network;
import org.apache.commons.io.FilenameUtils;
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

    private final UrlValidationService urlValidationService;

    public FileImporter(UrlValidationService urlValidationService) {
        this.urlValidationService = urlValidationService;
    }

    public CimCrac importCimCrac(String cracUrl) {
        InputStream cracInputStream = urlValidationService.openUrlStream(cracUrl);
        CimCracImporter cimCracImporter = new CimCracImporter();
        return cimCracImporter.importNativeCrac(cracInputStream);
    }

    public Crac importCrac(CimCrac cimCrac, OffsetDateTime targetProcessDateTime, Network network) {
        return CracCreators.createCrac(cimCrac, network, targetProcessDateTime).getCrac();
    }

    public Crac importCracFromJson(String cracUrl) {
        try (InputStream cracResultStream = urlValidationService.openUrlStream(cracUrl)) {
            return CracImporters.importCrac(FilenameUtils.getName(new URL(cracUrl).getPath()), cracResultStream);
        } catch (IOException e) {
            throw new SweInvalidDataException(String.format("Cannot import crac from JSON : %s", cracUrl));
        }
    }
}

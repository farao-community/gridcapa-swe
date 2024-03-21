/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_swe_commons.shift;

import com.farao_community.farao.gridcapa_swe_commons.diff_shift.DiffShiftedGenerator;
import com.farao_community.farao.gridcapa_swe_commons.diff_shift.ScalableInformation;
import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.glsk.api.GlskPoint;
import com.powsybl.glsk.api.GlskRegisteredResource;
import com.powsybl.glsk.cim.CimGlskDocumentImporter;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public class DiffShiftedNetworkTest {

    private static final String EIC_FR = "10YFR-RTE------C";
    private static final String EIC_ES = "10YES-REE------0";
    private static final String EIC_PT = "10YPT-REN------W";
    private static String glskFileName;
    private static String networkFileName1;
    private static String networkFileName2;
    private static OffsetDateTime timestamp;

    @BeforeAll
    static void setup() {
        timestamp = OffsetDateTime.parse("2023-07-31T07:30:00Z");
        // Network before shift
        networkFileName1 = "/shift/TestCase_with_transformers.xiidm";
        // Network shifted
        networkFileName2 = "/shift/TestCase_with_transformers_shift_es_fr_1000.xiidm";

        glskFileName = "/shift/TestCase_with_transformers_glsk.xml";
    }

    @Test
    void displayShiftDiffForSpainTest() {
        Network network1 = Network.read(networkFileName1, getClass().getResourceAsStream(networkFileName1));
        Network network2 = Network.read(networkFileName2, getClass().getResourceAsStream(networkFileName2));

        GlskDocument glskDocument = new CimGlskDocumentImporter().importGlsk(getClass().getResourceAsStream(glskFileName));

        List<ScalableInformation> scalableInformationsEs = getScalableInformation(glskDocument, EIC_ES, timestamp);
        List<ScalableInformation> scalableInformationsUp =  scalableInformationsEs.stream().filter(scalableInformation -> scalableInformation.getFlowDirectionType().equals(ScalableInformation.FlowDirectionType.UP)).collect(Collectors.toList());
        assertEquals(12, scalableInformationsUp.size());

        List<DiffShiftedGenerator> diffShiftedGeneratorsEsUp = scalableInformationsUp.stream().map(scalableInformation -> new DiffShiftedGenerator(scalableInformation, scalableInformation.getGeneratorInformation(network1), scalableInformation.getGeneratorInformation(network2))).collect(Collectors.toList());
        assertEquals(12, diffShiftedGeneratorsEsUp.size());
        System.out.println("List of all generators information in the scalable for SPAIN :");
        displayDiff(diffShiftedGeneratorsEsUp);

        List<DiffShiftedGenerator> diffGeneratorsWithDifferentTargetP = diffShiftedGeneratorsEsUp.stream().filter(DiffShiftedGenerator::hasDifferentTargetP).toList();
        assertEquals(5, diffGeneratorsWithDifferentTargetP.size());
        System.out.println("List of shifted generators information for SPAIN :");
        displayDiff(diffGeneratorsWithDifferentTargetP);

        List<DiffShiftedGenerator> diffGeneratorsWithDifferentConnectionStatus = diffShiftedGeneratorsEsUp.stream().filter(DiffShiftedGenerator::hasDifferentConnectionStatus).toList();
        assertEquals(4, diffGeneratorsWithDifferentConnectionStatus.size());
        System.out.println("List of generators that was connected or disconnected during the shift for SPAIN :");
        displayDiffWithTwt(diffGeneratorsWithDifferentConnectionStatus);
    }

    private static void displayDiffWithTwt(List<DiffShiftedGenerator> diffShiftedGenerators) {
        System.out.println("Direction;position;id;Pmin_glsk;Pmax_glsk;targetP_1;pMin_1;pMax_1;connected_1;mainComponentConnected_1;targetP_2;pMin_2;pMax_2;connected_2;mainComponentConnected_2;transformers_id");
        diffShiftedGenerators.forEach(diffShiftedGenerator -> System.out.println(diffShiftedGenerator.displayDiffGeneratorWithTwt()));
    }

    private static void displayDiff(List<DiffShiftedGenerator> diffShiftedGenerators) {
        System.out.println("Direction;position;id;Pmin_glsk;Pmax_glsk;targetP_1;pMin_1;pMax_1;connected_1;mainComponentConnected_1;targetP_2;pMin_2;pMax_2;connected_2;mainComponentConnected_2");
        diffShiftedGenerators.forEach(diffShiftedGenerator -> System.out.println(diffShiftedGenerator.displayDiffGenerator()));
    }

    private static List<ScalableInformation> getScalableInformation(GlskDocument glskDocument, String countryEic, OffsetDateTime timestamp) {
        List<GlskPoint> glskPointListEs = glskDocument.getGlskPoints(countryEic).stream()
                .filter(glskPoint -> glskPoint.getPointInterval().contains(timestamp.toInstant())).toList();
        List<ScalableInformation> scalableInformations = new ArrayList<>();
        glskPointListEs.forEach(glskPoint -> {
            glskPoint.getGlskShiftKeys().forEach(glskShiftKey -> {
                if (glskShiftKey.getFlowDirection().equals("A01")) {
                    GlskRegisteredResource glskRegisteredResource = glskShiftKey.getRegisteredResourceArrayList().get(0); //For Swe there is only one registredResource
                    scalableInformations.add(new ScalableInformation(glskRegisteredResource.getmRID(), glskShiftKey.getMeritOrderPosition(), glskRegisteredResource.getMinimumCapacity().get(), glskRegisteredResource.getMaximumCapacity().get(), ScalableInformation.FlowDirectionType.UP));
                } else {
                    GlskRegisteredResource glskRegisteredResource = glskShiftKey.getRegisteredResourceArrayList().get(0);
                    scalableInformations.add(new ScalableInformation(glskRegisteredResource.getmRID(), glskShiftKey.getMeritOrderPosition(), glskRegisteredResource.getMinimumCapacity().get(), glskRegisteredResource.getMaximumCapacity().get(), ScalableInformation.FlowDirectionType.DOWN));
                }
            });
        });
        return scalableInformations;
    }

}

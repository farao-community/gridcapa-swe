package com.farao_community.farao.gridcapa_swe_commons.shift;

import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
class ZonalScalableProviderTest {

    private ZonalScalableProvider zonalScalable;
    private final String glskFilename = "cim_Glsk.xml";

    @Test
    void testCountryGeneratorsScalableforFR() {
        this.zonalScalable = new ZonalScalableProvider();
        String glskUrl = Objects.requireNonNull(getClass().getResource("/shift/" + glskFilename)).toString(); //Glsk file does not contains FR
        OffsetDateTime timestamp = OffsetDateTime.parse("2021-02-09T19:30:00Z");
        Network network = Network.read("hvdc/TestCase16Nodes.xiidm", getClass().getResourceAsStream("/hvdc/TestCase16Nodes.xiidm"));
        ZonalData<Scalable> zonalData = zonalScalable.get(glskUrl, network, timestamp);
        Scalable scalableFR = zonalData.getData("10YFR-RTE------C");
        assertNotNull(scalableFR);
        assertEquals(5, scalableFR.filterInjections(network).size());
        assertEquals("FFR1AA1 _generator", scalableFR.filterInjections(network).get(0).getId());
    }

}

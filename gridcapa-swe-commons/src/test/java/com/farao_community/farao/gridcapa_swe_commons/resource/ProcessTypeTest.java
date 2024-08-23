package com.farao_community.farao.gridcapa_swe_commons.resource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
class ProcessTypeTest {

    @Test
    void testGetCode() {
        assertEquals("2D", ProcessType.D2CC.getCode(), "D2CC should return '2D'");
        assertEquals("1D", ProcessType.IDCC.getCode(), "IDCC should return '1D'");
        assertEquals("IDCF", ProcessType.IDCC_IDCF.getCode(), "IDCC_IDCF should return 'IDCF'");
        assertEquals("BTCC", ProcessType.BTCC.getCode(), "BTCC should return 'BTCC'");

    }
}

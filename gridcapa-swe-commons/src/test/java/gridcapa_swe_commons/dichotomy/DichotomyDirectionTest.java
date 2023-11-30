/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package gridcapa_swe_commons.dichotomy;

import com.farao_community.farao.gridcapa_swe_commons.dichotomy.DichotomyDirection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
class DichotomyDirectionTest {

    @Test
    void getShortNameTest() {
        assertEquals("ESFR", DichotomyDirection.ES_FR.getShortName());
        assertEquals("ESPT", DichotomyDirection.ES_PT.getShortName());
        assertEquals("FRES", DichotomyDirection.FR_ES.getShortName());
        assertEquals("PTES", DichotomyDirection.PT_ES.getShortName());
    }

    @Test
    void getDashNameTest() {
        assertEquals("ES-FR", DichotomyDirection.ES_FR.getDashName());
        assertEquals("ES-PT", DichotomyDirection.ES_PT.getDashName());
        assertEquals("FR-ES", DichotomyDirection.FR_ES.getDashName());
        assertEquals("PT-ES", DichotomyDirection.PT_ES.getDashName());
    }
}

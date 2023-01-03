package com.farao_community.farao.swe.runner.app.dichotomy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@SpringBootTest
class DichotomyDirectionTest {

    @Test
    void simpleTest() {
        DichotomyDirection directionEsFr = DichotomyDirection.ES_FR;
        assertEquals("ES-FR", directionEsFr.getName());
    }
}

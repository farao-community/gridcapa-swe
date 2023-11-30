package gridcapa_swe_commons.resource;

import com.farao_community.farao.gridcapa_swe_commons.resource.ProcessType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
class ProcessTypeTest {

    @Test
    void testGetCode() {
        assertEquals("2D", ProcessType.D2CC.getCode(), "D2CC should return '2D'");
        assertEquals("ID", ProcessType.IDCC.getCode(), "IDCC should return 'ID'");
        assertEquals("IDCF", ProcessType.IDCC_IDCF.getCode(), "IDCC_IDCF should return 'IDCF'");
    }

}

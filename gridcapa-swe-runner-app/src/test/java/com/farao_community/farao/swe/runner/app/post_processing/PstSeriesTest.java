package com.farao_community.farao.swe.runner.app.post_processing;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.json.RaoParametersJsonModule;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.sensitivity.json.SensitivityJsonModule;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

class PstSeriesTest {
    private final String testDirectory = "/arkale_biescas/";

    @Test
    void test() throws IOException {
        // Network
        final String networkFileName = testDirectory + "network_0230_ESFR_2750.xiidm";
        final Network network = Network.read(networkFileName, getClass().getResourceAsStream(networkFileName));
        // Crac
        final InputStream inputStream = getClass().getResourceAsStream(testDirectory + "cracFrEs_0230.json");
        final Crac crac = Crac.read("cracFrEs_0230.json", inputStream, network);
        // Rao result
        final InputStream raoResultIs = getClass().getResourceAsStream(testDirectory + "raoResult_0230_ESFR_2750.json");
        final RaoResult raoResult = RaoResult.read(raoResultIs, crac);
        // LF parameters
        final InputStream raoParamInputStream = getClass().getResourceAsStream(testDirectory + "raoParametersES_FR_0230.json");
        final ObjectMapper mapper = createObjectMapper();
        final RaoParameters raoParameters = mapper.readerForUpdating(new RaoParameters()).readValue(raoParamInputStream);
        final LoadFlowParameters loadFlowParameters = raoParameters.getLoadFlowAndSensitivityParameters().getSensitivityWithLoadFlowParameters().getLoadFlowParameters();
        //
        final PstSeries pstSeries = new PstSeries(network, raoResult, loadFlowParameters, crac);
        pstSeries.runRegulationLoadFLow();
        final double a = 1d;
    }

    public static ObjectMapper createObjectMapper() {
        return JsonMapper.builder()
                .enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
                .disable(JsonWriteFeature.WRITE_NAN_AS_STRINGS)
                .enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
                .build()
                .registerModule(new RaoParametersJsonModule())
                .registerModule(new SensitivityJsonModule());
    }
}

package com.farao_community.farao.gridcapa_swe_commons.shift;

import com.farao_community.farao.dichotomy.api.exceptions.ShiftingException;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;

import java.util.Set;

public class SweGeneratorsShiftHelper {

    private static final Set<Country> PRE_PROCESSING_COUNTRIES = Set.of(Country.ES, Country.PT);
    ScalableGeneratorConnector scalableGeneratorConnector;
    GeneratorLimitsHandler generatorLimitsHandler;

    public SweGeneratorsShiftHelper(ZonalData<Scalable> zonalScalable) {
        scalableGeneratorConnector = new ScalableGeneratorConnector(zonalScalable);
        generatorLimitsHandler = new GeneratorLimitsHandler(zonalScalable);
    }

    public void preProcessNetwork(Network network, String initialVariantId, String processedVariantId, String workingVariantCopyId) throws ShiftingException {
        network.getVariantManager().cloneVariant(initialVariantId, processedVariantId, true);
        network.getVariantManager().setWorkingVariant(processedVariantId);
        scalableGeneratorConnector.fillGeneratorsInitialState(network, PRE_PROCESSING_COUNTRIES);
        // here set working variant generators pmin and pmax values to default values
        // so that glsk generator pmin and pmax values are used
        generatorLimitsHandler.setPminPmaxToDefaultValue(network, PRE_PROCESSING_COUNTRIES);
        network.getVariantManager().cloneVariant(processedVariantId, workingVariantCopyId, true);
        network.getVariantManager().setWorkingVariant(workingVariantCopyId);
    }

    public void connectGeneratorsTransformers(Network network) {
        // During the shift some generators linked to the main network with a transformers are not connected correctly
        // Waiting for a fix in powsybl-core, we connect the transformers linked to these generators to be correctly connected to the main network component
        scalableGeneratorConnector.connectGeneratorsTransformers(network, PRE_PROCESSING_COUNTRIES);
    }

    public void resetInitialPminPmax(Network network) {
        // here set working variant generators pmin and pmax values to initial values
        generatorLimitsHandler.resetInitialPminPmax(network);
    }
}


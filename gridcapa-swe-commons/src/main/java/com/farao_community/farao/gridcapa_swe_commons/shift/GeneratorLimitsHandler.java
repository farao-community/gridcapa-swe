package com.farao_community.farao.gridcapa_swe_commons.shift;

import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.EICode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Before scaling: set generators pmin and pmax values to default values,
 * so that glsk generator pmin and pmax values are used
 * Only for {ES, PT} because FR is always in proportional and not absolute values
 * After scaling: reset to initial Pmin Pmax
 *
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public class GeneratorLimitsHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeneratorLimitsHandler.class);
    private final ZonalData<Scalable> zonalScalableData;
    private static final double DEFAULT_PMAX = 9999.0;
    private static final double DEFAULT_PMIN = -9999.0;
    private Map<String, InitGeneratorLimits> initGenerators;

    public GeneratorLimitsHandler(ZonalData<Scalable> zonalScalableData) {
        this.zonalScalableData = zonalScalableData;
    }

    void setPminPmaxToDefaultValue(Network network, Set<Country> countries) {
        initGenerators = new HashMap<>();
        countries.forEach(country -> {
                    Set<Generator> generators = zonalScalableData.getData(new EICode(country).getAreaCode()).filterInjections(network)
                            .stream()
                            .filter(Generator.class::isInstance)
                            .map(Generator.class::cast)
                            .filter(gen -> gen.getTerminal().getVoltageLevel().getSubstation().isPresent()
                                    && gen.getTerminal().getVoltageLevel().getSubstation().get().getCountry().equals(Optional.of(country)))
                            .collect(Collectors.toSet());
                    generators.forEach(generator -> {
                        saveInitLimits(generator);
                        if (Double.isNaN(generator.getTargetP())) {
                            generator.setTargetP(0.);
                        }
                        generator.setMinP(DEFAULT_PMIN);
                        generator.setMaxP(DEFAULT_PMAX);
                    });
                });
        LOGGER.info("Pmax and Pmin are set to default values for network {}", network.getNameOrId());
    }

    private void saveInitLimits(Generator generator) {
        InitGeneratorLimits initGeneratorLimits = new InitGeneratorLimits();
        initGeneratorLimits.setpMin(generator.getMinP());
        initGeneratorLimits.setpMax(generator.getMaxP());
        String genId = generator.getId();
        if (!initGenerators.containsKey(genId)) {
            initGenerators.put(genId, initGeneratorLimits);
        }
    }

    void resetInitialPminPmax(Network network) {
        // initGenerators contains the list of generators with Pmin/Pmax modifies in pre-processing step
        initGenerators.forEach((id, initValues) -> {
            Generator generator = network.getGenerator(id);
            generator.setMaxP(Math.max(generator.getTargetP(), initValues.getpMax()));
            generator.setMinP(Math.min(generator.getTargetP(), initValues.getpMin()));
            if (generator.getTargetP() > initValues.getpMax()
                    || generator.getTargetP() < initValues.getpMin()) {
                LOGGER.debug("GENERATOR: id=[{}], has targetP: [{}] outside initial min max values", generator.getId(), generator.getTargetP());
            }
        });
        LOGGER.info("Pmax and Pmin are reset to initial values for network {}", network.getNameOrId());
    }

    private static class InitGeneratorLimits {
        double pMin;
        double pMax;

        public double getpMin() {
            return pMin;
        }

        public void setpMin(double pMin) {
            this.pMin = pMin;
        }

        public double getpMax() {
            return pMax;
        }

        public void setpMax(double pMax) {
            this.pMax = pMax;
        }
    }
}

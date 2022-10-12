/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.configurations;

import com.farao_community.farao.swe.runner.app.utils.Direction;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Configuration
@ConfigurationProperties(prefix = "swe-runner.dichotomy")
public class DichotomyConfiguration {

    private Map<Direction, Parameters> direction;

    public Map<Direction, Parameters> getDirection() {
        return direction;
    }

    public void setDirection(Map<Direction, Parameters> direction) {
        this.direction = direction;
    }

    public static class Parameters {
        private double minValue;
        private double maxValue;
        private double precision;
        private double tolerance;

        public double getMinValue() {
            return minValue;
        }

        public void setMinValue(double minValue) {
            this.minValue = minValue;
        }

        public double getMaxValue() {
            return maxValue;
        }

        public void setMaxValue(double maxValue) {
            this.maxValue = maxValue;
        }

        public double getPrecision() {
            return precision;
        }

        public void setPrecision(double precision) {
            this.precision = precision;
        }

        public double getTolerance() {
            return tolerance;
        }

        public void setTolerance(double tolerance) {
            this.tolerance = tolerance;
        }
    }
}
/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Optional;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@ConfigurationProperties("swe-runner")
public class SweClientProperties {
    private BindingConfiguration binding;

    public BindingConfiguration getBinding() {
        return binding;
    }

    public void setBinding(BindingConfiguration binding) {
        this.binding = binding;
    }

    public static class BindingConfiguration {
        private String destination;
        private String routingKey;
        private String expiration;
        private String applicationId;

        public String getDestination() {
            return destination;
        }

        public void setDestination(String destination) {
            this.destination = destination;
        }

        public String getRoutingKey() {
            return Optional.ofNullable(routingKey).orElse("#");
        }

        public void setRoutingKey(String routingKey) {
            this.routingKey = routingKey;
        }

        public String getExpiration() {
            return expiration;
        }

        public void setExpiration(String expiration) {
            this.expiration = expiration;
        }

        public String getApplicationId() {
            return applicationId;
        }

        public void setApplicationId(String applicationId) {
            this.applicationId = applicationId;
        }
    }
}

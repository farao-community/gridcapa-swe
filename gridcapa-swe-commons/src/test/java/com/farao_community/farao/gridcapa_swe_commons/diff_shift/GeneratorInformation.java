/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_swe_commons.diff_shift;

import org.jgrapht.alg.util.Pair;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public class GeneratorInformation {
    private String id;
    private double pMin;
    private double pMax;
    private double targetP;

    private boolean connected;

    private boolean mainComponentConnected;

    private Map<String, Pair<Boolean, Boolean>> twoWindingsTransformerConnection;

    public GeneratorInformation(String id, double pMin, double pMax, double targetP, boolean connected, boolean mainComponentConnected, Map<String, Pair<Boolean, Boolean>> twoWindingsTransformersInfo) {
        this.id = id;
        this.pMin = pMin;
        this.pMax = pMax;
        this.targetP = targetP;
        this.connected = connected;
        this.mainComponentConnected = mainComponentConnected;
        this.twoWindingsTransformerConnection = twoWindingsTransformersInfo;
    }

    public GeneratorInformation(String id, double pMin, double pMax, double targetP, boolean connected, boolean mainComponentConnected) {
        this.id = id;
        this.pMin = pMin;
        this.pMax = pMax;
        this.targetP = targetP;
        this.connected = connected;
        this.mainComponentConnected = mainComponentConnected;
        this.twoWindingsTransformerConnection = new HashMap<>();
    }

    public String getId() {
        return id;
    }

    public double getpMin() {
        return pMin;
    }

    public double getpMax() {
        return pMax;
    }

    public double getTargetP() {
        return targetP;
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isMainComponentConnected() {
        return mainComponentConnected;
    }

    public boolean hasDifferentStatus(GeneratorInformation generatorInformation) {
        return id.equals(generatorInformation.getId()) && connected != generatorInformation.isConnected() || mainComponentConnected != generatorInformation.isMainComponentConnected();
    }

    public Map<String, Pair<Boolean, Boolean>> getTwoWindingsTransformerConnection() {
        return twoWindingsTransformerConnection;
    }

    public boolean hasDifferentTwoWindingsTransformersConnection(GeneratorInformation otherGeneratorInformation) {
        Map<String, Pair<Boolean, Boolean>> otherTwoWindingsTransformerConnection = otherGeneratorInformation.getTwoWindingsTransformerConnection();
        for (Map.Entry<String, Pair<Boolean, Boolean>> entry : twoWindingsTransformerConnection.entrySet()) {
            String k = entry.getKey();
            Pair<Boolean, Boolean> v = entry.getValue();
            Pair<Boolean, Boolean> otherStatus = otherTwoWindingsTransformerConnection.get(k);
            if (v.getFirst() != otherStatus.getFirst() || v.getSecond() != otherStatus.getSecond()) {
                return true;
            }
        }
        return false;
    }

    public String displayWithoutTwt() {
        return targetP + ";" + pMin + ";" + pMax + ";" + connected + ";" + mainComponentConnected;
    }

    public String listTwtIds() {
        StringBuilder twtIds = new StringBuilder();
        for (String s : twoWindingsTransformerConnection.keySet()) {
            twtIds.append(s).append(";");
        }
        return twtIds.toString();
    }
}

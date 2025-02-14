/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_swe_commons.hvdc;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
public class HvdcInformation {
    private final String id;

    private boolean isSide1GeneratorConnected;
    private double side1GeneratorTargetP;

    private boolean isSide2GeneratorConnected;
    private double side2GeneratorTargetP;

    private boolean isSide1LoadConnected;
    private double side1LoadP;

    private boolean isSide2LoadConnected;
    private double side2LoadP;

    private boolean isAcLineTerminal1Connected;
    private boolean isAcLineTerminal2Connected;

    public HvdcInformation(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public boolean isSide1GeneratorConnected() {
        return isSide1GeneratorConnected;
    }

    public double getSide1GeneratorTargetP() {
        return side1GeneratorTargetP;
    }

    public boolean isSide2GeneratorConnected() {
        return isSide2GeneratorConnected;
    }

    public double getSide2GeneratorTargetP() {
        return side2GeneratorTargetP;
    }

    public boolean isSide1LoadConnected() {
        return isSide1LoadConnected;
    }

    public double getSide1LoadP() {
        return side1LoadP;
    }

    public boolean isSide2LoadConnected() {
        return isSide2LoadConnected;
    }

    public double getSide2LoadP() {
        return side2LoadP;
    }

    public boolean isAcLineTerminal1Connected() {
        return isAcLineTerminal1Connected;
    }

    public boolean isAcLineTerminal2Connected() {
        return isAcLineTerminal2Connected;
    }

    public void setSide1GeneratorConnected(final boolean side1GeneratorConnected) {
        isSide1GeneratorConnected = side1GeneratorConnected;
    }

    public void setSide1GeneratorTargetP(final double side1GeneratorTargetP) {
        this.side1GeneratorTargetP = side1GeneratorTargetP;
    }

    public void setSide2GeneratorConnected(final boolean side2GeneratorConnected) {
        isSide2GeneratorConnected = side2GeneratorConnected;
    }

    public void setSide2GeneratorTargetP(final double side2GeneratorTargetP) {
        this.side2GeneratorTargetP = side2GeneratorTargetP;
    }

    public void setSide1LoadConnected(final boolean side1LoadConnected) {
        isSide1LoadConnected = side1LoadConnected;
    }

    public void setSide1LoadP(final double side1LoadP) {
        this.side1LoadP = side1LoadP;
    }

    public void setSide2LoadConnected(final boolean side2LoadConnected) {
        isSide2LoadConnected = side2LoadConnected;
    }

    public void setSide2LoadP(final double side2LoadP) {
        this.side2LoadP = side2LoadP;
    }

    public void setAcLineTerminal1Connected(final boolean acLineTerminal1Connected) {
        isAcLineTerminal1Connected = acLineTerminal1Connected;
    }

    public void setAcLineTerminal2Connected(final boolean acLineTerminal2Connected) {
        isAcLineTerminal2Connected = acLineTerminal2Connected;
    }
}

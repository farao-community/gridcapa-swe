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
    private String id;

    private Boolean isSide1GeneratorConnected;
    private Double side1GeneratorTargetP;

    private Boolean isSide2GeneratorConnected;
    private Double side2GeneratorTargetP;

    private Boolean isSide1LoadConnected;
    private Double side1LoadP;

    private Boolean isSide1Option2LoadConnected;
    private Double side1option2LoadP;

    private Boolean isSide2LoadConnected;
    private Double side2LoadP;

    private Boolean isAcLineTerminal1Connected;
    private Boolean isAcLineTerminal2Connected;

    public HvdcInformation(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public Boolean isSide1GeneratorConnected() {
        return isSide1GeneratorConnected;
    }

    public Double getSide1GeneratorTargetP() {
        return side1GeneratorTargetP;
    }

    public Boolean isSide2GeneratorConnected() {
        return isSide2GeneratorConnected;
    }

    public Double getSide2GeneratorTargetP() {
        return side2GeneratorTargetP;
    }

    public Boolean isSide1LoadConnected() {
        return isSide1LoadConnected;
    }

    public Double getSide1LoadP() {
        return side1LoadP;
    }

    public Boolean isSide2LoadConnected() {
        return isSide2LoadConnected;
    }

    public Double getSide2LoadP() {
        return side2LoadP;
    }

    public Boolean isAcLineTerminal1Connected() {
        return isAcLineTerminal1Connected;
    }

    public Boolean isAcLineTerminal2Connected() {
        return isAcLineTerminal2Connected;
    }

    public void setSide1GeneratorConnected(Boolean side1GeneratorConnected) {
        isSide1GeneratorConnected = side1GeneratorConnected;
    }

    public void setSide1GeneratorTargetP(Double side1GeneratorTargetP) {
        this.side1GeneratorTargetP = side1GeneratorTargetP;
    }

    public void setSide2GeneratorConnected(Boolean side2GeneratorConnected) {
        isSide2GeneratorConnected = side2GeneratorConnected;
    }

    public void setSide2GeneratorTargetP(Double side2GeneratorTargetP) {
        this.side2GeneratorTargetP = side2GeneratorTargetP;
    }

    public void setSide1LoadConnected(Boolean side1LoadConnected) {
        isSide1LoadConnected = side1LoadConnected;
    }

    public void setSide1LoadP(Double side1LoadP) {
        this.side1LoadP = side1LoadP;
    }

    public void setSide2LoadConnected(Boolean side2LoadConnected) {
        isSide2LoadConnected = side2LoadConnected;
    }

    public void setSide2LoadP(Double side2LoadP) {
        this.side2LoadP = side2LoadP;
    }

    public void setAcLineTerminal1Connected(Boolean acLineTerminal1Connected) {
        isAcLineTerminal1Connected = acLineTerminal1Connected;
    }

    public void setAcLineTerminal2Connected(Boolean acLineTerminal2Connected) {
        isAcLineTerminal2Connected = acLineTerminal2Connected;
    }

    public Boolean setSide1Option2LoadConnected() {
        return isSide1Option2LoadConnected;
    }

    public void setSide1Option2LoadConnected(Boolean side1Option2LoadConnected) {
        isSide1Option2LoadConnected = side1Option2LoadConnected;
    }

    public Double getSide1option2LoadP() {
        return side1option2LoadP;
    }

    public void setSide1option2LoadP(Double side1option2LoadP) {
        this.side1option2LoadP = side1option2LoadP;
    }
}

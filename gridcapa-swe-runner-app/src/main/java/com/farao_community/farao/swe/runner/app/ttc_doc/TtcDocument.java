/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.ttc_doc;

import com.farao_community.farao.dichotomy.api.results.DichotomyResult;
import com.farao_community.farao.swe.runner.api.exception.SweInternalException;
import com.farao_community.farao.swe.runner.api.exception.SweInvalidDataException;
import com.farao_community.farao.swe.runner.app.dichotomy.DichotomyDirection;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyResult;
import com.farao_community.farao.swe.runner.app.domain.SweDichotomyValidationData;
import com.farao_community.farao.swe.runner.app.parallelization.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
public class TtcDocument {
    private static final Logger LOGGER = LoggerFactory.getLogger(TtcDocument.class);
    public static final String DIRECTION_SEPARATOR = "-";

    private final ExecutionResult<SweDichotomyResult> executionResult;
    private final Map<DichotomyDirection, String> mapWithValues;
    private Document doc;

    private Element additionalInfos;
    private Element physicalExchanges;

    public TtcDocument(ExecutionResult<SweDichotomyResult> executionResult) {
        this.executionResult = executionResult;
        this.mapWithValues = new EnumMap<>(DichotomyDirection.class);
    }

    public InputStream buildTtcDocFile() {
        buildMapResult();
        initializeDocument();
        createAdditionalInfoElement();
        createPhysicalExchangesElement();
        createAllBorderElements();
        return exportFile();
    }

    private void buildMapResult() {
        List<SweDichotomyResult> listDichotomyResults = executionResult.getResult();
        listDichotomyResults.forEach(r -> addValueToResultMap(r.getDichotomyDirection(), r.getDichotomyResult()));
    }


    private void addValueToResultMap(DichotomyDirection direction, DichotomyResult<SweDichotomyValidationData> result) {
        if (result.hasValidStep()) {
            mapWithValues.put(direction, String.valueOf((int) result.getHighestValidStepValue()));
        } else {
            mapWithValues.put(direction, "");
        }
    }

    private void initializeDocument() {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            this.doc = dBuilder.newDocument();
        } catch (ParserConfigurationException e) {
            LOGGER.error("Could not instantiate ttc document");
        }
    }

    private void createAdditionalInfoElement() {
        this.additionalInfos = doc.createElement("Additional_info");
        doc.appendChild(additionalInfos);
    }

    private void createPhysicalExchangesElement() {
        this.physicalExchanges = doc.createElement("physical_exchanges");
        additionalInfos.appendChild(physicalExchanges);
    }

    private void createAllBorderElements() {
        mapWithValues.forEach(this::createBorderElement);
    }

    private void createBorderElement(DichotomyDirection direction, String value) {
        Element border = doc.createElement("border");
        addAttributes(border, direction);
        addValueElement(border, value);
        physicalExchanges.appendChild(border);
    }

    private void addAttributes(Element border, DichotomyDirection direction) {
        border.setAttribute("from", getCountryNameFromInitial(direction.getName().split(DIRECTION_SEPARATOR)[0]));
        border.setAttribute("to", getCountryNameFromInitial(direction.getName().split(DIRECTION_SEPARATOR)[1]));
    }

    private String getCountryNameFromInitial(String direction) {
        switch (direction) {
            case "ES":
                return "SPAIN";
            case "FR":
                return "FRANCE";
            case "PT":
                return "PORTUGAL";
            default:
                throw new SweInvalidDataException("This direction does not exist in the enum");
        }
    }

    private void addValueElement(Element border, String value) {
        Element valueElement = doc.createElement("value");
        valueElement.appendChild(doc.createTextNode(value));
        border.appendChild(valueElement);
    }

    private InputStream exportFile() {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Source xmlSource = new DOMSource(doc);
            Result outputTarget = new StreamResult(outputStream);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            transformerFactory.newTransformer().transform(xmlSource, outputTarget);
            return new ByteArrayInputStream(outputStream.toByteArray());
        } catch (TransformerException e) {
            throw new SweInternalException("Could not export ttc document");
        }
    }
}

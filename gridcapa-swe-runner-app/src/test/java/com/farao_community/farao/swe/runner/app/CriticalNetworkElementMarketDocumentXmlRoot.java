package com.farao_community.farao.swe.runner.app;

import com.powsybl.openrao.data.raoresult.io.cne.swe.xsd.CriticalNetworkElementMarketDocument;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "CriticalNetworkElement_MarketDocument", namespace = "urn:iec62325.351:tc57wg16:451-n:cnedocument:2:3")
public class CriticalNetworkElementMarketDocumentXmlRoot extends CriticalNetworkElementMarketDocument  {
}

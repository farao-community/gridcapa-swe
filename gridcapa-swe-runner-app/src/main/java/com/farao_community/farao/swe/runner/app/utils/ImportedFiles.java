package com.farao_community.farao.swe.runner.app.utils;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.cim.CimCrac;
import com.powsybl.iidm.network.Network;

public class ImportedFiles {

    private final Network network;
    private final CimCrac cimCrac;
    private final Crac cracEsPt;
    private final Crac cracFrEs;
    private final String jsonPathEsPt;
    private final String jsonPathFrEs;

    public ImportedFiles(Network network, CimCrac cimCrac, Crac cracEsPt, Crac cracFrEs, String jsonPathEsPt, String jsonPathFrEs) {
        this.network = network;
        this.cimCrac = cimCrac;
        this.cracEsPt = cracEsPt;
        this.cracFrEs = cracFrEs;
        this.jsonPathEsPt = jsonPathEsPt;
        this.jsonPathFrEs = jsonPathFrEs;
    }

    public Network getNetwork() {
        return network;
    }

    public CimCrac getCimCrac() {
        return cimCrac;
    }

    public Crac getCracEsPt() {
        return cracEsPt;
    }

    public Crac getCracFrEs() {
        return cracFrEs;
    }

    public String getJsonPathEsPt() {
        return jsonPathEsPt;
    }

    public String getJsonPathFrEs() {
        return jsonPathFrEs;
    }
}

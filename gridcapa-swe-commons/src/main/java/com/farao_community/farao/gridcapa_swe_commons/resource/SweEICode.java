/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_swe_commons.resource;

import com.powsybl.glsk.commons.CountryEICode;

import static com.powsybl.iidm.network.Country.*;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public final class SweEICode {

    public static final String PT_EIC = new CountryEICode(PT).getCode();
    public static final String ES_EIC = new CountryEICode(ES).getCode();
    public static final String FR_EIC = new CountryEICode(FR).getCode();

    private SweEICode() {
    }
}

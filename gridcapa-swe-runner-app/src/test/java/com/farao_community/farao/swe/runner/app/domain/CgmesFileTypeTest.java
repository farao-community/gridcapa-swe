/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.swe.runner.app.domain;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */

class CgmesFileTypeTest {

    @Test
    void getTsoTest() {
        SoftAssertions assertions = new SoftAssertions();
        assertions.assertThat(CgmesFileType.CORESO_SV.getTso()).isEqualTo("CORESO");
        assertions.assertThat(CgmesFileType.RTE_SSH.getTso()).isEqualTo("RTE");
        assertions.assertThat(CgmesFileType.RTE_EQ.getTso()).isEqualTo("RTE");
        assertions.assertThat(CgmesFileType.RTE_TP.getTso()).isEqualTo("RTE");
        assertions.assertThat(CgmesFileType.REE_SSH.getTso()).isEqualTo("REE");
        assertions.assertThat(CgmesFileType.REE_EQ.getTso()).isEqualTo("REE");
        assertions.assertThat(CgmesFileType.REE_TP.getTso()).isEqualTo("REE");
        assertions.assertThat(CgmesFileType.REN_SSH.getTso()).isEqualTo("REN");
        assertions.assertThat(CgmesFileType.REN_EQ.getTso()).isEqualTo("REN");
        assertions.assertThat(CgmesFileType.REN_TP.getTso()).isEqualTo("REN");
        assertions.assertAll();
    }

    @Test
    void getFileTypeTest() {
        SoftAssertions assertions = new SoftAssertions();
        assertions.assertThat(CgmesFileType.CORESO_SV.getFileType()).isEqualTo("SV");
        assertions.assertThat(CgmesFileType.RTE_SSH.getFileType()).isEqualTo("SSH");
        assertions.assertThat(CgmesFileType.RTE_EQ.getFileType()).isEqualTo("EQ");
        assertions.assertThat(CgmesFileType.RTE_TP.getFileType()).isEqualTo("TP");
        assertions.assertThat(CgmesFileType.REE_SSH.getFileType()).isEqualTo("SSH");
        assertions.assertThat(CgmesFileType.REE_EQ.getFileType()).isEqualTo("EQ");
        assertions.assertThat(CgmesFileType.REE_TP.getFileType()).isEqualTo("TP");
        assertions.assertThat(CgmesFileType.REN_SSH.getFileType()).isEqualTo("SSH");
        assertions.assertThat(CgmesFileType.REN_EQ.getFileType()).isEqualTo("EQ");
        assertions.assertThat(CgmesFileType.REN_TP.getFileType()).isEqualTo("TP");
        assertions.assertAll();
    }
}

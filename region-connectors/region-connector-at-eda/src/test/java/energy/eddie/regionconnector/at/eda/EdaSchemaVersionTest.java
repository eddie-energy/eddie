// SPDX-FileCopyrightText: 2023 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EdaSchemaVersionTest {
    @Test
    void edaSchemaVersionValue() {
        // given
        EdaSchemaVersion version = EdaSchemaVersion.CM_REQUEST_01_10;
        String expectedValue = "01.10";

        // when
        String actualValue = version.value();
        // then
        assertEquals(expectedValue, actualValue);
    }
}
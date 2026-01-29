// SPDX-FileCopyrightText: 2023-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.requests;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequestDataTypeTest {

    @Test
    void requestDataTypeToStringMasterData() {
        // given
        RequestDataType type = RequestDataType.MASTER_DATA;
        String expectedValue = "MasterData";

        // when
        String actualValue = type.toString();

        // then
        assertEquals(expectedValue, actualValue);
    }


    @Test
    void requestDataTypeToStringMeteringData() {
        // given
        RequestDataType type = RequestDataType.METERING_DATA;
        String expectedValue = "MeteringData";

        // when
        String actualValue = type.toString();

        // then
        assertEquals(expectedValue, actualValue);
    }
}

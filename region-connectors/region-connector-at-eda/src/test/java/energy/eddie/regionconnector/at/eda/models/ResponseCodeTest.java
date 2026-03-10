// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.models;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResponseCodeTest {

    @Test
    void givenCmReqOnl_whenGetCode_thenEachCodeUnique() {
        // Given
        var codes = ResponseCode.KnownResponseCodes.values();

        // When
        var res = Arrays.stream(codes)
                        .map(ResponseCode.KnownResponseCodes::getCode)
                        .distinct()
                        .toArray();

        // Then
        assertEquals(codes.length, res.length);
    }
}
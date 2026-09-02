// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.dtos;

import energy.eddie.aiida.ObjectMapperCreatorUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PermissionDetailsDtoTest {

    @Test
    void permissionRequestWithLimitDefaults_parsesExactValues() {
        // Given
        var json = """
                {
                  "permission_request": {
                    "permission_id": "a1f58555-4b87-4624-996c-ec4cf6ddb6c3",
                    "connection_id": "conn-1",
                    "meter_id": "meter-1",
                    "min_limit_kw": "0.1234567890123456789012345678901",
                    "max_limit_kw": "10.25",
                    "start": "2026-09-01",
                    "end": "2026-10-01"
                  }
                }
                """;

        // When
        var dto = ObjectMapperCreatorUtil.mapper().readValue(json, PermissionDetailsDto.class);

        // Then
        assertEquals(new BigDecimal("0.1234567890123456789012345678901"), dto.minLimitKw());
        assertEquals(new BigDecimal("10.25"), dto.maxLimitKw());
    }

    @Test
    void permissionRequestWithNullLimitDefaults_keepsLimitsNull() {
        // Given
        var json = """
                {
                  "permission_request": {
                    "permission_id": "a1f58555-4b87-4624-996c-ec4cf6ddb6c3",
                    "connection_id": "conn-1",
                    "meter_id": "meter-1",
                    "min_limit_kw": null,
                    "max_limit_kw": null,
                    "start": "2026-09-01",
                    "end": "2026-10-01"
                  }
                }
                """;

        // When
        var dto = ObjectMapperCreatorUtil.mapper().readValue(json, PermissionDetailsDto.class);

        // Then
        assertNull(dto.minLimitKw());
        assertNull(dto.maxLimitKw());
    }
}

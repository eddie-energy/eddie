// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.shared.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommonPathsTest {

    @Test
    void testGetServletPathForOutboundConnector_returnsPathForRegionConnector() {
        // Given
        var oc = "admin-console";

        // When
        var res = CommonPaths.getServletPathForOutboundConnector(oc);

        // Then
        assertEquals("/outbound-connectors/admin-console", res);
    }
}
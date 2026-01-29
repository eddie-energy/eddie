// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.cim.serde;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SerdeFactoryTest {

    @Test
    void testGetInstance_returnsNotNull() {
        // Given
        // When
        var res = SerdeFactory.getInstance();

        // Then
        assertNotNull(res);
    }
}
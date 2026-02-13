// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.aiida;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiidaAssetTest {
    @Test
    void forValue_returnsExpectedAsset() {
        // Given
        var assetValue = "CONNECTION-AGREEMENT-POINT";

        // Given / When
        var asset = AiidaAsset.forValue(assetValue);

        // Then
        assertEquals(AiidaAsset.CONNECTION_AGREEMENT_POINT, asset);
    }

    @Test
    void forValue_throws_whenUnknownValue() {
        // Given
        var unknownValue = "UNKNOWN_VALUE";

        // When / Then
        assertThrows(NoSuchElementException.class, () -> AiidaAsset.forValue(unknownValue));
    }
}

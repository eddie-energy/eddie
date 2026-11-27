// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.ee.elering;

import energy.eddie.api.v0.RegionConnectorMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class EleringRegionConnectorTest {

    @Mock
    private EleringRegionConnectorMetadata metadata;

    @Test
    void getMetadata_returnsExpected() {
        // Given
        EleringRegionConnector regionConnector = new EleringRegionConnector(metadata);

        // When
        RegionConnectorMetadata result = regionConnector.getMetadata();

        // Then
        assertEquals(metadata, result);
    }
}

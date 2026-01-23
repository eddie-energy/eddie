// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.nl.mijn.aansluiting;

import energy.eddie.regionconnector.nl.mijn.aansluiting.services.TerminationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MijnAansluitingRegionConnectorTest {
    @Mock
    private TerminationService terminationService;
    @InjectMocks
    private MijnAansluitingRegionConnector regionConnector;

    @Test
    void testTermination_terminatesPermissionRequest() {
        // Given
        // When
        regionConnector.terminatePermission("pid");

        // Then
        verify(terminationService).terminate("pid");
    }

    @Test
    void testGetMetadata_returnsMetadata() {
        // Given
        // When
        var res = regionConnector.getMetadata();

        // Then
        assertEquals(MijnAansluitingRegionConnectorMetadata.getInstance(), res);
    }
}
// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius;

import energy.eddie.regionconnector.be.fluvius.service.TerminationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FluviusRegionConnectorTest {
    @Mock
    private TerminationService terminationService;
    @Spy
    @SuppressWarnings("unused")
    private final FluviusRegionConnectorMetadata metadata = new FluviusRegionConnectorMetadata();
    @InjectMocks
    private FluviusRegionConnector fluviusRegionConnector;

    @Test
    void testTerminatePermission_callsTerminationService() {
        // Given
        // When
        fluviusRegionConnector.terminatePermission("pid");

        // Then
        verify(terminationService).terminate("pid");
    }
}
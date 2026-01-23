// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fi.fingrid.health;

import energy.eddie.regionconnector.fi.fingrid.client.FingridApiClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FingridHealthIndicatorTest {
    @Mock
    private FingridApiClient api;
    @InjectMocks
    private FingridHealthIndicator healthIndicator;

    @Test
    void testHealth() {
        // Given
        when(api.health()).thenReturn(Health.up().build());

        // When
        var res = healthIndicator.health();

        // Then
        assertEquals(Health.up().build(), res);
    }
}
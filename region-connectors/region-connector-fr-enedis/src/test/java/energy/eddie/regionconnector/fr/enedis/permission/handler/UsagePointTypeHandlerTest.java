// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.permission.handler;

import energy.eddie.regionconnector.fr.enedis.api.UsagePointType;
import energy.eddie.regionconnector.fr.enedis.permission.events.FrUsagePointTypeEvent;
import energy.eddie.regionconnector.fr.enedis.services.HistoricalDataService;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class UsagePointTypeHandlerTest {
    @Spy
    private final EventBus eventBus = new EventBusImpl();
    @Mock
    private HistoricalDataService historicalDataService;
    @InjectMocks
    @SuppressWarnings("unused")
    private UsagePointTypeHandler usagePointTypeHandler;

    @Test
    void testAccept_fetchesHistoricalData() {
        // Given
        // When
        eventBus.emit(new FrUsagePointTypeEvent("pid", UsagePointType.CONSUMPTION));

        // Then
        verify(historicalDataService).fetchHistoricalMeterReadings("pid");
    }
}

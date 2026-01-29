// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.permission.handlers;

import energy.eddie.regionconnector.be.fluvius.permission.events.AcceptedEvent;
import energy.eddie.regionconnector.be.fluvius.permission.request.MeterReading;
import energy.eddie.regionconnector.be.fluvius.persistence.BePermissionRequestRepository;
import energy.eddie.regionconnector.be.fluvius.service.polling.PollingService;
import energy.eddie.regionconnector.be.fluvius.util.DefaultFluviusPermissionRequestBuilder;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcceptedEventHandlerTest {
    @Spy
    private EventBus eventBus = new EventBusImpl();
    @SuppressWarnings("unused")
    @InjectMocks
    private AcceptedEventHandler handler;
    @Mock
    private PollingService pollingService;
    @Mock
    private BePermissionRequestRepository repository;

    @Test
    void testAccept_acceptedEventTriggersPoll() {
        // Given
        var pr = new DefaultFluviusPermissionRequestBuilder()
                .permissionId("pid")
                .build();
        when(repository.getByPermissionId("pid")).thenReturn(pr);
        when(pollingService.isActiveAndNeedsToBeFetched(pr)).thenReturn(true);
        var meterReading = new MeterReading("pid", "ean", null);

        // When
        eventBus.emit(new AcceptedEvent("pid", List.of(meterReading)));

        // Then
        verify(pollingService).pollTimeSeriesData(pr);
    }

    @Test
    void testAccept_acceptedEventForFuturePermissionRequest_doesNothing() {
        // Given
        var pr = new DefaultFluviusPermissionRequestBuilder()
                .permissionId("pid")
                .build();
        when(repository.getByPermissionId("pid")).thenReturn(pr);
        when(pollingService.isActiveAndNeedsToBeFetched(pr)).thenReturn(false);
        var meterReading = new MeterReading("pid", "ean", null);

        // When
        eventBus.emit(new AcceptedEvent("pid", List.of(meterReading)));

        // Then
        verify(pollingService, never()).pollTimeSeriesData(pr);
    }
}
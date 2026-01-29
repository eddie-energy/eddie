// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.permission.handlers;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.cds.permission.events.InternalPollingEvent;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequestBuilder;
import energy.eddie.regionconnector.cds.persistence.CdsPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternalPollingEventHandlerTest {
    @Spy
    private final EventBus eventBus = new EventBusImpl();
    @Mock
    private CdsPermissionRequestRepository repository;
    @Mock
    private Outbox outbox;
    @InjectMocks
    @SuppressWarnings("unused")
    private InternalPollingEventHandler handler;

    @Test
    void testAccept_onAllMeterReadingsAfterOrEqualsToEndDate_emitsFulfilledEvent() {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var pr = new CdsPermissionRequestBuilder()
                .setDataEnd(now.toLocalDate().minusDays(1))
                .build();
        var event = new InternalPollingEvent("pid", Map.of("meterID", now));
        when(repository.getByPermissionId("pid"))
                .thenReturn(pr);

        // When
        eventBus.emit(event);

        // Then
        verify(outbox).commit(assertArg(ev -> assertAll(
                () -> assertEquals("pid", ev.permissionId()),
                () -> assertEquals(PermissionProcessStatus.FULFILLED, ev.status())
        )));
    }

    @Test
    void testAccept_onAllMeterReadingsBeforeEndDate_emitsNothing() {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var pr = new CdsPermissionRequestBuilder()
                .setDataEnd(now.toLocalDate().plusDays(1))
                .build();
        var event = new InternalPollingEvent("pid", Map.of("meterID", now));
        when(repository.getByPermissionId("pid"))
                .thenReturn(pr);

        // When
        eventBus.emit(event);

        // Then
        verify(outbox, never()).commit(any());
    }
}
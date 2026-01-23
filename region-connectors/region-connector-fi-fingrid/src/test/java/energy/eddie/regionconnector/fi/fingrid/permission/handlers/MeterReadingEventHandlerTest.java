// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fi.fingrid.permission.handlers;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.data.needs.DataNeedNotFoundResult;
import energy.eddie.api.agnostic.data.needs.Timeframe;
import energy.eddie.api.agnostic.data.needs.ValidatedHistoricalDataDataNeedResult;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.fi.fingrid.permission.events.MeterReadingEvent;
import energy.eddie.regionconnector.fi.fingrid.permission.request.FingridPermissionRequestBuilder;
import energy.eddie.regionconnector.fi.fingrid.persistence.FiPermissionRequestRepository;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeterReadingEventHandlerTest {
    @Spy
    private final EventBus eventBus = new EventBusImpl();
    @Mock
    private DataNeedCalculationService<DataNeed> calculationService;
    @Mock
    private FiPermissionRequestRepository repository;
    @Mock
    private Outbox outbox;
    @InjectMocks
    @SuppressWarnings("unused")
    private MeterReadingEventHandler handler;


    @Test
    void accept_withUnknownDataNeed_doesNothing() {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var pr = new FingridPermissionRequestBuilder()
                .setPermissionId("pid")
                .setDataNeedId("dnid")
                .setCreated(now)
                .build();
        when(repository.getByPermissionId("pid")).thenReturn(pr);
        when(calculationService.calculate("dnid", now))
                .thenReturn(new DataNeedNotFoundResult());

        // When
        eventBus.emit(new MeterReadingEvent("pid", Map.of()));

        // Then
        verify(outbox, never()).commit(any());
    }

    @Test
    void accept_withUnfinishedMeterReading_doesNothing() {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var today = now.toLocalDate();
        var yesterday = today.minusDays(1);
        var pr = new FingridPermissionRequestBuilder()
                .setPermissionId("pid")
                .setDataNeedId("dnid")
                .setCreated(now)
                .build();
        when(repository.getByPermissionId("pid")).thenReturn(pr);
        when(calculationService.calculate("dnid", now))
                .thenReturn(new ValidatedHistoricalDataDataNeedResult(List.of(Granularity.P1D),
                                                                      new Timeframe(today, today),
                                                                      new Timeframe(today.minusDays(10),
                                                                                    yesterday)));

        // When
        eventBus.emit(new MeterReadingEvent("pid", Map.of("mid", yesterday.atStartOfDay(ZoneOffset.UTC))));

        // Then
        verify(outbox, never()).commit(any());
    }

    @Test
    void accept_withFinishedMeterReading_emitsFulfilledEvent() {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var today = now.toLocalDate();
        var yesterday = today.minusDays(1);
        var pr = new FingridPermissionRequestBuilder()
                .setPermissionId("pid")
                .setDataNeedId("dnid")
                .setCreated(now)
                .build();
        when(repository.getByPermissionId("pid")).thenReturn(pr);
        when(calculationService.calculate("dnid", now))
                .thenReturn(new ValidatedHistoricalDataDataNeedResult(List.of(Granularity.P1D),
                                                                      new Timeframe(today, today),
                                                                      new Timeframe(today.minusDays(10),
                                                                                    yesterday)));

        // When
        eventBus.emit(new MeterReadingEvent("pid", Map.of("mid", now)));

        // Then
        verify(outbox).commit(assertArg(event -> assertAll(
                () -> assertEquals("pid", event.permissionId()),
                () -> assertEquals(PermissionProcessStatus.FULFILLED, event.status())
        )));
    }
}
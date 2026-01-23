// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.permission.handlers;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.be.fluvius.permission.events.MeterReadingUpdatedEvent;
import energy.eddie.regionconnector.be.fluvius.permission.request.MeterReading;
import energy.eddie.regionconnector.be.fluvius.persistence.BePermissionRequestRepository;
import energy.eddie.regionconnector.be.fluvius.util.DefaultFluviusPermissionRequestBuilder;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeterReadingUpdatedEventHandlerTest {
    @Spy
    private final EventBus eventBus = new EventBusImpl();
    @Mock
    private Outbox outbox;
    @Mock
    private BePermissionRequestRepository repository;
    @InjectMocks
    @SuppressWarnings("unused")
    private MeterReadingUpdatedEventHandler handler;

    @ParameterizedTest
    @MethodSource("testAccept_allMeterReadingsAfterEnd_emitsFulfilledEvent")
    void testAccept_allMeterReadingsAfterEnd_emitsFulfilledEvent(ZonedDateTime end, Granularity granularity) {
        // Given
        var readingEnd = end.plusDays(1);
        var pr = new DefaultFluviusPermissionRequestBuilder()
                .permissionId("pid")
                .addMeterReadings(new MeterReading("pid", "001", end))
                .addMeterReadings(new MeterReading("pid", "002", readingEnd))
                .end(end.toLocalDate())
                .granularity(granularity)
                .build();
        when(repository.getByPermissionId("pid")).thenReturn(pr);

        // When
        eventBus.emit(new MeterReadingUpdatedEvent("pid", PermissionProcessStatus.ACCEPTED));

        // Then
        verify(outbox).commit(assertArg(e -> assertAll(
                () -> assertEquals("pid", e.permissionId()),
                () -> assertEquals(PermissionProcessStatus.FULFILLED, e.status())
        )));
    }

    @ParameterizedTest
    @MethodSource("testAccept_notAllMeterReadingsAfterEnd_emitsNothing")
    void testAccept_notAllMeterReadingsAfterEnd_emitsNothing(ZonedDateTime readingEnd, Granularity granularity) {
        // Given
        var end = ZonedDateTime.of(2025, 1, 31, 0, 0, 0, 0, ZoneOffset.UTC);
        var pr = new DefaultFluviusPermissionRequestBuilder()
                .permissionId("pid")
                .addMeterReadings(new MeterReading("pid", "001", readingEnd))
                .end(end.toLocalDate())
                .granularity(granularity)
                .build();
        when(repository.getByPermissionId("pid")).thenReturn(pr);

        // When
        eventBus.emit(new MeterReadingUpdatedEvent("pid", PermissionProcessStatus.ACCEPTED));

        // Then
        verify(outbox, never()).commit(any());
    }

    private static Stream<Arguments> testAccept_allMeterReadingsAfterEnd_emitsFulfilledEvent() {
        return Stream.of(
                Arguments.of(ZonedDateTime.of(2025, 1, 31, 0, 0, 0, 0, ZoneOffset.UTC), Granularity.PT15M),
                Arguments.of(ZonedDateTime.of(2025, 1, 31, 22, 0, 0, 0, ZoneOffset.UTC), Granularity.P1D)
        );
    }

    private static Stream<Arguments> testAccept_notAllMeterReadingsAfterEnd_emitsNothing() {
        return Stream.of(
                Arguments.of(ZonedDateTime.of(2025, 1, 30, 0, 0, 0, 0, ZoneOffset.UTC), Granularity.PT15M),
                Arguments.of(null, Granularity.PT15M),
                Arguments.of(ZonedDateTime.of(2025, 1, 29, 22, 0, 0, 0, ZoneOffset.UTC), Granularity.P1D)
        );
    }
}
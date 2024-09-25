package energy.eddie.regionconnector.us.green.button.permission.handlers;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.services.FulfillmentService;
import energy.eddie.regionconnector.us.green.button.permission.events.MeterReading;
import energy.eddie.regionconnector.us.green.button.permission.events.PollingStatus;
import energy.eddie.regionconnector.us.green.button.permission.events.UsMeterReadingUpdateEvent;
import energy.eddie.regionconnector.us.green.button.permission.request.GreenButtonPermissionRequest;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeterReadingUpdateHandlerTest {
    @Spy
    private final EventBus eventBus = new EventBusImpl();
    @Mock
    private FulfillmentService fulfillmentService;
    @Mock
    private UsPermissionRequestRepository repository;
    @SuppressWarnings("unused")
    @InjectMocks
    private MeterReadingUpdateHandler handler;

    @Test
    void meterReadingBeforeEnd_doesNotFulfillPermissionRequest() {
        // Given
        var pr = new GreenButtonPermissionRequest(
                "pid",
                "cid",
                "dnid",
                LocalDate.of(2024, 9, 1),
                LocalDate.of(2024, 9, 30),
                Granularity.PT15M,
                PermissionProcessStatus.ACCEPTED,
                ZonedDateTime.of(2024, 9, 1, 0, 0, 0, 0, ZoneOffset.UTC),
                "US",
                "company",
                "http://localhost",
                "scope",
                "1111");
        when(repository.getByPermissionId("pid")).thenReturn(pr);
        var readingDate = ZonedDateTime.of(2024, 9, 10, 0, 0, 0, 0, ZoneOffset.UTC);
        var meterReading = new MeterReading("pid", "usagePoint", readingDate);
        var event = new UsMeterReadingUpdateEvent("pid",
                                                  List.of(meterReading),
                                                  PollingStatus.DATA_READY);
        // When
        eventBus.emit(event);

        // Then
        verify(fulfillmentService, never()).tryFulfillPermissionRequest(pr);
    }

    @Test
    void permissionRequest_withoutMeterReading_doesNotFulfillPermissionRequest() {
        // Given
        var event = new UsMeterReadingUpdateEvent("pid", List.of(), PollingStatus.DATA_READY);

        // When
        eventBus.emit(event);

        // Then
        verify(fulfillmentService, never()).tryFulfillPermissionRequest(any());
    }

    @Test
    void meterReadingAfterEnd_fulfillsPermissionRequest() {
        // Given
        var pr = new GreenButtonPermissionRequest(
                "pid",
                "cid",
                "dnid",
                LocalDate.of(2024, 9, 1),
                LocalDate.of(2024, 9, 30),
                Granularity.PT15M,
                PermissionProcessStatus.ACCEPTED,
                ZonedDateTime.of(2024, 9, 1, 0, 0, 0, 0, ZoneOffset.UTC),
                "US",
                "company",
                "http://localhost",
                "scope",
                "1111");
        when(repository.getByPermissionId("pid")).thenReturn(pr);
        var readingDate = ZonedDateTime.of(2024, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        var meterReading = new MeterReading("pid", "usagePoint", readingDate);
        var event = new UsMeterReadingUpdateEvent("pid", List.of(meterReading), PollingStatus.DATA_READY);
        // When
        eventBus.emit(event);

        // Then
        verify(fulfillmentService).tryFulfillPermissionRequest(pr);
    }

    @Test
    void meterReadingEqualsEnd_fulfillsPermissionRequest() {
        // Given
        var pr = new GreenButtonPermissionRequest(
                "pid",
                "cid",
                "dnid",
                LocalDate.of(2024, 9, 1),
                LocalDate.of(2024, 9, 30),
                Granularity.PT15M,
                PermissionProcessStatus.ACCEPTED,
                ZonedDateTime.of(2024, 9, 1, 0, 0, 0, 0, ZoneOffset.UTC),
                "US",
                "company",
                "http://localhost",
                "scope",
                "1111");
        when(repository.getByPermissionId("pid")).thenReturn(pr);
        var readingDate = ZonedDateTime.of(2024, 9, 30, 23, 59, 59, 0, ZoneOffset.UTC);
        var meterReading = new MeterReading("pid", "usagePoint", readingDate);
        var event = new UsMeterReadingUpdateEvent("pid", List.of(meterReading), PollingStatus.DATA_READY);
        // When
        eventBus.emit(event);

        // Then
        verify(fulfillmentService).tryFulfillPermissionRequest(pr);
    }
}
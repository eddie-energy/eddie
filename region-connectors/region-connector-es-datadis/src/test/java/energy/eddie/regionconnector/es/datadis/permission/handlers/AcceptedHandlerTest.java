package energy.eddie.regionconnector.es.datadis.permission.handlers;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.permission.events.EsAcceptedEvent;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.DistributorCode;
import energy.eddie.regionconnector.es.datadis.persistence.EsPermissionRequestRepository;
import energy.eddie.regionconnector.es.datadis.services.HistoricalDataService;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcceptedHandlerTest {
    @Mock
    private Outbox outbox;
    @Mock
    private HistoricalDataService historicalDataService;
    @Mock
    private EsPermissionRequestRepository repository;
    @Spy
    private EventBus eventBus = new EventBusImpl();
    @InjectMocks
    @SuppressWarnings("unused")
    private AcceptedHandler acceptedHandler;

    @Test
    void testAccept_withUnknownPermissionRequest_doesNothing() {
        // Given
        when(repository.findByPermissionId("pid"))
                .thenReturn(Optional.empty());

        // When
        eventBus.emit(new EsAcceptedEvent("pid", null, null, false));

        // Then
        verifyNoInteractions(outbox);
    }

    @Test
    void testAccept_requestsData() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var pr = new DatadisPermissionRequest(
                "pid",
                "cid",
                "dnid",
                Granularity.PT1H,
                "nif",
                "mid",
                now,
                now.plusDays(10),
                null,
                null,
                null,
                PermissionProcessStatus.ACCEPTED,
                null,
                false,
                ZonedDateTime.now(ZoneOffset.UTC)
        );
        when(repository.findByPermissionId("pid"))
                .thenReturn(Optional.of(pr));

        // When
        eventBus.emit(new EsAcceptedEvent("pid", DistributorCode.VIESGO, 1, true));

        // Then
        verify(historicalDataService).fetchAvailableHistoricalData(assertArg(permissionRequest -> assertAll(
                () -> assertEquals(Optional.of(DistributorCode.VIESGO), permissionRequest.distributorCode()),
                () -> assertEquals(Optional.of(1), permissionRequest.pointType()),
                () -> assertTrue(permissionRequest.productionSupport())
        )));
    }
}
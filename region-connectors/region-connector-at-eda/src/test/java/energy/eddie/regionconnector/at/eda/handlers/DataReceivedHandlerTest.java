package energy.eddie.regionconnector.at.eda.handlers;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.events.DataReceivedEvent;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.services.FulfillmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataReceivedHandlerTest {
    @Spy
    private final EventBus eventBus = new EventBusImpl();
    @Mock
    private FulfillmentService fulfillmentService;
    @Mock
    private AtPermissionRequestRepository repository;
    @InjectMocks
    @SuppressWarnings("unused")
    private DataReceivedHandler handler;


    @Test
    void service_callsFulfill_whenMeteringDataEndIsAfterPermissionRequestEnd() {
        // Given
        LocalDate permissionRequestEnd = LocalDate.now(ZoneOffset.UTC).minusDays(1);
        LocalDate meteringDataEnd = permissionRequestEnd.plusDays(1);
        AtPermissionRequest permissionRequest = createPermissionRequest(permissionRequestEnd);
        when(fulfillmentService.isPermissionRequestFulfilledByDate(any(), any())).thenCallRealMethod();
        when(repository.getByPermissionId("pid")).thenReturn(permissionRequest);

        // When
        eventBus.emit(new DataReceivedEvent("pid", PermissionProcessStatus.ACCEPTED, meteringDataEnd, meteringDataEnd));

        // Then
        verify(fulfillmentService).tryFulfillPermissionRequest(any());
    }


    @Test
    void service_doesNotCallFulfill_whenMeteringDataEndIsEqualPermissionRequestEnd() {
        // Given
        LocalDate permissionRequestEnd = LocalDate.now(ZoneOffset.UTC);
        AtPermissionRequest permissionRequest = createPermissionRequest(permissionRequestEnd);
        when(repository.getByPermissionId("pid")).thenReturn(permissionRequest);
        when(fulfillmentService.isPermissionRequestFulfilledByDate(any(), any())).thenCallRealMethod();

        // When
        eventBus.emit(new DataReceivedEvent("pid",
                                            PermissionProcessStatus.ACCEPTED,
                                            permissionRequestEnd,
                                            permissionRequestEnd));

        // Then
        verify(fulfillmentService, never()).tryFulfillPermissionRequest(any());
    }

    @Test
    void service_doesNotCallFulfill_whenMeteringDataEndIsBeforePermissionRequestEnd() {
        // Given
        LocalDate permissionRequestEnd = LocalDate.now(ZoneOffset.UTC);
        LocalDate meteringDataEnd = permissionRequestEnd.minusDays(1);
        AtPermissionRequest permissionRequest = createPermissionRequest(permissionRequestEnd);
        when(repository.getByPermissionId("pid")).thenReturn(permissionRequest);
        when(fulfillmentService.isPermissionRequestFulfilledByDate(any(), any())).thenCallRealMethod();

        // When
        eventBus.emit(new DataReceivedEvent("pid", PermissionProcessStatus.ACCEPTED, meteringDataEnd, meteringDataEnd));

        // Then
        verify(fulfillmentService, never()).tryFulfillPermissionRequest(any());
    }

    @ParameterizedTest
    @EnumSource(value = PermissionProcessStatus.class, names = {"TERMINATED", "REVOKED", "FULFILLED", "INVALID", "MALFORMED", "EXTERNALLY_TERMINATED", "FAILED_TO_TERMINATE", "REQUIRES_EXTERNAL_TERMINATION"})
    void service_doesNotCallFulfilled_whenInATerminalState(PermissionProcessStatus status) {
        // Given
        LocalDate permissionRequestEnd = LocalDate.now(ZoneOffset.UTC).minusDays(1);
        LocalDate meteringDataEnd = permissionRequestEnd.plusDays(1);

        // When
        eventBus.emit(new DataReceivedEvent("pid", status, meteringDataEnd, permissionRequestEnd));

        // Then
        verify(fulfillmentService, never()).tryFulfillPermissionRequest(any());
    }

    private static EdaPermissionRequest createPermissionRequest(LocalDate meteringDataEnd) {
        return new EdaPermissionRequest(
                "cid",
                "pid",
                "dnid",
                "cmRequestId",
                "convId",
                "mid",
                "dsoId",
                null,
                meteringDataEnd,
                AllowedGranularity.PT15M,
                PermissionProcessStatus.ACCEPTED,
                "",
                "consentId",
                null
        );
    }
}

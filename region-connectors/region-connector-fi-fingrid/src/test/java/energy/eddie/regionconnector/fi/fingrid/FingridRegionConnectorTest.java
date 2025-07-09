package energy.eddie.regionconnector.fi.fingrid;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fi.fingrid.permission.request.FingridPermissionRequest;
import energy.eddie.regionconnector.fi.fingrid.permission.request.FingridPermissionRequestBuilder;
import energy.eddie.regionconnector.fi.fingrid.persistence.FiPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FingridRegionConnectorTest {
    @Mock
    private Outbox outbox;
    @Mock
    private FiPermissionRequestRepository repository;
    @InjectMocks
    private FingridRegionConnector fingridRegionConnector;

    @Test
    void testTerminatePermissionRequest_doesNothingForUnknownPermissionRequest() {
        // Given
        when(repository.findByPermissionId("pid")).thenReturn(Optional.empty());

        // When
        fingridRegionConnector.terminatePermission("pid");

        // Then
        verify(outbox, never()).commit(any());
    }

    @Test
    void testTerminatePermissionRequest_doesNothingForNotAcceptedPermissionRequest() {
        // Given
        when(repository.findByPermissionId("pid"))
                .thenReturn(Optional.of(getPermissionRequest(PermissionProcessStatus.VALIDATED)));

        // When
        fingridRegionConnector.terminatePermission("pid");

        // Then
        verify(outbox, never()).commit(any());
    }

    @Test
    void testTerminatePermissionRequest_emitsTerminated_forAcceptedPermissionRequest() {
        // Given
        when(repository.findByPermissionId("pid"))
                .thenReturn(Optional.of(getPermissionRequest(PermissionProcessStatus.ACCEPTED)));

        // When
        fingridRegionConnector.terminatePermission("pid");

        // Then
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.TERMINATED, event.status())));
    }

    private static @NotNull FingridPermissionRequest getPermissionRequest(PermissionProcessStatus status) {
        var now = LocalDate.now(ZoneOffset.UTC);
        return new FingridPermissionRequestBuilder().setPermissionId("pid")
                                                    .setConnectionId("cid")
                                                    .setDataNeedId("dnid")
                                                    .setStatus(status)
                                                    .setCreated(ZonedDateTime.now(ZoneOffset.UTC))
                                                    .setStart(now)
                                                    .setEnd(now)
                                                    .setCustomerIdentification("cid")
                                                    .setGranularity(Granularity.PT15M)
                                                    .setLastMeterReadings(null)
                                                    .build();
    }
}
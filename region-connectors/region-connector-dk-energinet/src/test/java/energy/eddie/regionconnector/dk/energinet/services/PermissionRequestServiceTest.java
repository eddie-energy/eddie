package energy.eddie.regionconnector.dk.energinet.services;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.persistence.DkPermissionRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionRequestServiceTest {
    @Mock
    private DkPermissionRequestRepository repository;
    @Mock
    @SuppressWarnings("unused")
    private EnerginetCustomerApi customerApi;
    @InjectMocks
    private PermissionRequestService service;

    @Test
    void givenNonExistingId_findConnectionStatusMessageById_returnsEmptyOptional() {
        var permissionId = "NonExistingId";

        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.empty());

        Optional<ConnectionStatusMessage> status = service.findConnectionStatusMessageById(permissionId);
        assertTrue(status.isEmpty());
    }

    @Test
    void givenExistingId_findConnectionStatusMessageById_returnsNonEmptyOptional() {
        // Given
        var permissionId = "a0ec0288-7eaf-4aa2-8387-77c6413cfd31";
        var connectionId = "connId";
        var dataNeedId = "dataNeedId";
        var permissionRequest = new EnerginetPermissionRequest(
                permissionId,
                connectionId,
                dataNeedId,
                "meteringPointId",
                "refreshToken",
                LocalDate.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC),
                Granularity.P1D,
                "accessToken",
                PermissionProcessStatus.ACCEPTED,
                ZonedDateTime.now(ZoneOffset.UTC)
        );
        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.of(permissionRequest));

        // When
        Optional<ConnectionStatusMessage> optionalStatus = service.findConnectionStatusMessageById(permissionId);

        // Then
        assertTrue(optionalStatus.isPresent());
        var status = optionalStatus.get();
        assertEquals(permissionId, status.permissionId());
        assertEquals(connectionId, status.connectionId());
        assertEquals(dataNeedId, status.dataNeedId());
        assertEquals(PermissionProcessStatus.ACCEPTED, status.status());
        assertNotNull(status.timestamp());
    }
}

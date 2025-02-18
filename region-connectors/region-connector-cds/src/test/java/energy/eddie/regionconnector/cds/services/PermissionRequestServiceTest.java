package energy.eddie.regionconnector.cds.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequest;
import energy.eddie.regionconnector.cds.persistence.CdsPermissionRequestRepository;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
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
    private CdsPermissionRequestRepository repository;
    @InjectMocks
    private PermissionRequestService service;

    @Test
    void testGetConnectionStatusMessage_onUnknownPermissionId_throws() {
        // Given
        when(repository.findByPermissionId("pid")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(PermissionNotFoundException.class, () -> service.getConnectionStatusMessage("pid"));
    }

    @Test
    void testGetConnectionStatusMessage_returnsConnectionStatusMessage() throws PermissionNotFoundException {
        // Given
        var today = LocalDate.now(ZoneOffset.UTC);
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var pr = new CdsPermissionRequest(
                "pid",
                "cid",
                "dnid",
                PermissionProcessStatus.ACCEPTED,
                1L,
                now,
                today,
                today,
                "state"
        );
        when(repository.findByPermissionId("pid")).thenReturn(Optional.of(pr));

        // When
        var res = service.getConnectionStatusMessage("pid");

        // Then
        assertAll(
                () -> assertEquals("pid", res.permissionId()),
                () -> assertEquals("cid", res.connectionId()),
                () -> assertEquals("dnid", res.dataNeedId()),
                () -> assertEquals(PermissionProcessStatus.ACCEPTED, res.status()),
                () -> assertEquals(pr.dataSourceInformation(), res.dataSourceInformation()),
                () -> assertEquals("", res.message()),
                () -> assertNull(res.additionalInformation())
        );
    }
}
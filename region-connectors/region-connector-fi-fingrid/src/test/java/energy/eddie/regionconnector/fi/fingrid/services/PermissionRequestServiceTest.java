package energy.eddie.regionconnector.fi.fingrid.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fi.fingrid.permission.request.FingridPermissionRequestBuilder;
import energy.eddie.regionconnector.fi.fingrid.persistence.FiPermissionRequestRepository;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionRequestServiceTest {
    @Mock
    private FiPermissionRequestRepository repository;
    @InjectMocks
    private PermissionRequestService service;

    @Test
    void connectionStatusMessage_mapsPermissionRequest() throws PermissionNotFoundException {
        // Given
        when(repository.findByPermissionId("pid"))
                .thenReturn(Optional.of(
                        new FingridPermissionRequestBuilder().setPermissionId("pid")
                                                             .setConnectionId("cid")
                                                             .setDataNeedId("dnid")
                                                             .setStatus(PermissionProcessStatus.CREATED)
                                                             .setCreated(ZonedDateTime.now(ZoneOffset.UTC))
                                                             .setStart(null)
                                                             .setEnd(null)
                                                             .setCustomerIdentification("identifier")
                                                             .setGranularity(Granularity.PT1H)
                                                             .setLastMeterReadings(null)
                                                             .createFingridPermissionRequest()
                ));

        // When
        var csm = service.connectionStatusMessage("pid");

        // Then
        assertAll(
                () -> assertEquals("pid", csm.permissionId()),
                () -> assertEquals("cid", csm.connectionId()),
                () -> assertEquals("dnid", csm.dataNeedId()),
                () -> assertEquals(PermissionProcessStatus.CREATED, csm.status())
        );
    }

    @Test
    void connectionStatusMessage_throwsOnUnknownPermissionRequest() {
        // Given
        when(repository.findByPermissionId("pid"))
                .thenReturn(Optional.empty());

        // When, Then
        assertThrows(PermissionNotFoundException.class, () -> service.connectionStatusMessage("pid"));
    }
}
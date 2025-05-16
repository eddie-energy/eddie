package energy.eddie.regionconnector.dk.energinet;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetPermissionRequestBuilder;
import energy.eddie.regionconnector.dk.energinet.persistence.DkPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnerginetRegionConnectorTest {
    @Mock
    private Outbox outbox;
    @Mock
    private DkPermissionRequestRepository repository;
    @InjectMocks
    private EnerginetRegionConnector rc;

    @Test
    void getMetadata_returnsExpected() {
        // Given
        // When
        var result = rc.getMetadata();

        assertEquals(EnerginetRegionConnectorMetadata.getInstance(), result);
    }

    @Test
    void terminatePermission_withNonExistentPermissionId_doesNotThrow() {
        // Given
        when(repository.findByPermissionId(anyString())).thenReturn(Optional.empty());

        // When
        // Then
        assertDoesNotThrow(() -> rc.terminatePermission("pid"));
    }

    @Test
    void terminatePermission_withExistingPermissionId_terminates() {
        // Given
        var permissionRequest = new EnerginetPermissionRequestBuilder()
                .setPermissionId("pid")
                .setStatus(PermissionProcessStatus.ACCEPTED)
                .build();
        when(repository.findByPermissionId(anyString())).thenReturn(Optional.of(permissionRequest));

        // When
        rc.terminatePermission("pid");

        // Then
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.TERMINATED, event.status())));
    }

    @Test
    void terminatePermission_withWrongState_doesNotThrow() {
        // Given
        var request = new EnerginetPermissionRequestBuilder()
                .setPermissionId("pid")
                .setStatus(PermissionProcessStatus.CREATED)
                .build();
        when(repository.findByPermissionId(anyString())).thenReturn(Optional.of(request));

        // When
        // Then
        assertDoesNotThrow(() -> rc.terminatePermission("pid"));
    }
}

package energy.eddie.regionconnector.fr.enedis;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequestBuilder;
import energy.eddie.regionconnector.fr.enedis.persistence.FrPermissionRequestRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnedisRegionConnectorTest {
    @Mock
    private FrPermissionRequestRepository repository;
    @Mock
    private Outbox outbox;
    @InjectMocks
    private EnedisRegionConnector rc;

    @Test
    void getMetadata_returnsExpected() {
        // Given
        // When
        var res = rc.getMetadata();

        // Then
        assertEquals(EnedisRegionConnectorMetadata.getInstance(), res);
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
        var request = new EnedisPermissionRequestBuilder()
                .setPermissionId("pid")
                .setStatus(PermissionProcessStatus.ACCEPTED)
                .create();
        when(repository.findByPermissionId(anyString()))
                .thenReturn(Optional.of(request));

        // When
        rc.terminatePermission("pid");
        // Then
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.TERMINATED, event.status())));
    }

    @Test
    void terminatePermission_withWrongState_doesNotThrow() {
        // Given
        var request = new EnedisPermissionRequestBuilder()
                .setPermissionId("pid")
                .setStatus(PermissionProcessStatus.CREATED)
                .create();
        when(repository.findByPermissionId(anyString())).thenReturn(Optional.of(request));

        // When
        // Then
        assertDoesNotThrow(() -> rc.terminatePermission("pid"));
        verify(outbox, never()).commit(any());
    }
}

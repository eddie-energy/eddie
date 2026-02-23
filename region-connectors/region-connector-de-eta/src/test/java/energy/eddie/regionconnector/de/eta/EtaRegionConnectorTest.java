package energy.eddie.regionconnector.de.eta;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.persistence.DePermissionRequestRepository;
import energy.eddie.regionconnector.de.eta.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EtaRegionConnectorTest {

    @Mock
    private DePermissionRequestRepository repository;

    @Mock
    private Outbox outbox;

    private EtaRegionConnector connector;

    @BeforeEach
    void setUp() {
        connector = new EtaRegionConnector(repository, outbox);
    }

    @Test
    void getMetadataShouldReturnCorrectMetadata() {
        var metadata = connector.getMetadata();
        assertThat(metadata).isNotNull();
        assertThat(metadata.id()).isEqualTo(EtaRegionConnectorMetadata.REGION_CONNECTOR_ID);
    }

    @Test
    void terminatePermissionWhenPermissionExistsShouldCommitEvents() {
        String permissionId = "test-permission-id";
        DePermissionRequest request = mock(DePermissionRequest.class);
        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.of(request));

        connector.terminatePermission(permissionId);

        ArgumentCaptor<SimpleEvent> eventCaptor = ArgumentCaptor.forClass(SimpleEvent.class);
        verify(outbox, times(2)).commit(eventCaptor.capture());

        var events = eventCaptor.getAllValues();
        assertThat(events).hasSize(2);
        assertThat(events.get(0).permissionId()).isEqualTo(permissionId);
        assertThat(events.get(0).status()).isEqualTo(PermissionProcessStatus.TERMINATED);
        assertThat(events.get(1).permissionId()).isEqualTo(permissionId);
        assertThat(events.get(1).status()).isEqualTo(PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION);
    }

    @Test
    void terminatePermissionWhenPermissionDoesNotExistShouldNotCommitEvents() {
        String permissionId = "non-existent-id";
        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.empty());

        connector.terminatePermission(permissionId);

        verify(outbox, never()).commit(any());
    }

    @Test
    void constructorShouldLogInitialization() {
        // Just to cover the constructor and the log line
        EtaRegionConnector newConnector = new EtaRegionConnector(repository, outbox);
        assertThat(newConnector).isNotNull();
    }
}

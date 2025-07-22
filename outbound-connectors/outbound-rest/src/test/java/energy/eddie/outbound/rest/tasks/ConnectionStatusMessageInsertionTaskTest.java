package energy.eddie.outbound.rest.tasks;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.outbound.rest.connectors.AgnosticConnector;
import energy.eddie.outbound.rest.model.ConnectionStatusMessageModel;
import energy.eddie.outbound.rest.persistence.ConnectionStatusMessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConnectionStatusMessageInsertionTaskTest {
    @Mock
    private ConnectionStatusMessageRepository repository;
    @Mock
    private AgnosticConnector connector;

    @Test
    void insertsConnectionStatusMessages() {
        // Given
        var csm = new ConnectionStatusMessage("cid", "pid", "dnid", null, PermissionProcessStatus.CREATED);
        when(connector.getConnectionStatusMessageStream())
                .thenReturn(Flux.just(csm));

        // When
        new ConnectionStatusMessageInsertionTask(connector, repository);

        // Then
        verify(repository).save(new ConnectionStatusMessageModel(csm));
    }
}
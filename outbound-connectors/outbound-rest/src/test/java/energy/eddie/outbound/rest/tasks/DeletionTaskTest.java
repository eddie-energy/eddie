package energy.eddie.outbound.rest.tasks;

import energy.eddie.outbound.rest.config.RestOutboundConnectorConfiguration;
import energy.eddie.outbound.rest.model.ConnectionStatusMessageModel;
import energy.eddie.outbound.rest.persistence.ConnectionStatusMessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.Duration;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeletionTaskTest {
    @Spy
    @SuppressWarnings("unused")
    private final RestOutboundConnectorConfiguration config = new RestOutboundConnectorConfiguration(Duration.ZERO);
    @Mock
    private ConnectionStatusMessageRepository repository;
    @InjectMocks
    private DeletionTask<ConnectionStatusMessageModel> task;

    @Test
    void deleteConnectionStatusMessages_deletes() {
        // Given
        // When
        task.delete();

        // Then
        verify(repository).delete(ArgumentMatchers.<Specification<ConnectionStatusMessageModel>>any());
    }
}
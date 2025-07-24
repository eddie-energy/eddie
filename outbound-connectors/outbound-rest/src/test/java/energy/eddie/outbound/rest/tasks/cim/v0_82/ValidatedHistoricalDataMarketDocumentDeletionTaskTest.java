package energy.eddie.outbound.rest.tasks.cim.v0_82;

import energy.eddie.outbound.rest.config.RestOutboundConnectorConfiguration;
import energy.eddie.outbound.rest.model.cim.v0_82.ValidatedHistoricalDataMarketDocumentModel;
import energy.eddie.outbound.rest.persistence.cim.v0_82.ValidatedHistoricalDataMarketDocumentRepository;
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
class ValidatedHistoricalDataMarketDocumentDeletionTaskTest {
    @Spy
    @SuppressWarnings("unused")
    private final RestOutboundConnectorConfiguration config = new RestOutboundConnectorConfiguration(Duration.ZERO);
    @Mock
    private ValidatedHistoricalDataMarketDocumentRepository repository;
    @InjectMocks
    private ValidatedHistoricalDataMarketDocumentDeletionTask task;

    @Test
    void deleteValidatedHistoricalData_deletes() {
        // Given
        // When
        task.delete();

        // Then
        verify(repository).delete(ArgumentMatchers.<Specification<ValidatedHistoricalDataMarketDocumentModel>>any());
    }
}
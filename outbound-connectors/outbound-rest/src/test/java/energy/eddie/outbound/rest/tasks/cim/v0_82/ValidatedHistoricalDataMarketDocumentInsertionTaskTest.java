package energy.eddie.outbound.rest.tasks.cim.v0_82;

import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.outbound.rest.connectors.cim.v0_82.CimConnector;
import energy.eddie.outbound.rest.persistence.cim.v0_82.ValidatedHistoricalDataMarketDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidatedHistoricalDataMarketDocumentInsertionTaskTest {
    @Mock
    private ValidatedHistoricalDataMarketDocumentRepository repository;
    @Mock
    private CimConnector connector;

    @Test
    void insertsConnectionStatusMessages() {
        // Given
        var doc = new ValidatedHistoricalDataEnvelope();
        when(connector.getHistoricalDataMarketDocumentStream())
                .thenReturn(Flux.just(doc));

        // When
        new ValidatedHistoricalDataMarketDocumentInsertionTask(repository, connector);

        // Then
        verify(repository).save(any());
    }
}
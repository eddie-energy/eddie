package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.DatadisPermissionRequestBuilder;
import energy.eddie.regionconnector.es.datadis.permission.events.EsValidatedEvent;
import energy.eddie.regionconnector.es.datadis.persistence.EsPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetryServiceTest {
    @Mock
    private Outbox outbox;
    @Mock
    private EsPermissionRequestRepository repository;
    @InjectMocks
    private RetryService retryService;

    @Test
    void testRetry_emitsPermissionEvent() {
        // Given
        var pr = new DatadisPermissionRequestBuilder().build();

        when(repository.findByStatus(PermissionProcessStatus.UNABLE_TO_SEND))
                .thenReturn(List.of(pr));

        // When
        retryService.retry();

        // Then
        verify(outbox).commit(isA(EsValidatedEvent.class));
    }
}

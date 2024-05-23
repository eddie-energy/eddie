package energy.eddie.regionconnector.dk.energinet.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.persistence.DkPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetryServiceTest {
    @Mock
    private Outbox outbox;
    @Mock
    private DkPermissionRequestRepository repository;
    @InjectMocks
    private RetryService retryService;

    @Test
    void testRetry_emitsUnableToSendPermissionRequests_ifFound() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        when(repository.findAllByStatus(PermissionProcessStatus.UNABLE_TO_SEND))
                .thenReturn(List.of(
                        new EnerginetPermissionRequest(
                                "pid",
                                "cid",
                                "dnid",
                                "mid",
                                "refresh",
                                now,
                                now,
                                Granularity.PT1H,
                                "access",
                                PermissionProcessStatus.UNABLE_TO_SEND,
                                ZonedDateTime.now(ZoneOffset.UTC)
                        )
                ));

        // When
        retryService.retry();

        // Then
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.VALIDATED, event.status())));
    }
}
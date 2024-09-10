package energy.eddie.regionconnector.us.green.button.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.us.green.button.permission.events.UsStartPollingEvent;
import energy.eddie.regionconnector.us.green.button.permission.request.GreenButtonPermissionRequest;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PollingRetryServiceTest {

    @Mock
    private UsPermissionRequestRepository repository;
    @Mock
    private Outbox outbox;
    @InjectMocks
    private PollingRetryService pollingRetryService;

    @Test
    void polling_emitsPollingEvent() {
        // Given
        var start = LocalDate.of(2024, 9, 3);
        var pr = new GreenButtonPermissionRequest(
                "pid",
                "cid",
                "dnid",
                start,
                LocalDate.of(2024, 9, 4),
                Granularity.PT15M,
                PermissionProcessStatus.ACCEPTED,
                ZonedDateTime.of(start, LocalTime.MIDNIGHT, ZoneOffset.UTC),
                "US",
                "company",
                "http://localhost",
                "scope"
        );
        when(repository.findAllAcceptedAndNotPolled())
                .thenReturn(List.of(pr));

        // Then
        pollingRetryService.polling();

        // Then
        verify(outbox).commit(isA(UsStartPollingEvent.class));
    }
}
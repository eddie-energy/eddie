package energy.eddie.regionconnector.nl.mijn.aansluiting.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.request.MijnAansluitingPermissionRequest;
import energy.eddie.regionconnector.nl.mijn.aansluiting.persistence.NlPermissionRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FutureDataServiceTest {

    @Mock
    private PollingService pollingService;
    @Mock
    private NlPermissionRequestRepository repository;
    @InjectMocks
    private FutureDataService futureDataService;

    @Test
    void testScheduleNextMeterReading_pollsData() {
        // Given
        var pr = new MijnAansluitingPermissionRequest(
                "pid",
                "cid",
                "dnid",
                PermissionProcessStatus.ACCEPTED,
                "",
                "",
                ZonedDateTime.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC).minusDays(10),
                LocalDate.now(ZoneOffset.UTC).minusDays(1),
                Granularity.P1D
        );
        when(repository.findByStatus(PermissionProcessStatus.ACCEPTED))
                .thenReturn(List.of(pr));

        // When
        futureDataService.scheduleNextMeterReading();

        // Then
        verify(pollingService).fetchConsumptionData(pr);
    }
}
package energy.eddie.regionconnector.fi.fingrid.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.fi.fingrid.permission.request.FingridPermissionRequest;
import energy.eddie.regionconnector.fi.fingrid.persistence.FiPermissionRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FutureDataServiceTest {
    @Mock
    private PollingService pollingService;
    @Mock
    private FiPermissionRequestRepository repository;
    @Mock
    private DataNeedsService dataNeedsService;
    @InjectMocks
    private FutureDataService futureDataService;

    @Test
    void testScheduleNextMeterReading_pollsData() {
        // Given
        var permissionRequest = getPermissionRequest();
        when(repository.findByStatus(PermissionProcessStatus.ACCEPTED))
                .thenReturn(List.of(permissionRequest));
        when(dataNeedsService.getById("dnid"))
                .thenReturn(new ValidatedHistoricalDataDataNeed(
                        new RelativeDuration(Period.ofDays(-10), Period.ofDays(-1), null),
                        EnergyType.ELECTRICITY,
                        Granularity.P1D,
                        Granularity.P1D
                ));

        // When
        futureDataService.schedulePolling();

        // Then
        verify(pollingService).pollTimeSeriesData(permissionRequest);
    }

    @Test
    void testScheduleNextMeterReading_withAccountingPointDataNeed_doesNotPollData() {
        // Given
        when(repository.findByStatus(PermissionProcessStatus.ACCEPTED))
                .thenReturn(List.of(getPermissionRequest()));
        when(dataNeedsService.getById("dnid"))
                .thenReturn(new AccountingPointDataNeed());

        // When
        futureDataService.schedulePolling();

        // Then
        verify(pollingService, never()).pollTimeSeriesData(any());
    }

    private static FingridPermissionRequest getPermissionRequest() {
        var now = LocalDate.now(ZoneOffset.UTC);
        return new FingridPermissionRequest(
                "pid",
                "cid",
                "dnid",
                PermissionProcessStatus.ACCEPTED,
                ZonedDateTime.now(ZoneOffset.UTC),
                now,
                now,
                "cid",
                "ean",
                Granularity.PT15M,
                null
        );
    }
}
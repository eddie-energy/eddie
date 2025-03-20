package energy.eddie.regionconnector.fi.fingrid.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.fi.fingrid.FingridRegionConnectorMetadata;
import energy.eddie.regionconnector.fi.fingrid.permission.request.FingridPermissionRequest;
import energy.eddie.regionconnector.fi.fingrid.persistence.FiPermissionRequestRepository;
import energy.eddie.regionconnector.shared.services.CommonFutureDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FutureDataServiceTest {
    @Mock
    private DataNeedsService dataNeedsService;
    @Spy
    @InjectMocks
    private PollingService pollingService;
    @Mock
    private FiPermissionRequestRepository repository;
    @Mock
    FingridRegionConnectorMetadata metadata;
    private CommonFutureDataService<FingridPermissionRequest> futureDataService;

    @BeforeEach
    void setup() {
        when(metadata.timeZone()).thenReturn(ZoneId.of("Europe/Helsinki"));
        futureDataService = new CommonFutureDataService<>(pollingService, repository, "0 0 17 * * *", metadata);
    }


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
        futureDataService.fetchMeterData();

        // Then
        verify(pollingService).pollTimeSeriesData(permissionRequest);
    }

    @Test
    void testScheduleNextMeterReading_withAccountingPointDataNeed_doesNotPollData() {
        // Given
        when(repository.findByStatus(PermissionProcessStatus.ACCEPTED))
                .thenReturn(List.of(getPermissionRequest()));

        // When
        futureDataService.fetchMeterData();

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
package energy.eddie.regionconnector.nl.mijn.aansluiting.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.nl.mijn.aansluiting.MijnAansluitingRegionConnector;
import energy.eddie.regionconnector.nl.mijn.aansluiting.MijnAansluitingRegionConnectorMetadata;
import energy.eddie.regionconnector.nl.mijn.aansluiting.api.NlPermissionRequest;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.request.MijnAansluitingPermissionRequest;
import energy.eddie.regionconnector.nl.mijn.aansluiting.persistence.NlPermissionRequestRepository;
import energy.eddie.regionconnector.shared.services.CommonFutureDataService;
import energy.eddie.regionconnector.shared.services.CommonPollingService;
import org.junit.jupiter.api.BeforeEach;
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

    public static final MijnAansluitingPermissionRequest PERMISSION_REQUEST = new MijnAansluitingPermissionRequest("pid", "cid", "dnid", PermissionProcessStatus.ACCEPTED, "", "", ZonedDateTime.now(ZoneOffset.UTC), LocalDate.now(ZoneOffset.UTC).minusDays(10), LocalDate.now(ZoneOffset.UTC).minusDays(1), Granularity.P1D);
    @Mock
    private CommonPollingService<NlPermissionRequest> pollingService;
    @Mock
    private NlPermissionRequestRepository<NlPermissionRequest> repository;
    @Mock
    private DataNeedsService dataNeedsService;
    @Mock
    private MijnAansluitingRegionConnectorMetadata metadata; // without this metadata, the region connector cannot be mocked correctly
    @InjectMocks
    private MijnAansluitingRegionConnector regionConnector;
    private CommonFutureDataService<NlPermissionRequest> futureDataService;

    @BeforeEach
    public void setup() {
        futureDataService = new CommonFutureDataService<>(pollingService, repository, dataNeedsService, null, "Europe/Amsterdam", "0 0 17 * * *", regionConnector);
    }

    @Test
    void testScheduleNextMeterReading_pollsData() {
        // Given
        when(repository.findByStatus(PermissionProcessStatus.ACCEPTED)).thenReturn(List.of(PERMISSION_REQUEST));
        when(dataNeedsService.getById("dnid")).thenReturn(new ValidatedHistoricalDataDataNeed(new RelativeDuration(Period.ofDays(-10), Period.ofDays(-1), null), EnergyType.ELECTRICITY, Granularity.P1D, Granularity.P1D));

        // When
        futureDataService.fetchMeterData();

        // Then
        verify(pollingService).pollTimeSeriesData(PERMISSION_REQUEST);
    }

    @Test
    void testScheduleNextMeterReading_withAccountingPointDataNeed_doesNotPollData() {
        // Given
        when(repository.findByStatus(PermissionProcessStatus.ACCEPTED)).thenReturn(List.of(PERMISSION_REQUEST));
        when(dataNeedsService.getById("dnid")).thenReturn(new AccountingPointDataNeed());

        // When
        futureDataService.fetchMeterData();

        // Then
        verify(pollingService, never()).pollTimeSeriesData(PERMISSION_REQUEST);
    }
}
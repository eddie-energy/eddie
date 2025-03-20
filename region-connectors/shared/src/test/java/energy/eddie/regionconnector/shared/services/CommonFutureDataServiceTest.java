package energy.eddie.regionconnector.shared.services;

import energy.eddie.api.agnostic.DataSourceInformation;
import energy.eddie.api.agnostic.process.model.MeterReadingPermissionRequest;
import energy.eddie.api.agnostic.process.model.persistence.StatusPermissionRequestRepository;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.RegionConnectorMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommonFutureDataServiceTest {
    @Mock
    private CommonPollingService<MeterReadingPermissionRequest> pollingService;
    @Mock
    private StatusPermissionRequestRepository<MeterReadingPermissionRequest> repository;
    @Mock
    RegionConnectorMetadata metadata;
    private CommonFutureDataService<MeterReadingPermissionRequest> service;

    @BeforeEach
    void setUp() {
        when(metadata.timeZone()).thenReturn(ZoneId.of("Europe/Brussels"));
        service = new CommonFutureDataService<>(pollingService, repository, "0 0 17 * * *", metadata);
    }

    static MeterReadingPermissionRequest createPermissionRequest(LocalDate startDate,
                                                                 LocalDate endDate,
                                                                 Optional<LocalDate> latestMeterReading) {
        return new MeterReadingPermissionRequest() {

            @Override
            public String permissionId() {
                return "pID";
            }

            @Override
            public String connectionId() {
                return "cID";
            }

            @Override
            public String dataNeedId() {
                return "dnID";
            }

            @Override
            public PermissionProcessStatus status() {
                return PermissionProcessStatus.ACCEPTED;
            }

            @Override
            public DataSourceInformation dataSourceInformation() {
                return null;
            }

            @Override
            public ZonedDateTime created() {
                return ZonedDateTime.now(ZoneOffset.UTC);
            }

            @Override
            public LocalDate start() {
                return startDate;
            }

            @Override
            public LocalDate end() {
                return endDate;
            }

            @Override
            public Optional<LocalDate> latestMeterReadingEndDate() {
                return latestMeterReading;
            }
        };
    }

    @Test
    void fetchMeterData_doesNotPollDataWithoutPermissions() {
        // Given
        when(repository.findByStatus(PermissionProcessStatus.ACCEPTED)).thenReturn(Collections.emptyList());

        // When
        service.fetchMeterData();

        // Then
        verify(repository).findByStatus(PermissionProcessStatus.ACCEPTED);
        verifyNoMoreInteractions(pollingService);
    }

    @Test
    void fetchMeterReadings_invokesPollingService_forActivePermissionsThatNeedToBePolled(
    ) {
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Brussels"));
        var validPr1 = createPermissionRequest(today.minusDays(1), today, Optional.empty()); //Permission: start = yesterday, end = today, no latest meter reading
        var validPr2 = createPermissionRequest(today.minusDays(2), today, Optional.of(today.minusDays(1))); //Permission: start = 2 days ago, end = today, latest meter reading = yesterday
        // Given
        when(repository.findByStatus(PermissionProcessStatus.ACCEPTED))
                .thenReturn(List.of(validPr1, validPr2));
        when(pollingService.isActiveAndNeedsToBeFetched(validPr1)).thenReturn(true);
        when(pollingService.isActiveAndNeedsToBeFetched(validPr2)).thenReturn(true);

        // When
        service.fetchMeterData();

        // Then
        verify(repository).findByStatus(PermissionProcessStatus.ACCEPTED);
        verify(pollingService).pollTimeSeriesData(validPr1);
        verify(pollingService).pollTimeSeriesData(validPr2);
    }

    @Test
    void fetchMeterReadings_doesNotInvokePollingService_IfPermissionIsActiveButDoesNotNeedToBeFetched(
    ) {
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Brussels"));
        var invalidPr1 = createPermissionRequest(today.minusDays(1), today.plusDays(1), Optional.of(today)); //Permission: start = yesterday, end = tomorrow, latest meter reading = today, data is already up to date
        var invalidPr2 = createPermissionRequest(today.plusDays(1), today.plusDays(2), Optional.empty()); //Permission: start = tomorrow, end = day after tomorrow, inactive
        // Given
        when(repository.findByStatus(PermissionProcessStatus.ACCEPTED))
                .thenReturn(List.of(invalidPr1, invalidPr2));
        when(pollingService.isActiveAndNeedsToBeFetched(invalidPr1)).thenReturn(false);
        when(pollingService.isActiveAndNeedsToBeFetched(invalidPr2)).thenReturn(false);

        // Execute
        service.fetchMeterData();

        // Then
        verify(repository).findByStatus(PermissionProcessStatus.ACCEPTED);
        verify(pollingService).isActiveAndNeedsToBeFetched(invalidPr1);
        verify(pollingService).isActiveAndNeedsToBeFetched(invalidPr2);
        verifyNoMoreInteractions(pollingService);
    }
}

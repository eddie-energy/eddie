package energy.eddie.regionconnector.shared.services;

import energy.eddie.api.agnostic.DataSourceInformation;
import energy.eddie.api.agnostic.data.needs.AccountingPointDataNeedResult;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.data.needs.Timeframe;
import energy.eddie.api.agnostic.data.needs.ValidatedHistoricalDataDataNeedResult;
import energy.eddie.api.agnostic.process.model.MeterReadingPermissionRequest;
import energy.eddie.api.agnostic.process.model.persistence.StatusPermissionRequestRepository;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.dataneeds.needs.DataNeed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;

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
    private RegionConnectorMetadata metadata;
    @Mock
    private CommonPollingService<MeterReadingPermissionRequest> pollingService;
    @Mock
    private StatusPermissionRequestRepository<MeterReadingPermissionRequest> repository;
    @Mock
    private DataNeedCalculationService<DataNeed> calculationService;
    private CommonFutureDataService<MeterReadingPermissionRequest> service;

    @BeforeEach
    void setUp() {
        when(metadata.timeZone()).thenReturn(ZoneId.of("Europe/Brussels"));
        service = new CommonFutureDataService<>(pollingService,
                                                repository,
                                                "0 0 17 * * *",
                                                metadata,
                                                new SimpleAsyncTaskScheduler(),
                                                calculationService);
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
    void fetchMeterReadings_invokesPollingService_forActivePermissionsThatNeedToBePolled() {
        // Given
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Brussels"));
        var validPr1 = createPermissionRequest(today.minusDays(1),
                                               today,
                                               Optional.empty()); //Permission: start = yesterday, end = today, no latest meter reading
        var validPr2 = createPermissionRequest(today.minusDays(2),
                                               today,
                                               Optional.of(today.minusDays(1))); //Permission: start = 2 days ago, end = today, latest meter reading = yesterday
        when(repository.findByStatus(PermissionProcessStatus.ACCEPTED))
                .thenReturn(List.of(validPr1, validPr2));
        when(pollingService.isActiveAndNeedsToBeFetched(validPr1)).thenReturn(true);
        when(pollingService.isActiveAndNeedsToBeFetched(validPr2)).thenReturn(true);
        when(calculationService.calculate("dnID"))
                .thenReturn(createValidatedHistoricalDataNeedResult());

        // When
        service.fetchMeterData();

        // Then
        verify(repository).findByStatus(PermissionProcessStatus.ACCEPTED);
        verify(pollingService).pollTimeSeriesData(validPr1);
        verify(pollingService).pollTimeSeriesData(validPr2);
    }

    @Test
    void fetchMeterReadings_doesNotInvokePollingService_IfPermissionIsActiveButDoesNotNeedToBeFetched() {
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Brussels"));
        var invalidPr1 = createPermissionRequest(today.minusDays(1),
                                                 today.plusDays(1),
                                                 Optional.of(today)); //Permission: start = yesterday, end = tomorrow, latest meter reading = today, data is already up to date
        var invalidPr2 = createPermissionRequest(today.plusDays(1),
                                                 today.plusDays(2),
                                                 Optional.empty()); //Permission: start = tomorrow, end = day after tomorrow, inactive
        // Given
        when(repository.findByStatus(PermissionProcessStatus.ACCEPTED))
                .thenReturn(List.of(invalidPr1, invalidPr2));
        when(pollingService.isActiveAndNeedsToBeFetched(invalidPr1)).thenReturn(false);
        when(pollingService.isActiveAndNeedsToBeFetched(invalidPr2)).thenReturn(false);
        when(calculationService.calculate("dnID")).thenReturn(createValidatedHistoricalDataNeedResult());

        // Execute
        service.fetchMeterData();

        // Then
        verify(repository).findByStatus(PermissionProcessStatus.ACCEPTED);
        verify(pollingService).isActiveAndNeedsToBeFetched(invalidPr1);
        verify(pollingService).isActiveAndNeedsToBeFetched(invalidPr2);
        verifyNoMoreInteractions(pollingService);
    }

    @Test
    void fetchMeterReadings_doesNotInvokePollingService_forPermissionRequestWithWrongDataNeed() {
        // Given
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Brussels"));
        var validPr = createPermissionRequest(today.minusDays(1),
                                               today,
                                               Optional.empty()); //Permission: start = yesterday, end = today, no latest meter reading
        when(repository.findByStatus(PermissionProcessStatus.ACCEPTED))
                .thenReturn(List.of(validPr ));
        when(calculationService.calculate("dnID"))
                .thenReturn(new AccountingPointDataNeedResult(new Timeframe(today, today)));

        // When
        service.fetchMeterData();

        // Then
        verify(pollingService, never()).pollTimeSeriesData(any());
    }

    private static ValidatedHistoricalDataDataNeedResult createValidatedHistoricalDataNeedResult() {
        var today = LocalDate.now(ZoneOffset.UTC);
        return new ValidatedHistoricalDataDataNeedResult(List.of(),
                                                         new Timeframe(today, today),
                                                         new Timeframe(today, today));
    }

    private static MeterReadingPermissionRequest createPermissionRequest(
            LocalDate startDate,
            LocalDate endDate,
            @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<LocalDate> latestMeterReading
    ) {
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
}

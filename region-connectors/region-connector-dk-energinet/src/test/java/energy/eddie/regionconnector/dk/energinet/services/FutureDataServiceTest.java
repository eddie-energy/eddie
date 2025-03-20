package energy.eddie.regionconnector.dk.energinet.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.persistence.DkPermissionRequestRepository;
import energy.eddie.regionconnector.shared.services.CommonFutureDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata.DK_ZONE_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FutureDataServiceTest {
    @Mock
    private DataNeedsService dataNeedsService;
    @Mock
    private EnerginetCustomerApi energinetCustomerApi;
    @Mock
    private ObjectMapper objectMapper; //this is needed to mock the PollingService
    @Spy
    @InjectMocks
    private PollingService pollingService;
    @Mock
    private DkPermissionRequestRepository repository;
    @Mock
    private EnerginetRegionConnectorMetadata metadata;
    private CommonFutureDataService<DkEnerginetPermissionRequest> futureDataService;

    @BeforeEach
    void setup() {
        when(metadata.timeZone()).thenReturn(ZoneId.of("Europe/Copenhagen"));
        futureDataService = new CommonFutureDataService<>(pollingService, repository, "0 0 17 * * *", metadata);
    }

    @Test
    void testScheduleNextMeterReading_pollsData() {
        // Given
        var permissionRequest = getPermissionRequest();
        when(repository.findByStatus(PermissionProcessStatus.ACCEPTED))
                .thenReturn(List.of(permissionRequest));
        when(dataNeedsService.findById("dnid"))
                .thenReturn(Optional.of(new ValidatedHistoricalDataDataNeed(
                        new RelativeDuration(Period.ofDays(-10), Period.ofDays(-1), null),
                        EnergyType.ELECTRICITY,
                        Granularity.P1D,
                        Granularity.P1D
                )));
        when(energinetCustomerApi.accessToken("token")).thenReturn(Mono.just("return"));

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

    private static EnerginetPermissionRequest getPermissionRequest() {
        var start = LocalDate.now(DK_ZONE_ID).minusDays(10);
        var end = start.plusDays(5);
        var connectionId = "connId";
        var dataNeedId = "dnid";
        var refreshToken = "token";
        var meteringPoint = "meteringPoint";
        return new EnerginetPermissionRequest(
                UUID.randomUUID().toString(),
                connectionId,
                dataNeedId,
                meteringPoint,
                refreshToken,
                start,
                end,
                Granularity.PT1H,
                null,
                PermissionProcessStatus.ACCEPTED,
                ZonedDateTime.now(ZoneOffset.UTC)
        );
    }

}

package energy.eddie.regionconnector.de.eta.service;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.de.eta.client.EtaPlusApiClient;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestBuilder;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestRepository;
import energy.eddie.regionconnector.de.eta.providers.EtaPlusMeteredData;
import energy.eddie.regionconnector.de.eta.providers.cim.EtaToCimMapper;
import energy.eddie.regionconnector.de.eta.providers.cim.v104.DeValidatedHistoricalDataMarketDocumentProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PollingServiceTest {

    @Mock private DataNeedsService dataNeedsService;
    @Mock private EtaPlusApiClient etaPlusApiClient;
    @Mock private EtaToCimMapper mapper;
    @Mock private DeValidatedHistoricalDataMarketDocumentProvider publisher;
    @Mock private DePermissionRequestRepository repository;

    @InjectMocks
    private PollingService pollingService;

    @Captor
    private ArgumentCaptor<DePermissionRequest> permissionRequestCaptor;


    @Test
    void pollTimeSeriesData_successfullyPollsAndUpdatesWatermark() {
        LocalDate now = LocalDate.now(ZoneOffset.UTC);
        LocalDate lastReadingDate = now.minusDays(2); // Needs data for Yesterday (T-1)

        DePermissionRequest pr = new DePermissionRequestBuilder()
                .permissionId("pid-123")
                .connectionId("cid")
                .meteringPointId("malo-123")
                .dataNeedId("dnid")
                .status(PermissionProcessStatus.ACCEPTED)
                .created(ZonedDateTime.now(ZoneOffset.UTC))
                .start(now.minusDays(30))
                .end(now.plusDays(365)) // Valid contract
                .latestReading(lastReadingDate.atStartOfDay(ZoneOffset.UTC)) // Watermark
                .granularity(Granularity.PT15M)
                .energyType(EnergyType.ELECTRICITY)
                .build();

        EtaPlusMeteredData.MeterReading mockReading = new EtaPlusMeteredData.MeterReading(
                "2023-01-01T00:00:00Z",
                123.45,
                "KWH",
                "validated"
        );

        when(etaPlusApiClient.streamMeteredData(anyString(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Flux.just(mockReading));

        VHDEnvelope mockEnvelope = new VHDEnvelope();
        when(mapper.mapToEnvelope(eq(pr), anyList()))
                .thenReturn(Optional.of(mockEnvelope));

        pollingService.pollTimeSeriesData(pr);

        verify(etaPlusApiClient).streamMeteredData(
                eq("malo-123"),
                eq(lastReadingDate.plusDays(1)), // Fetch Start
                eq(now) // Fetch End (Exclusive)
        );

        verify(publisher).emitDocument(mockEnvelope);

        verify(repository).save(permissionRequestCaptor.capture());
        DePermissionRequest capturedRequest = permissionRequestCaptor.getValue();

        assertThat(capturedRequest.latestMeterReadingEndDate())
                .isPresent()
                .contains(now.minusDays(1));
    }

    @ParameterizedTest
    @MethodSource("futurePermissionRequestStartDates")
    void pollTimeSeriesData_doesNothing_onFuturePermissionRequests(LocalDate start) {
        DePermissionRequest pr = new DePermissionRequestBuilder()
                .permissionId("pid")
                .connectionId("cid")
                .dataNeedId("dnid")
                .status(PermissionProcessStatus.ACCEPTED)
                .created(ZonedDateTime.now(ZoneOffset.UTC))
                .start(start)
                .end(LocalDate.now(ZoneOffset.UTC).plusDays(30))
                .granularity(Granularity.PT15M)
                .energyType(EnergyType.ELECTRICITY)
                .build();

        pollingService.pollTimeSeriesData(pr);

        verifyNoInteractions(etaPlusApiClient);
        verifyNoInteractions(publisher);
        verifyNoInteractions(repository);
    }

    @Test
    void isActiveAndNeedsToBeFetched_returnsTrue_forActivePermissionWithVHD() {
        var now = LocalDate.now(ZoneOffset.UTC);
        var pr = new DePermissionRequestBuilder()
                .permissionId("pid")
                .connectionId("cid")
                .dataNeedId("dnid")
                .status(PermissionProcessStatus.ACCEPTED)
                .created(ZonedDateTime.now(ZoneOffset.UTC))
                .start(now.minusDays(10))
                .end(now)
                .granularity(Granularity.PT15M)
                .energyType(EnergyType.ELECTRICITY)
                .build();
        ValidatedHistoricalDataDataNeed dataNeed = new ValidatedHistoricalDataDataNeed(
                new RelativeDuration(null, null, null),
                EnergyType.ELECTRICITY,
                Granularity.PT15M,
                Granularity.P1D
        );
        when(dataNeedsService.getById("dnid")).thenReturn(dataNeed);

        boolean result = pollingService.isActiveAndNeedsToBeFetched(pr);

        assertThat(result).isTrue();
    }

    @Test
    void isActiveAndNeedsToBeFetched_returnsFalse_forFuturePermissionRequest() {
        var now = LocalDate.now(ZoneOffset.UTC);
        var pr = new DePermissionRequestBuilder()
                .permissionId("pid")
                .connectionId("cid")
                .dataNeedId("dnid")
                .status(PermissionProcessStatus.ACCEPTED)
                .created(ZonedDateTime.now(ZoneOffset.UTC))
                .start(now.plusDays(1)) // Future start date
                .end(now.plusDays(30))
                .granularity(Granularity.PT15M)
                .energyType(EnergyType.ELECTRICITY)
                .build();

        boolean result = pollingService.isActiveAndNeedsToBeFetched(pr);

        assertThat(result).isFalse();
        verifyNoInteractions(dataNeedsService);
    }

    @Test
    void isActiveAndNeedsToBeFetched_returnsFalse_forNonAcceptedPermissionRequest() {
        LocalDate now = LocalDate.now(ZoneOffset.UTC);
        DePermissionRequest pr = new DePermissionRequestBuilder()
                .permissionId("pid")
                .connectionId("cid")
                .dataNeedId("dnid")
                .status(PermissionProcessStatus.CREATED) // Not ACCEPTED
                .created(ZonedDateTime.now(ZoneOffset.UTC))
                .start(now.minusDays(10))
                .end(now)
                .granularity(Granularity.PT15M)
                .energyType(EnergyType.ELECTRICITY)
                .build();

        boolean result = pollingService.isActiveAndNeedsToBeFetched(pr);

        assertThat(result).isFalse();
        verifyNoInteractions(dataNeedsService);
    }

    @Test
    void isActiveAndNeedsToBeFetched_returnsFalse_forAccountingPointDataNeed() {
        var now = LocalDate.now(ZoneOffset.UTC);
        var pr = new DePermissionRequestBuilder()
                .permissionId("pid")
                .connectionId("cid")
                .dataNeedId("dnid")
                .status(PermissionProcessStatus.ACCEPTED)
                .created(ZonedDateTime.now(ZoneOffset.UTC))
                .start(now.minusDays(10))
                .end(now)
                .granularity(Granularity.PT15M)
                .energyType(EnergyType.ELECTRICITY)
                .build();
        when(dataNeedsService.getById("dnid")).thenReturn(new AccountingPointDataNeed());

        boolean result = pollingService.isActiveAndNeedsToBeFetched(pr);

        assertThat(result).isFalse();
    }

    private static Stream<Arguments> futurePermissionRequestStartDates() {
        return Stream.of(
                Arguments.of(LocalDate.now(ZoneOffset.UTC)),
                Arguments.of(LocalDate.now(ZoneOffset.UTC).plusDays(1)),
                Arguments.of(LocalDate.now(ZoneOffset.UTC).plusDays(30))
        );
    }
}
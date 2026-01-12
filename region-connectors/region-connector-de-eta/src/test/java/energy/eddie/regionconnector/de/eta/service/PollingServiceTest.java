package energy.eddie.regionconnector.de.eta.service;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PollingServiceTest {

    @Mock
    private DataNeedsService dataNeedsService;

    @InjectMocks
    private PollingService pollingService;

    @ParameterizedTest
    @MethodSource("futurePermissionRequestStartDates")
    void pollTimeSeriesData_doesNothing_onFuturePermissionRequests(LocalDate start) {
        // Given
        var pr = new DePermissionRequestBuilder()
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

        // When - should not throw and should not do anything
        pollingService.pollTimeSeriesData(pr);

        // Then - no interactions expected (API not implemented yet, but verifying no exceptions)
        // The method should return early for future permission requests
    }

    @Test
    void pollTimeSeriesData_logsWarning_forActivePermissionRequest() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var start = now.minusDays(10);
        var pr = new DePermissionRequestBuilder()
                .permissionId("pid")
                .connectionId("cid")
                .dataNeedId("dnid")
                .status(PermissionProcessStatus.ACCEPTED)
                .created(ZonedDateTime.now(ZoneOffset.UTC))
                .start(start)
                .end(now)
                .granularity(Granularity.PT15M)
                .energyType(EnergyType.ELECTRICITY)
                .build();

        // When - should not throw
        pollingService.pollTimeSeriesData(pr);

        // Then - method completes without exception (API implementation pending)
    }

    @Test
    void isActiveAndNeedsToBeFetched_returnsTrue_forActivePermissionWithVHD() {
        // Given
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
        var dataNeed = new ValidatedHistoricalDataDataNeed(
                new RelativeDuration(null, null, null),
                EnergyType.ELECTRICITY,
                Granularity.PT15M,
                Granularity.P1D
        );
        when(dataNeedsService.getById("dnid")).thenReturn(dataNeed);

        // When
        boolean result = pollingService.isActiveAndNeedsToBeFetched(pr);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isActiveAndNeedsToBeFetched_returnsFalse_forFuturePermissionRequest() {
        // Given
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

        // When
        boolean result = pollingService.isActiveAndNeedsToBeFetched(pr);

        // Then
        assertThat(result).isFalse();
        verifyNoInteractions(dataNeedsService);
    }

    @Test
    void isActiveAndNeedsToBeFetched_returnsFalse_forNonAcceptedPermissionRequest() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var pr = new DePermissionRequestBuilder()
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

        // When
        boolean result = pollingService.isActiveAndNeedsToBeFetched(pr);

        // Then
        assertThat(result).isFalse();
        verifyNoInteractions(dataNeedsService);
    }

    @Test
    void isActiveAndNeedsToBeFetched_returnsFalse_forAccountingPointDataNeed() {
        // Given
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

        // When
        boolean result = pollingService.isActiveAndNeedsToBeFetched(pr);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isActiveAndNeedsToBeFetched_returnsFalse_forStartDateEqualToToday() {
        // Given - permission that starts today (not in the past)
        var now = LocalDate.now(ZoneOffset.UTC);
        var pr = new DePermissionRequestBuilder()
                .permissionId("pid")
                .connectionId("cid")
                .dataNeedId("dnid")
                .status(PermissionProcessStatus.ACCEPTED)
                .created(ZonedDateTime.now(ZoneOffset.UTC))
                .start(now) // Start date is today
                .end(now.plusDays(30))
                .granularity(Granularity.PT15M)
                .energyType(EnergyType.ELECTRICITY)
                .build();

        // When
        boolean result = pollingService.isActiveAndNeedsToBeFetched(pr);

        // Then
        assertThat(result).isFalse();
        verifyNoInteractions(dataNeedsService);
    }

    private static Stream<Arguments> futurePermissionRequestStartDates() {
        return Stream.of(
                Arguments.of(LocalDate.now(ZoneOffset.UTC)),
                Arguments.of(LocalDate.now(ZoneOffset.UTC).plusDays(1)),
                Arguments.of(LocalDate.now(ZoneOffset.UTC).plusDays(30))
        );
    }
}


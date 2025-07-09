package energy.eddie.regionconnector.be.fluvius.sandbox;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.be.fluvius.client.DataServiceType;
import energy.eddie.regionconnector.be.fluvius.client.FluviusApi;
import energy.eddie.regionconnector.be.fluvius.client.model.CreateMandateResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.client.model.FluviusSessionCreateResultResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.client.model.GetEnergyResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.client.model.GetMandateResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.permission.request.Flow;
import energy.eddie.regionconnector.be.fluvius.permission.request.FluviusPermissionRequest;
import energy.eddie.regionconnector.be.fluvius.persistence.BePermissionRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SandboxFluviusApiClientTest {
    @Mock
    private FluviusApi api;
    @Mock
    private BePermissionRequestRepository repository;
    @Mock
    private DataNeedsService dataNeedsService;
    @InjectMocks
    private SandboxFluviusApiClient sandboxFluviusApiClient;

    @Test
    void testShortUrlIdentifier_callsMockMandate() {
        // Given
        var today = LocalDate.now(ZoneOffset.UTC);
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        when(repository.getByPermissionId("pid"))
                .thenReturn(new FluviusPermissionRequest(
                        "pid",
                        "cid",
                        "dnid",
                        PermissionProcessStatus.VALIDATED,
                        Granularity.PT15M,
                        today,
                        today,
                        now,
                        Flow.B2B
                ));
        when(dataNeedsService.getById("dnid"))
                .thenReturn(createDataNeed());
        when(api.mockMandate("pid", now, now, "541440110000000001", Granularity.PT15M))
                .thenReturn(Mono.just(new CreateMandateResponseModelApiDataResponse()));
        when(api.shortUrlIdentifier("pid", Flow.B2B, now, now, Granularity.PT15M))
                .thenReturn(Mono.just(new FluviusSessionCreateResultResponseModelApiDataResponse()));

        // When
        var res = sandboxFluviusApiClient.shortUrlIdentifier("pid", Flow.B2B, now, now, Granularity.PT15M);

        // Then
        StepVerifier.create(res)
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void testMandateFor_callsDecoratee() {
        // Given
        when(api.mandateFor("pid"))
                .thenReturn(Mono.just(new GetMandateResponseModelApiDataResponse()));

        // When
        var res = sandboxFluviusApiClient.mandateFor("pid");

        // Then
        StepVerifier.create(res)
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void testEnergy_callsDecoratee() {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        when(api.energy("pid", "eanNumber", DataServiceType.DAILY, now, now))
                .thenReturn(Mono.just(new GetEnergyResponseModelApiDataResponse()));

        // When
        var res = sandboxFluviusApiClient.energy("pid", "eanNumber", DataServiceType.DAILY, now, now);

        // Then
        StepVerifier.create(res)
                    .expectNextCount(1)
                    .verifyComplete();
    }

    private static ValidatedHistoricalDataDataNeed createDataNeed() {
        return new ValidatedHistoricalDataDataNeed(new RelativeDuration(Period.ZERO, Period.ZERO, null),
                                                   EnergyType.ELECTRICITY,
                                                   Granularity.PT5M,
                                                   Granularity.P1Y);
    }
}
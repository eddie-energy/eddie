package energy.eddie.regionconnector.dk.energinet.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.energinet.DtoLoader;
import energy.eddie.regionconnector.dk.energinet.EnerginetBeanConfig;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.permission.events.DkSimpleEvent;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetPermissionRequest;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata.DK_ZONE_ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountingPointDetailsServiceTest {
    private final ObjectMapper mapper = new EnerginetBeanConfig().objectMapper();
    private final String refreshToken = "token";
    @Mock
    private EnerginetCustomerApi customerApi;
    @Mock
    private Outbox outbox;
    @Captor
    private ArgumentCaptor<DkSimpleEvent> simpleEventArgumentCaptor;
    private AccountingPointDetailsService accountingPointDetailsService;

    @BeforeEach
    void setUp() {
        accountingPointDetailsService = new AccountingPointDetailsService(
                customerApi,
                mapper,
                outbox,
                new ApiExceptionService(outbox)
        );
    }

    @Test
    void fetchMeteringPointDetails_revokesPermissionRequest_whenTokenInvalid() {
        // Given
        var start = LocalDate.now(DK_ZONE_ID).minusDays(10);
        var end = start.plusDays(5);
        var permissionRequest = permissionRequest(start, end);
        WebClientResponseException unauthorized = WebClientResponseException.create(HttpStatus.UNAUTHORIZED.value(),
                                                                                    "",
                                                                                    HttpHeaders.EMPTY,
                                                                                    null,
                                                                                    null);
        doReturn(Mono.error(unauthorized))
                .when(customerApi).accessToken(anyString());

        // When
        StepVerifier.create(accountingPointDetailsService.identifiableMeteringPointDetailsFlux())
                    .then(() -> accountingPointDetailsService.fetchMeteringPointDetails(permissionRequest))
                    .then(accountingPointDetailsService::close)
                    .expectComplete()
                    .verify(Duration.ofSeconds(2));

        // Then
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.REVOKED, event.status())));
    }

    private EnerginetPermissionRequest permissionRequest(
            LocalDate start,
            LocalDate end
    ) {
        String connectionId = "connId";
        String meteringPoint = "meteringPoint";
        String dataNeedId = "dataNeedId";
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

    @Test
    void fetchMeteringPointDetails_emitsDetailsAndFulfillsPermissionRequest() throws IOException {
        // Given
        var start = LocalDate.now(DK_ZONE_ID).minusDays(10);
        var end = start.plusDays(5);
        var permissionRequest = permissionRequest(start, end);
        doReturn(Mono.just(refreshToken))
                .when(customerApi).accessToken(anyString());
        var response = DtoLoader.validApiResponse();
        when(customerApi.getMeteringPointDetails(any(), eq("token")))
                .thenReturn(Mono.just(response));

        // When
        var stepVerifier = StepVerifier
                .create(accountingPointDetailsService.identifiableMeteringPointDetailsFlux())
                .then(() -> accountingPointDetailsService.fetchMeteringPointDetails(
                        permissionRequest));

        // Then
        stepVerifier
                .assertNext(mr -> assertAll(
                        () -> assertEquals(permissionRequest.permissionId(), mr.permissionRequest().permissionId()),
                        () -> assertEquals(permissionRequest.connectionId(), mr.permissionRequest().connectionId()),
                        () -> assertEquals(permissionRequest.dataNeedId(), mr.permissionRequest().dataNeedId()),
                        () -> assertNotNull(mr.meteringPointDetails())
                ))
                .then(accountingPointDetailsService::close)
                .expectComplete()
                .verify(Duration.ofSeconds(2));
        verify(outbox).commit(simpleEventArgumentCaptor.capture());
        var res = simpleEventArgumentCaptor.getValue();
        assertEquals(PermissionProcessStatus.FULFILLED, res.status());
    }
}

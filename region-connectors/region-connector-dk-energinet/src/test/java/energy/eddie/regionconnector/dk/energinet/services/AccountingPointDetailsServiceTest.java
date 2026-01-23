// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.dk.energinet.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.energinet.DtoLoader;
import energy.eddie.regionconnector.dk.energinet.customer.client.EnerginetCustomerApiClient;
import energy.eddie.regionconnector.dk.energinet.permission.events.DkSimpleEvent;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetPermissionRequestBuilder;
import energy.eddie.regionconnector.dk.energinet.providers.EnergyDataStreams;
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
import tools.jackson.databind.ObjectMapper;

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
    private final ObjectMapper mapper = new ObjectMapper();
    private final String refreshToken = "token";
    @Mock
    private EnerginetCustomerApiClient customerApi;
    @Mock
    private Outbox outbox;
    private EnergyDataStreams streams;
    @Captor
    private ArgumentCaptor<DkSimpleEvent> simpleEventArgumentCaptor;
    private AccountingPointDetailsService accountingPointDetailsService;

    @BeforeEach
    void setUp() {
        streams = new EnergyDataStreams();
        accountingPointDetailsService = new AccountingPointDetailsService(
                customerApi,
                mapper,
                outbox,
                new ApiExceptionService(outbox),
                streams
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
        StepVerifier.create(streams.getAccountingPointDataStream())
                    .then(() -> accountingPointDetailsService.fetchMeteringPointDetails(permissionRequest))
                    .then(streams::close)
                    .expectComplete()
                    .verify(Duration.ofSeconds(2));

        // Then
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.REVOKED, event.status())));
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
                .create(streams.getAccountingPointDataStream())
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
                .then(streams::close)
                .expectComplete()
                .verify(Duration.ofSeconds(2));
        verify(outbox).commit(simpleEventArgumentCaptor.capture());
        var res = simpleEventArgumentCaptor.getValue();
        assertEquals(PermissionProcessStatus.FULFILLED, res.status());
    }

    private EnerginetPermissionRequest permissionRequest(
            LocalDate start,
            LocalDate end
    ) {
        String connectionId = "connId";
        String meteringPoint = "meteringPoint";
        String dataNeedId = "dataNeedId";
        return new EnerginetPermissionRequestBuilder().setPermissionId(UUID.randomUUID().toString())
                                                      .setConnectionId(connectionId)
                                                      .setDataNeedId(dataNeedId)
                                                      .setMeteringPoint(meteringPoint)
                                                      .setRefreshToken(refreshToken)
                                                      .setStart(start)
                                                      .setEnd(end)
                                                      .setGranularity(Granularity.PT1H)
                                                      .setAccessToken(null)
                                                      .setStatus(PermissionProcessStatus.ACCEPTED)
                                                      .setCreated(ZonedDateTime.now(ZoneOffset.UTC))
                                                      .build();
    }
}

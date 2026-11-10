// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.application.information.ApplicationInformation;
import energy.eddie.aiida.models.permission.HandshakeTarget;
import energy.eddie.aiida.models.permission.Permission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HandshakeServiceTest {
    private static final String HANDSHAKE_URL = "https://eddie.example.org/region-connectors/aiida/permission-request/42";
    private static final HandshakeTarget TARGET = new HandshakeTarget(HANDSHAKE_URL, "accessToken");
    private final List<ClientRequest> requests = new ArrayList<>();
    @Mock
    private ApplicationInformationService applicationInformationService;
    private HttpStatus responseStatus = HttpStatus.NO_CONTENT;
    private HandshakeService handshakeService;

    @BeforeEach
    void setUp() {
        when(applicationInformationService.applicationInformation())
                .thenReturn(new ApplicationInformation(UUID.randomUUID(), Instant.now()));
        var webClient = WebClient.builder()
                                 .exchangeFunction(request -> {
                                     requests.add(request);
                                     return Mono.just(ClientResponse.create(responseStatus).build());
                                 })
                                 .build();
        handshakeService = new HandshakeService(applicationInformationService, webClient);
    }

    @Test
    void fetchDetails_getsHandshakeUrl() {
        // Given
        responseStatus = HttpStatus.OK;

        // When
        handshakeService.fetchDetails(TARGET).block();

        // Then
        assertRequest(HttpMethod.GET, HANDSHAKE_URL);
    }

    @Test
    void accept_postsToAcceptEndpoint() {
        // Given
        responseStatus = HttpStatus.OK;

        // When
        handshakeService.accept(TARGET).block();

        // Then
        assertRequest(HttpMethod.POST, HANDSHAKE_URL + "/accept");
    }

    @Test
    void reject_postsToRejectEndpoint() {
        // When
        handshakeService.reject(TARGET, "reason");

        // Then
        assertRequest(HttpMethod.POST, HANDSHAKE_URL + "/reject");
    }

    @Test
    void markUnfulfillable_postsToUnfulfillableEndpoint() {
        // When
        handshakeService.denoteUnfulfillable(TARGET, null);

        // Then
        assertRequest(HttpMethod.POST, HANDSHAKE_URL + "/unfulfillable");
    }

    @Test
    void givenErrorResponse_reject_rethrows() {
        // Given
        responseStatus = HttpStatus.BAD_REQUEST;

        // When, Then
        assertThrows(WebClientResponseException.class, () -> handshakeService.reject(TARGET, null));
    }

    @Test
    void handshakeTargetOf_usesUrlAndTokenOfPermission() {
        // Given
        var permission = new Permission(UUID.randomUUID(), UUID.randomUUID(), HANDSHAKE_URL, "token", UUID.randomUUID());

        // When
        var target = HandshakeTarget.of(permission);

        // Then
        assertEquals(new HandshakeTarget(HANDSHAKE_URL, "token"), target);
    }

    @Test
    void handshakeTargetOf_expandsPermissionIdInUrlTemplate() {
        // Given
        var permissionId = UUID.fromString("f69f9bc2-e16c-4de4-8c3e-00d219dcd819");

        // When
        var target = HandshakeTarget.of("https://eddie.example/permission-request/{permissionId}", permissionId, "token");

        // Then
        assertEquals(new HandshakeTarget("https://eddie.example/permission-request/f69f9bc2-e16c-4de4-8c3e-00d219dcd819",
                                         "token"), target);
    }

    private void assertRequest(HttpMethod method, String url) {
        assertEquals(1, requests.size());
        var request = requests.getFirst();
        assertEquals(method, request.method());
        assertEquals(URI.create(url), request.url());
        assertEquals("Bearer accessToken", request.headers().getFirst(HttpHeaders.AUTHORIZATION));
    }
}

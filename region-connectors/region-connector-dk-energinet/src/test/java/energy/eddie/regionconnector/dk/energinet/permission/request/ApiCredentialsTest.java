// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.dk.energinet.permission.request;

import energy.eddie.regionconnector.dk.energinet.customer.client.EnerginetCustomerApiClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiCredentialsTest {

    static Stream<Arguments> refreshToken_throwsIfInvalidPayload() {
        return Stream.of(
                Arguments.of("{ \"exp\": \"Not an integer\"}"),
                Arguments.of("{ \"other\": 0}")
        );
    }

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void refreshToken_returnsAccessTokenIfValid() {
        // Given
        var expiration = ZonedDateTime.now(ZoneOffset.UTC).plusYears(1);
        String payload = "{ \"exp\": %d}".formatted(expiration.toEpochSecond());
        String accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.%s.4Adcj3UFYzPUVaVF43FmMab6RlaQD8A9V8wFzzht-KQ"
                .formatted(Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8)));

        var customerApi = mock(EnerginetCustomerApiClient.class);
        when(customerApi.accessToken("jwt")).thenReturn(Mono.just("validToken"));
        ApiCredentials credentials = new ApiCredentials(customerApi, "jwt", accessToken, mapper);

        // When
        var res = credentials.accessToken();

        // Then
        StepVerifier.create(res)
                .expectNext(accessToken)
                .verifyComplete();
    }

    @Test
    void refreshToken_refreshesAccessTokenWhenInvalid() {
        // Given
        var expiration = ZonedDateTime.now(ZoneOffset.UTC).minusYears(1);
        String payload = "{ \"exp\": %d}".formatted(expiration.toEpochSecond());
        String accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.%s.4Adcj3UFYzPUVaVF43FmMab6RlaQD8A9V8wFzzht-KQ"
                .formatted(Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8)));

        var customerApi = mock(EnerginetCustomerApiClient.class);
        when(customerApi.accessToken("jwt")).thenReturn(Mono.just("validToken"));
        ApiCredentials credentials = new ApiCredentials(customerApi, "jwt", accessToken, mapper);

        // When
        var res = credentials.accessToken();

        // Then
        StepVerifier.create(res)
                .expectNext("validToken")
                .verifyComplete();
    }

    @ParameterizedTest
    @MethodSource
    void refreshToken_throwsIfInvalidPayload(String payload) {
        // Given
        String accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.%s.4Adcj3UFYzPUVaVF43FmMab6RlaQD8A9V8wFzzht-KQ"
                .formatted(Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8)));

        var customerApi = mock(EnerginetCustomerApiClient.class);
        when(customerApi.accessToken("jwt")).thenReturn(Mono.just("validToken"));
        ApiCredentials credentials = new ApiCredentials(customerApi, "jwt", accessToken, mapper);

        // When
        // Then
        assertThrows(ApiCredentials.CredentialsException.class, credentials::accessToken);

    }

}
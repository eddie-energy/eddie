// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.dk.energinet.permission.request;

import energy.eddie.regionconnector.dk.energinet.customer.client.EnerginetCustomerApiClient;
import jakarta.annotation.Nullable;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public final class ApiCredentials {
    private static final TypeReference<HashMap<String, Object>> TYPE_REF = new TypeReference<>() {
    };
    private final EnerginetCustomerApiClient customerApi;
    private final String refreshToken;
    private final ObjectMapper mapper;
    @Nullable
    private String accessToken;

    public ApiCredentials(
            EnerginetCustomerApiClient customerApi,
            String refreshToken,
            @Nullable String accessToken,
            ObjectMapper mapper
    ) {
        this.customerApi = customerApi;
        this.refreshToken = refreshToken;
        this.accessToken = accessToken;
        this.mapper = mapper;
    }

    public Mono<String> accessToken() {
        if (accessToken != null && isAccessTokenValid()) {
            return Mono.just(accessToken);
        }
        return customerApi.accessToken(refreshToken)
                          .mapNotNull(token -> this.accessToken = token);
    }

    private boolean isAccessTokenValid() {
        var payload = decode();
        Object exp = payload.get("exp");
        if (exp == null) {
            throw new CredentialsException("No expiration found");
        }
        if (!(exp instanceof Integer timestamp)) {
            throw new CredentialsException("Expiration timestamp is of wrong type: " + exp.getClass());
        }
        return ZonedDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.of("Europe/Paris"))
                            .isAfter(ZonedDateTime.now(ZoneOffset.UTC));
    }

    private Map<String, Object> decode() {
        if (accessToken == null) {
            throw new NullPointerException("AccessToken is null.");
        }
        String[] chunks = accessToken.split("\\.", -1);
        Base64.Decoder decoder = Base64.getUrlDecoder();
        String payload = new String(decoder.decode(chunks[1]), StandardCharsets.UTF_8);
        try {
            return mapper.readValue(payload, TYPE_REF);
        } catch (JacksonException e) {
            accessToken = null;
            throw new CredentialsException("Could not parse token.", e);
        }
    }

    public static class CredentialsException extends RuntimeException {
        public CredentialsException(String msg) {
            super(msg);
        }

        public CredentialsException(String msg, Exception e) {
            super(msg, e);
        }
    }
}

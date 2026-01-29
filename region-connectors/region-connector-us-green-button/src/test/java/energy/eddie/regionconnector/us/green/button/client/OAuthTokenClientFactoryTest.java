// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.client;

import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import energy.eddie.regionconnector.us.green.button.config.exceptions.MissingApiTokenException;
import energy.eddie.regionconnector.us.green.button.config.exceptions.MissingClientIdException;
import energy.eddie.regionconnector.us.green.button.config.exceptions.MissingClientSecretException;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OAuthTokenClientFactoryTest {
    private final GreenButtonConfiguration config = new GreenButtonConfiguration(
            "http://localhost",
            Map.of("company", "client-id", "only-id", "client-id", "missing-token", "client-id"),
            Map.of("company", "client-secret", "only-secret", "client-secret", "missing-token", "client-secret"),
            Map.of("company", "token", "only-secret", "token", "only-id", "token"),
            "http://localhost",
            "secret");
    private final OAuthTokenClientFactory factory = new OAuthTokenClientFactory(config, WebClient.builder().build());

    @Test
    void create_withMissingClientSecret_throwsException() {
        // Given
        // When & Then
        assertThrows(MissingClientSecretException.class, () -> factory.create("only-id", "http://localhost"));
    }

    @Test
    void create_withMissingClientId_throwsException() {
        // Given
        // When & Then
        assertThrows(MissingClientIdException.class, () -> factory.create("only-secret", "http://localhost"));
    }

    @Test
    void create_withMissingToken_throwsException() {
        // Given
        // When & Then
        assertThrows(MissingApiTokenException.class, () -> factory.create("missing-token", "http://localhost"));
    }

    @Test
    void create_withValidCredentials_returnsClient() throws MissingClientIdException, MissingClientSecretException, MissingApiTokenException {
        // Given
        // When
        var res = factory.create("company", "http://localhost");

        // Then
        assertNotNull(res);
    }
}
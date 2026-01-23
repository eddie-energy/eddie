// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messenger;

import energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapterConfiguration;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class WebClientMessengerHealthTest {

    static MockWebServer mockBackEnd;

    private static PontonXPAdapterConfiguration config;
    private final WebClient webClient = WebClient.create();

    @BeforeAll
    static void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
        config = new PontonXPAdapterConfiguration(
                "adapterId",
                "adapterVersion",
                "hostname",
                1234,
                "http://localhost:" + mockBackEnd.getPort(),
                "folder",
                "username",
                "password"
        );
    }

    @Test
    void messengerStatus_whenMessengerUp_returnsUp() {
        // Arrange
        var messengerHealth = new WebClientMessengerHealth(webClient, config);

        var body = """
                {
                  "healthChecks": {
                    "simpleHealthCheck": {
                      "name": "simpleHealthCheck",
                      "ok": true,
                      "content": "UP"
                    },
                    "activation": {
                      "name": "activation",
                      "ok": true,
                      "content": "Activation Valid"
                    }
                  },
                  "ok": true
                }
                """;
        mockBackEnd.enqueue(new MockResponse()
                                    .setBody(body)
                                    .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        );

        // Act
        var status = messengerHealth.messengerStatus();

        // Assert
        assertAll(
                () -> assertTrue(status.ok()),
                () -> assertEquals(2, status.healthChecks().size()),
                () -> assertTrue(status.healthChecks().get("simpleHealthCheck").ok()),
                () -> assertEquals("UP", status.healthChecks().get("simpleHealthCheck").content()),
                () -> assertEquals("simpleHealthCheck", status.healthChecks().get("simpleHealthCheck").name()),
                () -> assertTrue(status.healthChecks().get("activation").ok()),
                () -> assertEquals("Activation Valid", status.healthChecks().get("activation").content()),
                () -> assertEquals("activation", status.healthChecks().get("activation").name())
        );
    }

    @Test
    void messengerStatus_whenMessengerDown_returnsDown() {
        // Arrange
        var messengerHealth = new WebClientMessengerHealth(webClient, config);

        var body = """
                {
                  "healthChecks": {
                    "simpleHealthCheck": {
                      "name": "simpleHealthCheck",
                      "ok": false,
                      "content": "DOWN"
                    },
                    "activation": {
                      "name": "activation",
                      "ok": false,
                      "content": "Activation Invalid"
                    }
                  },
                  "ok": false
                }
                """;
        mockBackEnd.enqueue(new MockResponse()
                                    .setBody(body)
                                    .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        );

        // Act
        var status = messengerHealth.messengerStatus();

        // Assert
        assertAll(
                () -> assertFalse(status.ok()),
                () -> assertEquals(2, status.healthChecks().size()),
                () -> assertFalse(status.healthChecks().get("simpleHealthCheck").ok()),
                () -> assertEquals("DOWN", status.healthChecks().get("simpleHealthCheck").content()),
                () -> assertEquals("simpleHealthCheck", status.healthChecks().get("simpleHealthCheck").name()),
                () -> assertFalse(status.healthChecks().get("activation").ok()),
                () -> assertEquals("Activation Invalid", status.healthChecks().get("activation").content()),
                () -> assertEquals("activation", status.healthChecks().get("activation").name())
        );
    }

    @Test
    void messengerStatus_whenServerReturnsNull_returnsDown() {
        // Arrange
        var messengerHealth = new WebClientMessengerHealth(webClient, config);

        mockBackEnd.enqueue(new MockResponse());

        // Act
        var status = messengerHealth.messengerStatus();

        // Assert
        assertAll(
                () -> assertFalse(status.ok()),
                () -> assertEquals(0, status.healthChecks().size())
        );
    }

    @ParameterizedTest
    @CsvSource({
            "400, Bad Request",
            "401, Unauthorized",
            "403, Forbidden",
            "404, Not Found",
            "500, Internal Server Error"
    })
    void messengerStatus_whenServerReturnsError_returnsDown(int errorCode, String errorMessage) {
        // Arrange
        var messengerHealth = new WebClientMessengerHealth(webClient, config);

        mockBackEnd.enqueue(new MockResponse()
                                    .setStatus("HTTP/1.1 " + errorCode + " " + errorMessage)
        );

        // Act
        var status = messengerHealth.messengerStatus();

        // Assert
        assertAll(
                () -> assertFalse(status.ok()),
                () -> assertEquals(0, status.healthChecks().size())
        );
    }
}

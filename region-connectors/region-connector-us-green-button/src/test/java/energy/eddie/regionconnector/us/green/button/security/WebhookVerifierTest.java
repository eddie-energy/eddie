// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.security;

import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookVerifierTest {
    public static final byte[] PAYLOAD = "{\"json\": \"object\"}".getBytes(StandardCharsets.UTF_8);
    public static final String VALID_SIGNATURE = "6cd59ee8461872a3e0deea0d6428b5cdad5049452b4914ad66ed1cc1081b5e7d";
    private final GreenButtonConfiguration config = new GreenButtonConfiguration(
            "localhost",
            Map.of(),
            Map.of(),
            Map.of(),
            "localhost",
            "secret"
    );
    @Mock
    private HttpServletRequest request;

    public static Stream<Arguments> testVerifySignature_returnsFalse_onMissingSignatureOrSalt() {
        return Stream.of(
                Arguments.of("signature", null),
                Arguments.of(null, "salt")
        );
    }

    @ParameterizedTest
    @MethodSource
    void testVerifySignature_returnsFalse_onMissingSignatureOrSalt(String signature, String salt) {
        // Given
        var verifier = new WebhookVerifier(salt, "secret", new ByteArrayInputStream(new byte[10]), signature);

        // When
        var res = verifier.verifySignature();

        // Then
        assertFalse(res);
    }

    @Test
    void testVerifySignature_returnsFalse_ifProvidedSignatureWrong() {
        // Given
        var verifier = new WebhookVerifier("salt",
                                           "secret",
                                           new ByteArrayInputStream(new byte[10]),
                                           "invalid signature");

        // When
        var res = verifier.verifySignature();

        // Then
        assertFalse(res);
    }

    @Test
    void testVerifySignature_returnsTrue_ifProvidedSignatureCorrect() {
        // Given
        var verifier = new WebhookVerifier(
                "salt",
                "secret",
                new ByteArrayInputStream(PAYLOAD),
                VALID_SIGNATURE
        );

        // When
        var res = verifier.verifySignature();

        // Then
        assertTrue(res);
    }

    @Test
    void testVerifySignature_returnsTrueDecision_onValidSignature() {
        // Given
        var mockReq = new MockHttpServletRequest();
        mockReq.setContent(PAYLOAD);
        mockReq.addHeader(WebhookVerifier.WEBHOOK_SALT_HEADER, "salt");
        mockReq.addHeader(WebhookVerifier.WEBHOOK_SIGNATURE_HEADER, VALID_SIGNATURE);

        // When
        var res = WebhookVerifier.verifySignature(mockReq, config);

        // Then
        assertTrue(res.isGranted());
    }

    @Test
    void testVerifySignature_returnsNegativeDecision_onInvalidRequest() throws IOException {
        // Given
        when(request.getInputStream()).thenThrow(IOException.class);
        when(request.getHeader(WebhookVerifier.WEBHOOK_SALT_HEADER)).thenReturn("salt");
        when(request.getHeader(WebhookVerifier.WEBHOOK_SIGNATURE_HEADER)).thenReturn("signature");

        // When
        var res = WebhookVerifier.verifySignature(request, config);

        // Then
        assertFalse(res.isGranted());
    }
}
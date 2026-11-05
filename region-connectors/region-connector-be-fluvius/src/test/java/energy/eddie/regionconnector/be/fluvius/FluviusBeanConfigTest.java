// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius;

import energy.eddie.regionconnector.be.fluvius.config.FluviusConfiguration;
import energy.eddie.regionconnector.be.fluvius.config.FluviusOAuthConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.webclient.autoconfigure.WebClientSsl;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FluviusBeanConfigTest {
    @Mock
    private WebClientSsl webClientSsl;
    private final FluviusBeanConfig beans = new FluviusBeanConfig();
    private final FluviusConfiguration config = new FluviusConfiguration("https://apihub.fluvius.be/esco-sbx", "key", "contract", false);
    private final FluviusOAuthConfiguration oauthConfig = new FluviusOAuthConfiguration("https://login.microsoftonline.com/tenant/oauth2/v2.0/token",
                                                                                       "client", "secret", "tenant", "scope");

    @Test
    void sandboxClientWorksWithoutCertificateBundle() {
        assertNotNull(beans.webClient(WebClient.builder(), webClientSsl, new DefaultSslBundleRegistry(), config, oauthConfig));
        verifyNoInteractions(webClientSsl);
    }

    @Test
    void appliesConfiguredCertificateBundleWhenPresent() {
        var bundles = new DefaultSslBundleRegistry();
        var bundle = SslBundle.of(null);
        bundles.registerBundle("fluvius", bundle);
        when(webClientSsl.fromBundle(bundle)).thenReturn(builder -> {});
        assertNotNull(beans.webClient(WebClient.builder(), webClientSsl, bundles, config, oauthConfig));
        verify(webClientSsl).fromBundle(bundle);
    }
}

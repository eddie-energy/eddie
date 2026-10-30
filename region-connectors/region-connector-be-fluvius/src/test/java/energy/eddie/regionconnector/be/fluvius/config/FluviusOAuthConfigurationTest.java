// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FluviusOAuthConfigurationTest {
    private static final String PREFIX = "region-connector.be.fluvius.oauth.";

    @Test
    void existingSandboxPropertiesDefaultToClientSecretAuthentication() {
        var config = bind(properties());
        assertEquals(FluviusOAuthConfiguration.AuthenticationMethod.CLIENT_SECRET, config.authenticationMethod());
        assertEquals("fluvius", config.sslBundle());
        assertEquals("tenant", config.tenantId());
    }

    @Test
    void certificateModeBindsWithoutClientSecret() {
        var properties = properties();
        properties.remove(PREFIX + "client-secret");
        properties.put(PREFIX + "authentication-method", "certificate");
        properties.put(PREFIX + "ssl-bundle", "live-certificate");
        var config = bind(properties);
        assertEquals(FluviusOAuthConfiguration.AuthenticationMethod.CERTIFICATE, config.authenticationMethod());
        assertEquals("live-certificate", config.sslBundle());
        assertNull(config.clientSecret());
    }

    @ParameterizedTest
    @ValueSource(strings = {"token-url", "client-id", "scope", "client-secret", "ssl-bundle"})
    void rejectsBlankRequiredProperties(String property) {
        var properties = properties();
        properties.put(PREFIX + property, " ");
        assertThrows(org.springframework.boot.context.properties.bind.BindException.class, () -> bind(properties));
    }

    private static Map<String, Object> properties() {
        return new HashMap<>(Map.of(PREFIX + "token-url", "https://login.microsoftonline.com/tenant/oauth2/v2.0/token",
                                   PREFIX + "client-id", "client", PREFIX + "client-secret", "secret",
                                   PREFIX + "tenant-id", "tenant", PREFIX + "scope", "api://test/.default"));
    }

    private static FluviusOAuthConfiguration bind(Map<String, Object> properties) {
        return new Binder(new MapConfigurationPropertySource(properties))
                .bind("region-connector.be.fluvius.oauth", FluviusOAuthConfiguration.class).get();
    }
}

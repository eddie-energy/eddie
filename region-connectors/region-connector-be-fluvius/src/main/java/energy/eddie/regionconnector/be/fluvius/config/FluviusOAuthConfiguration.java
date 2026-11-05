// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.config;

import jakarta.annotation.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.Assert;

@ConfigurationProperties("region-connector.be.fluvius.oauth")
public class FluviusOAuthConfiguration {
    private final String tokenUrl;
    private final String clientId;
    @Nullable
    private final String clientSecret;
    private final String tenantId;
    private final String scope;
    private final AuthenticationMethod authenticationMethod;
    private final String sslBundle;

    public FluviusOAuthConfiguration(
            String tokenUrl,
            String clientId,
            String clientSecret,
            String tenantId,
            String scope
    ) {
        this(tokenUrl, clientId, clientSecret, tenantId, scope, AuthenticationMethod.CLIENT_SECRET, "fluvius");
    }

    @ConstructorBinding
    public FluviusOAuthConfiguration(
            String tokenUrl,
            String clientId,
            @Nullable String clientSecret,
            String tenantId,
            String scope,
            @DefaultValue("client-secret") AuthenticationMethod authenticationMethod,
            @DefaultValue("fluvius") String sslBundle
    ) {
        Assert.hasText(tokenUrl, "Fluvius OAuth token-url is required");
        Assert.hasText(clientId, "Fluvius OAuth client-id is required");
        Assert.hasText(scope, "Fluvius OAuth scope is required");
        if (authenticationMethod == AuthenticationMethod.CLIENT_SECRET) {
            Assert.hasText(clientSecret, "Fluvius OAuth client-secret is required for client-secret authentication");
        }
        Assert.hasText(sslBundle, "Fluvius OAuth ssl-bundle is required");
        this.tokenUrl = tokenUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tenantId = tenantId;
        this.scope = scope;
        this.authenticationMethod = authenticationMethod;
        this.sslBundle = sslBundle;
    }

    public String tokenUrl() {
        return tokenUrl;
    }

    public String clientId() {
        return clientId;
    }

    @Nullable
    public String clientSecret() {
        return clientSecret;
    }

    public String tenantId() {
        return tenantId;
    }

    public String scope() {
        return scope;
    }

    public AuthenticationMethod authenticationMethod() {
        return authenticationMethod;
    }

    public String sslBundle() {
        return sslBundle;
    }

    public enum AuthenticationMethod {
        CLIENT_SECRET,
        CERTIFICATE
    }
}

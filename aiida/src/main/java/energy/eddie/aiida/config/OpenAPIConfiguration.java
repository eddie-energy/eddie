// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfiguration {
    private static final String BEARER_AUTH_SCHEME = "bearerAuth";
    private static final String KEYCLOAK_AUTH_SCHEME = "keycloak";

    private final KeycloakConfiguration keycloakConfiguration;

    public OpenAPIConfiguration(KeycloakConfiguration keycloakConfiguration) {
        this.keycloakConfiguration = keycloakConfiguration;
    }

    @Bean
    public OpenAPI openAPI() {
        var info = new Info()
                .title("AIIDA API")
                .version("1.0");

        var bearerSecurityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        var keycloakSecurityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.OAUTH2)
                .flows(
                        new OAuthFlows()
                                .authorizationCode(
                                        new OAuthFlow()
                                                .authorizationUrl(keycloakConfiguration.authorizationUri())
                                                .tokenUrl(keycloakConfiguration.tokenUri())
                                                .scopes(new Scopes().addString("openid", "OpenID Connect scope"))
                                )
                );

        return new OpenAPI()
                .info(info)
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH_SCHEME))
                .addSecurityItem(new SecurityRequirement().addList(KEYCLOAK_AUTH_SCHEME))
                .components(new Components()
                                    .addSecuritySchemes(BEARER_AUTH_SCHEME, bearerSecurityScheme)
                                    .addSecuritySchemes(KEYCLOAK_AUTH_SCHEME, keycloakSecurityScheme)
                );
    }
}
// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida;

import energy.eddie.api.agnostic.RegionConnectorSecurityConfig;
import energy.eddie.regionconnector.shared.security.JwtAuthorizationManager;
import energy.eddie.spring.regionconnector.extensions.SecurityExceptionHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

import static energy.eddie.regionconnector.aiida.web.PermissionRequestController.PATH_HANDSHAKE_PERMISSION_REQUEST;
import static energy.eddie.regionconnector.shared.utils.CommonPaths.ALL_REGION_CONNECTORS_BASE_URL_PATH;
import static energy.eddie.regionconnector.shared.utils.CommonPaths.CE_FILE_NAME;
import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_REQUEST;
import static energy.eddie.regionconnector.shared.web.RestApiPaths.SWAGGER_DOC_PATH;

@RegionConnectorSecurityConfig
public class AiidaSecurityConfig {
    private static final String AIIDA_ENABLED_PROPERTY = "region-connector.aiida.enabled";

    @Bean
    @ConditionalOnProperty(value = AIIDA_ENABLED_PROPERTY, havingValue = "true")
    public PathPatternRequestMatcher.Builder aiidaMvcRequestMatcher() {
        return PathPatternRequestMatcher.withDefaults()
                                        .basePath("/" + ALL_REGION_CONNECTORS_BASE_URL_PATH + "/" + AiidaRegionConnectorMetadata.REGION_CONNECTOR_ID);
    }

    @Bean
    @ConditionalOnProperty(value = AIIDA_ENABLED_PROPERTY, havingValue = "true")
    public SecurityFilterChain aiidaSecurityFilterChain(
            @Qualifier("aiidaMvcRequestMatcher") PathPatternRequestMatcher.Builder aiidaRequestMatcher,
            HttpSecurity http,
            JwtAuthorizationManager jwtHeaderAuthorizationManager,
            CorsConfigurationSource corsConfigurationSource,
            ObjectMapper mapper
    ) {
        return http
                .securityMatcher(aiidaRequestMatcher.matcher("/**"))    // apply following rules only to requests of this DispatcherServlet
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
// @formatter:off   all calls for one request pattern should be on one line
                        .requestMatchers(aiidaRequestMatcher.matcher(PATH_PERMISSION_REQUEST)).permitAll()
                        .requestMatchers(aiidaRequestMatcher.matcher(PATH_HANDSHAKE_PERMISSION_REQUEST)).access(jwtHeaderAuthorizationManager)
                        .requestMatchers(aiidaRequestMatcher.matcher("/" + CE_FILE_NAME)).permitAll()
                        .requestMatchers(aiidaRequestMatcher.matcher("/" + SWAGGER_DOC_PATH)).permitAll()
                        .anyRequest().denyAll()
// @formatter:on
                )
                .exceptionHandling(new SecurityExceptionHandler(mapper))
                .sessionManagement(customizer -> customizer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .build();
    }
}

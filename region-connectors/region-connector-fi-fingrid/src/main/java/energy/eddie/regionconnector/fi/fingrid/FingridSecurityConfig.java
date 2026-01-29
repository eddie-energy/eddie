// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fi.fingrid;

import energy.eddie.api.agnostic.RegionConnectorSecurityConfig;
import energy.eddie.regionconnector.shared.security.JwtAuthorizationManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_ACCEPTED;
import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_REJECTED;
import static energy.eddie.spring.regionconnector.extensions.SecurityUtils.pathPatternRequestMatcher;
import static energy.eddie.spring.regionconnector.extensions.SecurityUtils.securityFilterChain;

@RegionConnectorSecurityConfig
public class FingridSecurityConfig {
    private static final String FINGRID_ENABLED_PROPERTY = "region-connector.fi.fingrid.enabled";

    @Bean
    @ConditionalOnProperty(value = FINGRID_ENABLED_PROPERTY, havingValue = "true")
    public PathPatternRequestMatcher.Builder fingridRequestMatcher() {
        return pathPatternRequestMatcher(FingridRegionConnectorMetadata.REGION_CONNECTOR_ID);
    }

    @Bean
    @ConditionalOnProperty(value = FINGRID_ENABLED_PROPERTY, havingValue = "true")
    @SuppressWarnings("java:S4502")
    public SecurityFilterChain fingridSecurityFilterChain(
            @Qualifier("fingridRequestMatcher") PathPatternRequestMatcher.Builder fingridMvcRequestMatcher,
            HttpSecurity http,
            JwtAuthorizationManager jwtHeaderAuthorizationManager,
            CorsConfigurationSource corsConfigurationSource,
            ObjectMapper mapper
    ) {
        return securityFilterChain(fingridMvcRequestMatcher,
                                   http,
                                   jwtHeaderAuthorizationManager,
                                   corsConfigurationSource,
                                   mapper,
                                   List.of(PATH_PERMISSION_ACCEPTED, PATH_PERMISSION_REJECTED),
                                   List.of("/organisation-information")
        );
    }
}

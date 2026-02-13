// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core.security;

import energy.eddie.regionconnector.shared.security.JwtAuthorizationManager;
import energy.eddie.spring.regionconnector.extensions.SecurityExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

@Configuration
@SuppressWarnings("java:S4502")
public class SecurityConfig {

    @Bean
    public SecurityFilterChain connectionStatusMessageFilterChain(
            HttpSecurity http,
            JwtAuthorizationManager jwtHeaderAuthorizationManager,
            CorsConfigurationSource corsConfigurationSource,
            ObjectMapper mapper
    ) {
        var pathPatternRequestMatcherBuilder = PathPatternRequestMatcher.withDefaults();

        return http
                .securityMatcher("/api/connection-status-messages/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                                               // @formatter:off
                            .requestMatchers(pathPatternRequestMatcherBuilder.matcher("/api/connection-status-messages")).access(jwtHeaderAuthorizationManager)
                            .requestMatchers(pathPatternRequestMatcherBuilder.matcher("/api/connection-status-messages/{permissionId}")).access(jwtHeaderAuthorizationManager)
                            .anyRequest().denyAll()
                       // @formatter
                )
                .exceptionHandling(new SecurityExceptionHandler(mapper))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .build();
    }
}

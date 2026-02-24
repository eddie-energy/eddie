// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;


@Configuration
@EnableWebSecurity
public class OAuth2SecurityConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(OAuth2SecurityConfiguration.class);

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource
    ) {
        return http
                .csrf(csrf ->
                              csrf.ignoringRequestMatchers("/webhook/event")
                                  .ignoringRequestMatchers("/mqtt-auth/**")
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/assets/**",
                                "/favicon.svg",
                                "/mqtt-auth/**",
                                "/actuator/health/**",
                                "/application-information",
                                "/webhook/event",
                                "/inbound/**",
                                "/",
                                "/data-sources",
                                "/account",
                                "/svgs/**")
                        .permitAll()
                        .anyRequest().authenticated()
                )
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${aiida.cors.allowed-origins:}") String allowedCorsOrigins) {
        if (allowedCorsOrigins.isEmpty()) {
            LOGGER.info("No CORS origins configured, will not set any CORS headers.");
            return new UrlBasedCorsConfigurationSource();
        }

        LOGGER.info("Will allow CORS requests from origin patterns '{}'", allowedCorsOrigins);

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setExposedHeaders(List.of("Location"));
        configuration.setAllowedOriginPatterns(List.of(allowedCorsOrigins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE"));
        configuration.setAllowCredentials(true);
        configuration.setAllowedHeaders(List.of("Content-Type", "Authorization"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

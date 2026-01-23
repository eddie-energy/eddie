// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.security;

import energy.eddie.api.agnostic.RegionConnectorSecurityConfig;
import energy.eddie.regionconnector.us.green.button.GreenButtonRegionConnectorMetadata;
import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import energy.eddie.spring.regionconnector.extensions.SecurityExceptionHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

import static energy.eddie.regionconnector.us.green.button.security.WebhookSecurityConfig.US_GREEN_BUTTON_ENABLED;
import static energy.eddie.spring.regionconnector.extensions.SecurityUtils.pathPatternRequestMatcher;

@RegionConnectorSecurityConfig
@EnableConfigurationProperties(GreenButtonConfiguration.class)
@ConditionalOnProperty(value = US_GREEN_BUTTON_ENABLED, havingValue = "true")
public class WebhookSecurityConfig {
    public static final String US_GREEN_BUTTON_ENABLED = "region-connector.us.green.button.enabled";

    @Bean
    @ConditionalOnProperty(value = US_GREEN_BUTTON_ENABLED, havingValue = "true")
    public PathPatternRequestMatcher.Builder greenButtonRequestMatcher() {
        return pathPatternRequestMatcher(GreenButtonRegionConnectorMetadata.REGION_CONNECTOR_ID);
    }

    @Bean
    @ConditionalOnProperty(name = US_GREEN_BUTTON_ENABLED, havingValue = "true")
    @SuppressWarnings("java:S4502")
    public SecurityFilterChain webhookFilterChain(
            HttpSecurity http,
            @Qualifier("greenButtonMvcRequestMatcher") PathPatternRequestMatcher.Builder mvcRequestMatcher,
            ObjectMapper mapper,
            CorsConfigurationSource corsConfigurationSource,
            GreenButtonConfiguration config
    ) throws Exception {
        return http
                .securityMatcher(mvcRequestMatcher.matcher("/**"))    // apply following rules only to requests of this DispatcherServlet
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterBefore(new RequestBodyCachingFilter(), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(mvcRequestMatcher.matcher("/webhook"))
                        .access((authorization, object) -> WebhookVerifier.verifySignature(object.getRequest(), config))
                        .anyRequest().permitAll()
                )
                .exceptionHandling(new SecurityExceptionHandler(mapper))
                .sessionManagement(customizer -> customizer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .build();
    }
}

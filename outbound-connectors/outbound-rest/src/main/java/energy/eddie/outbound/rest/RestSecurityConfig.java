package energy.eddie.outbound.rest;

import energy.eddie.api.agnostic.outbound.OutboundConnectorSecurityConfig;
import energy.eddie.outbound.shared.utils.CommonPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;

@SuppressWarnings("unused")
@OutboundConnectorSecurityConfig
public class RestSecurityConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestSecurityConfig.class);

    @Bean
    @ConditionalOnExpression(value = "${outbound-connector.rest.enabled:false} and ${outbound-connector.rest.oauth2.enabled:false}")
    public PathPatternRequestMatcher.Builder restRequestMatcher() {
        return PathPatternRequestMatcher.withDefaults()
                                        .basePath(CommonPaths.getServletPathForOutboundConnector("rest"));
    }

    @Bean
    @ConditionalOnExpression(value = "${outbound-connector.rest.enabled:false} and ${outbound-connector.rest.oauth2.enabled:false}")
    @SuppressWarnings("java:S4502")
    public SecurityFilterChain restSecurityFilterChain(
            PathPatternRequestMatcher.Builder restRequestMatcher,
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource,
            JwtDecoder decoder,
            @Value("${outbound-connector.rest.oauth2.enabled:false}") boolean oauthEnabled,
            @Value("${outbound-connector.rest.oauth2.scopes.enabled:false}") boolean oauthScopesEnabled
    ) {
        LOGGER.info("OAuth Security enabled for REST outbound connector");
        Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry> authorizeHttpRequestsConfigurer = oauth -> {};
        if (oauthEnabled) {
            if (oauthScopesEnabled) {
                authorizeHttpRequestsConfigurer = this.authorizeHttpRequestsCustomizerWithScopes(restRequestMatcher);
            } else {
                authorizeHttpRequestsConfigurer = this.authorizeHttpRequestsCustomizer();
            }
        }
        return http
                .securityMatcher(restRequestMatcher.matcher("/**"))
                .authorizeHttpRequests(authorizeHttpRequestsConfigurer)
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(decoder)))
                .sessionManagement(customizer -> customizer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .exceptionHandling(eh -> eh.authenticationEntryPoint((request, response, authException) -> {
                    LOGGER.warn("Got security exception", authException);
                    response.addHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Restricted Content\"");
                    response.sendError(HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.getReasonPhrase());
                }))
                .build();
    }

    @Bean
    @ConditionalOnExpression(value = "${outbound-connector.rest.enabled:false} and ${outbound-connector.rest.oauth2.enabled:false}")
    public JwtDecoder jwtDecoder(Environment environment) {
        var issuer = environment.getRequiredProperty("outbound-connector.rest.oauth2.issuer-url", String.class);
        return NimbusJwtDecoder.withIssuerLocation(issuer)
                               .build();
    }

    private Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry> authorizeHttpRequestsCustomizer() {
        return auth -> auth.anyRequest().authenticated();
    }

    private Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry> authorizeHttpRequestsCustomizerWithScopes(
            PathPatternRequestMatcher.Builder restRequestMatcher
    ) {
        return auth -> auth
                // @formatter:off
                .requestMatchers(restRequestMatcher.matcher("/agnostic/connection-status-messages")).hasAuthority("SCOPE_agnostic-connection-status-messages:read")
                .requestMatchers(restRequestMatcher.matcher("/agnostic/raw-data-messages")).hasAuthority("SCOPE_agnostic-raw-data-messages:read")
                .requestMatchers(restRequestMatcher.matcher("/cim_0_82/permission-md")).hasAuthority("SCOPE_cim-permission-md:read")
                .requestMatchers(restRequestMatcher.matcher("/cim_0_82/accounting-point-data-md")).hasAuthority("SCOPE_cim-accounting-point-data-md:read")
                .requestMatchers(restRequestMatcher.matcher("/cim_0_82/validated-historical-data-md")).hasAuthority("SCOPE_cim-validated-historical-data-md:read")
                .requestMatchers(restRequestMatcher.matcher("/cim_1_04/near-real-time-data-md")).hasAuthority("SCOPE_cim-near-real-time-data-md:read")
                .requestMatchers(restRequestMatcher.matcher("/cim_0_82/termination-md")).hasAuthority("SCOPE_cim-termination-md:write")
                .requestMatchers(restRequestMatcher.matcher("/cim_0_91_08/redistribution-transaction-rd")).hasAuthority("SCOPE_cim-redistribution-transaction-rd:write")
                .requestMatchers(restRequestMatcher.matcher("/cim_1_04/validated-historical-data-md")).hasAuthority("SCOPE_cim-validated-historical-data-md:read")
                .requestMatchers(restRequestMatcher.matcher("/v3/api-docs")).permitAll() // Allow requests to the swagger file
                .anyRequest().denyAll();
                // @formatter:on
    }
}

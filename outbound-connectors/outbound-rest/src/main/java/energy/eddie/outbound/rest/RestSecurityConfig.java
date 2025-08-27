package energy.eddie.outbound.rest;

import energy.eddie.api.agnostic.outbound.OutboundConnectorSecurityConfig;
import energy.eddie.outbound.shared.utils.CommonPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

@SuppressWarnings("unused")
@OutboundConnectorSecurityConfig
public class RestSecurityConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestSecurityConfig.class);

    @Bean
    @ConditionalOnExpression(value = "${outbound-connector.rest.enabled:false} and ${outbound-connector.rest.oauth2.enabled:false}")
    public MvcRequestMatcher.Builder restRequestMatcher(HandlerMappingIntrospector introspector) {
        return new MvcRequestMatcher.Builder(introspector).servletPath(CommonPaths.getServletPathForOutboundConnector("rest"));
    }

    @Bean
    @ConditionalOnExpression(value = "${outbound-connector.rest.enabled:false} and ${outbound-connector.rest.oauth2.enabled:false}")
    @SuppressWarnings("java:S4502")
    public SecurityFilterChain restSecurityFilterChain(
            MvcRequestMatcher.Builder restRequestMatcher,
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource,
            JwtDecoder decoder
    ) throws Exception {
        LOGGER.info("OAuth Security enabled for REST outbound connector");
        return http
                .securityMatcher(restRequestMatcher.pattern("/**"))    // apply following rules only to requests of this DispatcherServlet
                .authorizeHttpRequests(auth -> auth
                                               .anyRequest().authenticated()
                                       //.dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.REQUEST).permitAll()
                )
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
}

package energy.eddie.core.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.regionconnector.shared.security.JwtAuthorizationManager;
import energy.eddie.regionconnector.shared.security.JwtUtil;
import energy.eddie.spring.regionconnector.extensions.SecurityExceptionHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

import static energy.eddie.regionconnector.shared.utils.CommonPaths.ALL_REGION_CONNECTORS_BASE_URL_PATH;

@Configuration
@SuppressWarnings("java:S4502")
public class SecurityConfig {

    @Bean
    public MvcRequestMatcher.Builder regionConnectorMvcRequestMatcher(HandlerMappingIntrospector introspector) {
        return new MvcRequestMatcher.Builder(introspector)
                .servletPath("/" + ALL_REGION_CONNECTORS_BASE_URL_PATH);
    }

    @Bean
    public SecurityFilterChain regionConnectorSecurityFilterChain(
            @Qualifier("regionConnectorMvcRequestMatcher") MvcRequestMatcher.Builder mvcRequestMatcher,
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource,
            ObjectMapper mapper,
            JwtUtil util
    ) throws Exception {
        return http
                .securityMatcher(mvcRequestMatcher.pattern("/**"))
                .addFilterBefore(new JwtIssuerFilter(mapper, util), AnonymousAuthenticationFilter.class)
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(new SecurityExceptionHandler(mapper))
                .sessionManagement(customizer -> customizer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .build();
    }

    @Bean
    public SecurityFilterChain connectionStatusMessageFilterChain(
            HttpSecurity http,
            JwtAuthorizationManager jwtHeaderAuthorizationManager,
            CorsConfigurationSource corsConfigurationSource,
            ObjectMapper mapper,
            HandlerMappingIntrospector introspector
    ) throws Exception {
        MvcRequestMatcher.Builder mvcMatcherBuilder = new MvcRequestMatcher.Builder(introspector);

        return http
                .securityMatcher("/api/connection-status-messages/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(mvcMatcherBuilder.pattern("/api/connection-status-messages/{permissionId}"))
                        .access(jwtHeaderAuthorizationManager)
                        .anyRequest().denyAll()
                )
                .exceptionHandling(new SecurityExceptionHandler(mapper))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .build();
    }
}

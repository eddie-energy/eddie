package energy.eddie.core.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.regionconnector.shared.security.JwtAuthorizationManager;
import energy.eddie.spring.regionconnector.extensions.SecurityExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

@Configuration
@SuppressWarnings("java:S4502")
public class SecurityConfig {

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
                        // @formatter:off
                        .requestMatchers(mvcMatcherBuilder.pattern("/api/connection-status-messages/{permissionId}")).access(jwtHeaderAuthorizationManager)
                        .anyRequest().denyAll()
                       // @formatter
                )
                .exceptionHandling(new SecurityExceptionHandler(mapper))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .build();
    }
}

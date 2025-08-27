package energy.eddie.core;

import energy.eddie.regionconnector.shared.security.JwtAuthorizationManager;
import energy.eddie.regionconnector.shared.security.JwtUtil;
import energy.eddie.regionconnector.shared.timeout.TimeoutConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class CoreSecurityConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(CoreSecurityConfig.class);

    /**
     * Add a custom security filter chain for core, otherwise Spring will create a default one.
     */
    @Bean
    public SecurityFilterChain coreSecurityFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource
    ) throws Exception {
        http.securityMatcher("/lib/**")
            .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
            .cors(cors -> cors.configurationSource(corsConfigurationSource));

        return http.build();
    }

    /**
     * Generate admin user for basic login to admin console or provide a custom {@link InMemoryUserDetailsManager} to avoid Spring auto-generating a user.
     */
    @Bean
    public InMemoryUserDetailsManager userDetailsService(
            @Value("${outbound-connector.admin.console.login.mode:false}") String loginMode,
            @Value("${outbound-connector.admin.console.login.username:admin}") String username,
            @Value("${outbound-connector.admin.console.login.encoded-password:$2a$10$qYTmwhGa3dd7Sl1CdXKKHOfmf0lNXL3L2k4CVhhm3CfY131hrcEyS}") String encodedPassword
    ) {
        if ("basic".equalsIgnoreCase(loginMode)) {
            var user = User.builder()
                           .username(username)
                           .password(encodedPassword)
                           .roles("ADMIN")
                           .build();
            return new InMemoryUserDetailsManager(user);
        }
        return new InMemoryUserDetailsManager();
    }

    @Bean
    public JwtUtil jwtUtil(
            @Value("${eddie.jwt.hmac.secret}") String jwtHmacSecret,
            TimeoutConfiguration timeoutConfiguration
    ) {
        return new JwtUtil(jwtHmacSecret, timeoutConfiguration.duration());
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${eddie.cors.allowed-origins:}") String allowedCorsOrigins) {
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
        configuration.setAllowedHeaders(List.of("content-type", "Authorization"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public JwtAuthorizationManager jwtHeaderAuthorizationManager(JwtUtil jwtUtil) {
        return new JwtAuthorizationManager(jwtUtil);
    }
}

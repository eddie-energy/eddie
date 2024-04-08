package energy.eddie.core;

import energy.eddie.regionconnector.shared.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class CoreSecurityConfig {
    /**
     * Add a custom security filter chain for core, otherwise Spring will create a default one.
     */
    @Bean
    public SecurityFilterChain coreSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/**")
            .authorizeHttpRequests(requests -> requests.anyRequest().permitAll());

        return http.build();
    }

    /**
     * Provide a custom {@link InMemoryUserDetailsManager} to avoid Spring auto-generating a user.
     */
    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        return new InMemoryUserDetailsManager();
    }

    @Bean
    public JwtUtil jwtUtil(@Value("${eddie.jwt.hmac.secret}") String jwtHmacSecret) {
        return new JwtUtil(jwtHmacSecret);
    }
}

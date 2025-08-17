package energy.eddie.outbound.admin.console.config;

import energy.eddie.api.agnostic.outbound.OutboundConnectorSecurityConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

import static energy.eddie.outbound.admin.console.config.AdminConsoleConfig.*;


@OutboundConnectorSecurityConfig
public class AdminConsoleSecurityConfig {
    public static final String ADMIN_CONSOLE_BASE_URL = "/outbound-connectors/admin-console";

    @Bean
    public MvcRequestMatcher.Builder adminConsoleRequestMatcher(HandlerMappingIntrospector introspector) {
        return new MvcRequestMatcher.Builder(introspector).servletPath(ADMIN_CONSOLE_BASE_URL);
    }

    @Bean
    @ConditionalOnProperty(value = LOGIN_ENABLED, havingValue = "true")
    public SecurityFilterChain loginEnabledSecurityFilterChain(
            HttpSecurity http,
            MvcRequestMatcher.Builder adminConsoleRequestMatcher,
            @Value("${" + LOGIN_USERNAME + "}") String adminUsername,
            @Value("${" + LOGIN_ENCODED_PASSWORD + "}") String adminEncodedPassword
    ) throws Exception {
        return http
                .csrf(csrf -> csrf.requireCsrfProtectionMatcher(
                        adminConsoleRequestMatcher.pattern("*")
                ))
                .authorizeHttpRequests(authorize -> authorize
                        // Allow access to static resources used by the login page
                        .requestMatchers(adminConsoleRequestMatcher.pattern("/static/**")).permitAll()
                        .requestMatchers(adminConsoleRequestMatcher.pattern("**")).authenticated()
                        .anyRequest().permitAll()
                )
                .httpBasic(Customizer.withDefaults())
                .formLogin(formLogin -> formLogin
                        .loginPage(ADMIN_CONSOLE_BASE_URL + "/login")
                        .defaultSuccessUrl(ADMIN_CONSOLE_BASE_URL)
                        .failureUrl(ADMIN_CONSOLE_BASE_URL + "/login?error")
                        .permitAll()
                )
                .logout(LogoutConfigurer::permitAll)
                .userDetailsService(new InMemoryUserDetailsManager(
                        User.builder()
                            .username(adminUsername)
                            .password(adminEncodedPassword)
                            .build())
                )
                .build();
    }

    @Bean
    @ConditionalOnProperty(value = LOGIN_ENABLED, havingValue = "true")
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

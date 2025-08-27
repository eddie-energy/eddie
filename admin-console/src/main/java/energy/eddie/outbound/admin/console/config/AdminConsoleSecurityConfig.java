package energy.eddie.outbound.admin.console.config;

import energy.eddie.api.agnostic.outbound.OutboundConnectorSecurityConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

import static energy.eddie.outbound.shared.utils.CommonPaths.getServletPathForOutboundConnector;


@OutboundConnectorSecurityConfig
public class AdminConsoleSecurityConfig {
    public static final String ADMIN_CONSOLE_BASE_URL = getServletPathForOutboundConnector("admin-console");

    @Bean
    public MvcRequestMatcher.Builder adminConsoleRequestMatcher(HandlerMappingIntrospector introspector) {
        return new MvcRequestMatcher.Builder(introspector).servletPath(ADMIN_CONSOLE_BASE_URL);
    }

    @Bean
    @ConditionalOnProperty(value = "outbound-connector.admin.console.login.mode")
    public SecurityFilterChain loginEnabledSecurityFilterChain(
            HttpSecurity http,
            MvcRequestMatcher.Builder adminConsoleRequestMatcher,
            @Value("${outbound-connector.admin.console.login.mode}") String authMode
    ) throws Exception {
        http
                .csrf(csrf -> csrf.requireCsrfProtectionMatcher(
                        adminConsoleRequestMatcher.pattern("*")
                ))
                .authorizeHttpRequests(authorize -> authorize
                        // Allow access to static resources used by the login page
                        .requestMatchers(adminConsoleRequestMatcher.pattern("/static/**")).permitAll()
                        .requestMatchers(adminConsoleRequestMatcher.pattern("**")).authenticated()
                        .anyRequest().permitAll()
                )
                .logout(LogoutConfigurer::permitAll);

        switch (authMode) {
            case "basic" -> http
                    .httpBasic(Customizer.withDefaults())
                    .formLogin(formLogin -> formLogin
                            .loginPage(ADMIN_CONSOLE_BASE_URL + "/login")
                            .defaultSuccessUrl(ADMIN_CONSOLE_BASE_URL)
                            .failureUrl(ADMIN_CONSOLE_BASE_URL + "/login?error")
                            .permitAll()
                    );
            case "keycloak" -> http
                    .oauth2Login(Customizer.withDefaults());
            default -> throw new IllegalStateException(
                    "Unsupported value for outbound-connector.admin.console.login.mode: " + authMode);
        }

        return http.build();
    }

    @Bean
    @ConditionalOnProperty(value = "outbound-connector.admin.console.login.mode", havingValue = "basic")
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @ConditionalOnProperty(value = "outbound-connector.admin.console.login.mode", havingValue = "keycloak")
    public ClientRegistrationRepository clientRegistrationRepository(
            @Value("${outbound-connector.admin.console.login.keycloak.client-id}") String clientId,
            @Value("${outbound-connector.admin.console.login.keycloak.client-secret}") String clientSecret,
            @Value("${outbound-connector.admin.console.login.keycloak.issuer-uri}") String issuerUri,
            @Value("${eddie.management.url}") String managementUrl
    ) {
        return new InMemoryClientRegistrationRepository(
                ClientRegistration.withRegistrationId("keycloak")
                                  .clientName("Keycloak")
                                  .clientId(clientId)
                                  .clientSecret(clientSecret)
                                  .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                                  .redirectUri(managementUrl + ADMIN_CONSOLE_BASE_URL + "/login/oauth2/code/keycloak")
                                  .scope("openid")
                                  .authorizationUri(issuerUri + "/protocol/openid-connect/auth")
                                  .tokenUri(issuerUri + "/protocol/openid-connect/token")
                                  .userInfoUri(issuerUri + "/protocol/openid-connect/userinfo")
                                  .userNameAttributeName("preferred_username")
                                  .jwkSetUri(issuerUri + "/protocol/openid-connect/certs")
                                  .build());
    }
}

package energy.eddie.aiida.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Map;


@Configuration
@EnableWebSecurity
public class OAuth2SecurityConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(OAuth2SecurityConfiguration.class);

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            ClientRegistrationRepository clientRegistrationRepository,
            CorsConfigurationSource corsConfigurationSource
    ) throws Exception {
        return http
                .csrf(csrf ->
                              csrf.ignoringRequestMatchers("/webhook/event")
                                  .ignoringRequestMatchers("/mqtt-auth/**")
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .oauth2Login(oauth2 -> oauth2.loginPage("/login"))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .logout(logout -> {
                    var logoutSuccessHandler = new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
                    logoutSuccessHandler.setPostLogoutRedirectUri("{baseUrl}/login");
                    logout.logoutSuccessHandler(logoutSuccessHandler);
                })
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/assets/**",
                                "/favicon.svg",
                                "/vue",
                                "/vue/**",
                                "/login",
                                "/mqtt-auth/**",
                                "/actuator/health/**",
                                "/application-information",
                                "/img/aiida.svg",
                                "/img/icon.svg",
                                "/css/style.css",
                                "/css/login.css",
                                "/webhook/event",
                                "/js/main.js")
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

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(
            OAuth2ClientProperties oAuth2ClientProperties,
            KeycloakConfiguration keycloakConfiguration
    ) {
        var clientPropertiesMapper = new OAuth2ClientPropertiesMapper(oAuth2ClientProperties);
        var clientRegistrations = clientPropertiesMapper
                .asClientRegistrations()
                .entrySet()
                .stream()
                .map(entry -> entry.getKey().equals("keycloak")
                        ? customizeKeycloakRegistration(entry.getValue(), keycloakConfiguration)
                        : entry.getValue())
                .toList();

        return new InMemoryClientRegistrationRepository(clientRegistrations);
    }

    private ClientRegistration customizeKeycloakRegistration(
            ClientRegistration clientRegistration,
            KeycloakConfiguration keycloakConfiguration
    ) {
        return ClientRegistration
                .withClientRegistration(clientRegistration)
                .providerConfigurationMetadata(Map.of("end_session_endpoint", keycloakConfiguration.endSessionUri()))
                .build();
    }
}

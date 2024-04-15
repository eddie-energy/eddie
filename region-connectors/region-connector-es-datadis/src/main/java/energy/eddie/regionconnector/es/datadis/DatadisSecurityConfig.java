package energy.eddie.regionconnector.es.datadis;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.EddieApiError;
import energy.eddie.api.agnostic.RegionConnectorSecurityConfig;
import energy.eddie.regionconnector.shared.security.JwtAuthorizationManager;
import energy.eddie.regionconnector.shared.security.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

import java.util.List;
import java.util.Map;

import static energy.eddie.api.agnostic.GlobalConfig.ERRORS_PROPERTY_NAME;
import static energy.eddie.regionconnector.es.datadis.web.PermissionController.PATH_PERMISSION_ACCEPTED;
import static energy.eddie.regionconnector.es.datadis.web.PermissionController.PATH_PERMISSION_REJECTED;
import static energy.eddie.regionconnector.shared.utils.CommonPaths.ALL_REGION_CONNECTORS_BASE_URL_PATH;
import static energy.eddie.regionconnector.shared.utils.CommonPaths.CE_FILE_NAME;
import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_REQUEST;
import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_STATUS_WITH_PATH_PARAM;

@RegionConnectorSecurityConfig
public class DatadisSecurityConfig {
    private static final String DATADIS_ENABLED_PROPERTY = "region-connector.es.datadis.enabled";

    @Bean
    @ConditionalOnProperty(value = DATADIS_ENABLED_PROPERTY, havingValue = "true")
    public JwtAuthorizationManager datadisAuthorizationManager(JwtUtil jwtUtil) {
        return new JwtAuthorizationManager(jwtUtil);
    }

    @Bean
    @ConditionalOnProperty(value = DATADIS_ENABLED_PROPERTY, havingValue = "true")
    public MvcRequestMatcher.Builder datadisMvcRequestMatcher(HandlerMappingIntrospector introspector) {
        return new MvcRequestMatcher.Builder(introspector).servletPath(
                "/" + ALL_REGION_CONNECTORS_BASE_URL_PATH + "/" + DatadisRegionConnectorMetadata.REGION_CONNECTOR_ID);
    }

    @Bean
    @ConditionalOnProperty(value = DATADIS_ENABLED_PROPERTY, havingValue = "true")
    public SecurityFilterChain datadisSecurityFilterChain(
            MvcRequestMatcher.Builder datadisMvcRequestMatcher,
            HttpSecurity http,
            JwtAuthorizationManager datadisAuthorizationManager,
            CorsConfigurationSource corsConfigurationSource,
            ObjectMapper mapper
    ) throws Exception {
        return http
                .securityMatcher(datadisMvcRequestMatcher.pattern("/**"))    // apply following rules only to requests of this DispatcherServlet
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
// @formatter:off   all calls for one request pattern should be on one line
                        .requestMatchers(datadisMvcRequestMatcher.pattern(PATH_PERMISSION_REQUEST)).permitAll()
                        .requestMatchers(datadisMvcRequestMatcher.pattern(PATH_PERMISSION_STATUS_WITH_PATH_PARAM)).permitAll()
                        .requestMatchers(datadisMvcRequestMatcher.pattern(PATH_PERMISSION_ACCEPTED)).access(datadisAuthorizationManager)
                        .requestMatchers(datadisMvcRequestMatcher.pattern(PATH_PERMISSION_REJECTED)).access(datadisAuthorizationManager)
                        .requestMatchers(datadisMvcRequestMatcher.pattern("/" + CE_FILE_NAME)).permitAll()
                        .anyRequest().denyAll()
// @formatter:on
                )
                .exceptionHandling(customizer -> customizer
                        .authenticationEntryPoint((request, response, authException) -> {
                            // we cannot properly differentiate between unauthenticated/unauthorized because we are not directly using Spring's security context, so no specific error message can be given
                            var errors = Map.of(ERRORS_PROPERTY_NAME,
                                                List.of(new EddieApiError(
                                                        "Not authenticated/authorized to access the requested resource")));

                            // when using response.sendError(), the message will be overwritten
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write(mapper.writeValueAsString(errors));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            var errors = Map.of(ERRORS_PROPERTY_NAME,
                                                List.of(new EddieApiError(
                                                        "Not authorized to access the requested resource")));
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write(mapper.writeValueAsString(errors));
                        })
                )
                .sessionManagement(customizer -> customizer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .build();
    }
}

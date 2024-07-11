package energy.eddie.regionconnector.fi.fingrid;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.RegionConnectorSecurityConfig;
import energy.eddie.regionconnector.shared.security.JwtAuthorizationManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

import static energy.eddie.regionconnector.shared.web.RestApiPaths.*;
import static energy.eddie.spring.regionconnector.extensions.SecurityUtils.mvcRequestMatcher;
import static energy.eddie.spring.regionconnector.extensions.SecurityUtils.securityFilterChain;

@RegionConnectorSecurityConfig
public class FingridSecurityConfig {
    private static final String FINGRID_ENABLED_PROPERTY = "region-connector.fi.fingrid.enabled";

    @Bean
    @ConditionalOnProperty(value = FINGRID_ENABLED_PROPERTY, havingValue = "true")
    public MvcRequestMatcher.Builder fingridMvcRequestMatcher(HandlerMappingIntrospector introspector) {
        return mvcRequestMatcher(introspector, FingridRegionConnectorMetadata.REGION_CONNECTOR_ID);
    }

    @Bean
    @ConditionalOnProperty(value = FINGRID_ENABLED_PROPERTY, havingValue = "true")
    @SuppressWarnings("java:S4502")
    public SecurityFilterChain fingridSecurityFilterChain(
            MvcRequestMatcher.Builder fingridMvcRequestMatcher,
            HttpSecurity http,
            JwtAuthorizationManager jwtHeaderAuthorizationManager,
            CorsConfigurationSource corsConfigurationSource,
            ObjectMapper mapper
    ) throws Exception {
        return securityFilterChain(fingridMvcRequestMatcher,
                                   http,
                                   jwtHeaderAuthorizationManager,
                                   corsConfigurationSource,
                                   mapper,
                                   PATH_PERMISSION_ACCEPTED,
                                   PATH_PERMISSION_REJECTED,
                                   "/organisation-information"
        );
    }
}

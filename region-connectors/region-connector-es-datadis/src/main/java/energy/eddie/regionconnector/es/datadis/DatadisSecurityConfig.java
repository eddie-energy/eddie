package energy.eddie.regionconnector.es.datadis;

import energy.eddie.api.agnostic.RegionConnectorSecurityConfig;
import energy.eddie.regionconnector.shared.security.JwtAuthorizationManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_ACCEPTED;
import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_REJECTED;
import static energy.eddie.spring.regionconnector.extensions.SecurityUtils.pathPatternRequestMatcher;
import static energy.eddie.spring.regionconnector.extensions.SecurityUtils.securityFilterChain;

@RegionConnectorSecurityConfig
public class DatadisSecurityConfig {
    private static final String DATADIS_ENABLED_PROPERTY = "region-connector.es.datadis.enabled";

    @Bean
    @ConditionalOnProperty(value = DATADIS_ENABLED_PROPERTY, havingValue = "true")
    public PathPatternRequestMatcher.Builder datadisRequestMatcher() {
        return pathPatternRequestMatcher(DatadisRegionConnectorMetadata.REGION_CONNECTOR_ID);
    }

    @Bean
    @ConditionalOnProperty(value = DATADIS_ENABLED_PROPERTY, havingValue = "true")
    public SecurityFilterChain datadisSecurityFilterChain(
            PathPatternRequestMatcher.Builder datadisMvcRequestMatcher,
            HttpSecurity http,
            JwtAuthorizationManager jwtHeaderAuthorizationManager,
            CorsConfigurationSource corsConfigurationSource,
            ObjectMapper mapper
    ) throws Exception {
        return securityFilterChain(datadisMvcRequestMatcher,
                                   http,
                                   jwtHeaderAuthorizationManager,
                                   corsConfigurationSource,
                                   mapper,
                                   List.of(PATH_PERMISSION_ACCEPTED, PATH_PERMISSION_REJECTED),
                                   List.of());
    }
}

package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.regionconnector.shared.security.JwtAuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

import static energy.eddie.regionconnector.shared.utils.CommonPaths.ALL_REGION_CONNECTORS_BASE_URL_PATH;
import static energy.eddie.regionconnector.shared.utils.CommonPaths.CE_FILE_NAME;
import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_REQUEST;
import static energy.eddie.regionconnector.shared.web.RestApiPaths.SWAGGER_DOC_PATH;

/**
 * This class provides methods that create a security config for region-connectors that require a manuel response by the
 * final customers. For example, the Fingrid and Datadis region-connector.
 */
public class SecurityUtils {
    private SecurityUtils() {}

    @SuppressWarnings("java:S4502")
    public static SecurityFilterChain securityFilterChain(
            PathPatternRequestMatcher.Builder patternRequestMatcher,
            HttpSecurity http,
            JwtAuthorizationManager jwtHeaderAuthorizationManager,
            CorsConfigurationSource corsConfigurationSource,
            ObjectMapper mapper,
            Iterable<String> authorizationPaths,
            Iterable<String> publicPaths
    ) {
        return http
                .securityMatcher(patternRequestMatcher.matcher("/**"))    // apply following rules only to requests of this DispatcherServlet
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> configureAuthorization(patternRequestMatcher,
                                                                      jwtHeaderAuthorizationManager,
                                                                      auth,
                                                                      authorizationPaths,
                                                                      publicPaths)
                )
                .exceptionHandling(new SecurityExceptionHandler(mapper))
                .sessionManagement(customizer -> customizer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .build();
    }

    public static PathPatternRequestMatcher.Builder pathPatternRequestMatcher(
            String regionConnectorId
    ) {
        return PathPatternRequestMatcher.withDefaults()
                                        .basePath("/" + ALL_REGION_CONNECTORS_BASE_URL_PATH + "/" + regionConnectorId);
    }

    private static void configureAuthorization(
            PathPatternRequestMatcher.Builder patternRequestMatcher,
            JwtAuthorizationManager jwtHeaderAuthorizationManager,
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth,
            Iterable<String> authorizationPaths,
            Iterable<String> publicPaths
    ) {
        auth.requestMatchers(patternRequestMatcher.matcher(PATH_PERMISSION_REQUEST)).permitAll();

        for (String path : authorizationPaths) {
            auth.requestMatchers(patternRequestMatcher.matcher(path)).access(jwtHeaderAuthorizationManager);
        }

        for (String path : publicPaths) {
            auth.requestMatchers(patternRequestMatcher.matcher(path)).permitAll();
        }

        auth
                .requestMatchers(patternRequestMatcher.matcher("/" + CE_FILE_NAME)).permitAll()
                .requestMatchers(patternRequestMatcher.matcher("/" + SWAGGER_DOC_PATH)).permitAll()
                .anyRequest().denyAll();
    }
}

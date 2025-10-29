package energy.eddie.spring.regionconnector.extensions;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.regionconnector.shared.security.JwtAuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

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
            MvcRequestMatcher.Builder mvcRequestMatcher,
            HttpSecurity http,
            JwtAuthorizationManager jwtHeaderAuthorizationManager,
            CorsConfigurationSource corsConfigurationSource,
            ObjectMapper mapper,
            Iterable<String> authorizationPaths,
            Iterable<String> publicPaths
    ) throws Exception {
        return http
                .securityMatcher(mvcRequestMatcher.pattern("/**"))    // apply following rules only to requests of this DispatcherServlet
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> configureAuthorization(mvcRequestMatcher,
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

    public static MvcRequestMatcher.Builder mvcRequestMatcher(
            HandlerMappingIntrospector introspector,
            String regionConnectorId
    ) {
        return new MvcRequestMatcher.Builder(introspector)
                .servletPath("/" + ALL_REGION_CONNECTORS_BASE_URL_PATH + "/" + regionConnectorId);
    }

    private static void configureAuthorization(
            MvcRequestMatcher.Builder mvcRequestMatcher,
            JwtAuthorizationManager jwtHeaderAuthorizationManager,
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth,
            Iterable<String> authorizationPaths,
            Iterable<String> publicPaths
    ) {
        auth.requestMatchers(mvcRequestMatcher.pattern(PATH_PERMISSION_REQUEST)).permitAll();

        for (String path : authorizationPaths) {
            auth.requestMatchers(mvcRequestMatcher.pattern(path)).access(jwtHeaderAuthorizationManager);
        }

        for (String path : publicPaths) {
            auth.requestMatchers(mvcRequestMatcher.pattern(path)).permitAll();
        }

        auth
                .requestMatchers(mvcRequestMatcher.pattern("/" + CE_FILE_NAME)).permitAll()
                .requestMatchers(mvcRequestMatcher.pattern("/" + SWAGGER_DOC_PATH)).permitAll()
                .anyRequest().denyAll();
    }
}

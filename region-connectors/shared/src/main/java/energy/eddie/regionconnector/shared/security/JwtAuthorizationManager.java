package energy.eddie.regionconnector.shared.security;

import jakarta.servlet.http.Cookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Manages authorization by checking if the requested permissionId is saved in the JWT token that is supplied as a
 * cookie in the request. The JWT's signature is validated to prevent the acceptance of tampered tokens. The
 * permissionId is expected to be a path variable of the request URL.
 */
public class JwtAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthorizationManager.class);
    private final JwtUtil jwtUtil;

    public JwtAuthorizationManager(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        String requestURI = context.getRequest().getRequestURI();


        Optional<Cookie> jwtCookie = Optional.ofNullable(context.getRequest().getCookies())
                                             .map(Arrays::asList)
                                             .orElse(Collections.emptyList())
                                             .stream()
                                             .filter(cookie -> cookie.getName().equals(JwtUtil.JWT_COOKIE_NAME))
                                             .findFirst();

        if (jwtCookie.isEmpty()) {
            LOGGER.trace("Denying authorization for request URI {} because no JWT cookie was included in the request",
                         requestURI);
            return new AuthorizationDecision(false);
        }

        var permissions = jwtUtil.getPermissions(jwtCookie.get().getValue());


        String requestedPermissionId = context.getVariables().get("permissionId");
        String requestedConnectorId = context.getRequest().getHttpServletMapping().getServletName();
        List<String> permittedPermissionsForRequestedConnector = permissions.get(requestedConnectorId);

        if (requestedPermissionId == null || requestedConnectorId == null || permittedPermissionsForRequestedConnector == null) {
            LOGGER.trace(
                    "Denying authorization for request URI {} because one of the required fields is null ({} {} {})",
                    requestURI,
                    requestedPermissionId,
                    requestedConnectorId,
                    permittedPermissionsForRequestedConnector);
            return new AuthorizationDecision(false);
        }

        if (!permittedPermissionsForRequestedConnector.contains(requestedPermissionId)) {
            LOGGER.trace(
                    "Denying authorization for request URI {} because the requested permissionId is not in the JWT ({})",
                    requestURI,
                    permissions);
            return new AuthorizationDecision(false);
        }

        return new AuthorizationDecision(true);
    }
}

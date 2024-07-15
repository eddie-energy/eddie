package energy.eddie.regionconnector.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.List;
import java.util.function.Supplier;

public class JwtAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {
    public static final String BEARER_PREFIX = "Bearer ";
    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthorizationManager.class);
    private final JwtUtil jwtUtil;

    /**
     * Creates a new {@link AuthorizationManager} that enforces authorization by checking the JWT that is supplied with
     * the request. It checks whether the combination of region connector ID and permission ID from the request URL are
     * contained in the JWT and is therefore only suitable for requests where the permissionId is a path parameter of
     * the request. For example, for a request with the URL
     * {@code /region-connectors/es-datadis/permission-request/exampleId/rejected} to be allowed, the list of
     * permissions stored in the JWT has to contain the ID {@code exampleId} associated with the region connector
     * {@code es-datadis}.
     * <br>
     * The JWT's signature is validated to prevent the acceptance of tampered tokens.
     *
     * @param jwtUtil   {@link JwtUtil} used to parse and validate the JWTs.
     */
    public JwtAuthorizationManager(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        String requestURI = context.getRequest().getRequestURI();
        String jwt = getJwtFromHeader(context.getRequest());

        var permissions = jwtUtil.getPermissions(jwt);

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
            throw new AccessDeniedException("Not authorized to access the requested resource");
        }

        if (!permittedPermissionsForRequestedConnector.contains(requestedPermissionId)) {
            LOGGER.trace(
                    "Denying authorization for request URI {} because the requested permissionId is not in the JWT ({})",
                    requestURI,
                    permissions);
            throw new AccessDeniedException("Not authorized to access the requested resource");
        }

        return new AuthorizationDecision(true);
    }

    /**
     * Reads and returns the JWT stored in the {@value HttpHeaders#AUTHORIZATION} header.
     *
     * @throws AccessDeniedException Thrown if the request has no {@value HttpHeaders#AUTHORIZATION} header or its value
     *                               does not start with {@value #BEARER_PREFIX}.
     */
    private String getJwtFromHeader(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null || !authorizationHeader.trim().startsWith(BEARER_PREFIX)) {
            LOGGER.trace("Denying authorization for request URI {} because no JWT was included as " + HttpHeaders.AUTHORIZATION + " header",
                         request.getRequestURI());
            throw new AccessDeniedException("No header with JWT provided");
        }

        return authorizationHeader.substring(BEARER_PREFIX.length());
    }
}

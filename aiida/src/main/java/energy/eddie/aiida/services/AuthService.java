package energy.eddie.aiida.services;

import energy.eddie.aiida.errors.UnauthorizedException;
import energy.eddie.aiida.models.permission.Permission;
import io.micrometer.common.lang.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);

    public @Nullable UUID getCurrentUserId() {
        var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof OidcUser) {
            var subject = ((OidcUser) principal).getSubject();
            var uuid = UUID.fromString(subject);
            LOGGER.debug("Successfully Parsed UUID ({}) from token!", uuid);
            return uuid;
        }

        LOGGER.error("Could not parse UUID from token, because the user is not an OIDC user");
        return null;
    }

    public void checkAuthorizationForPermission(Permission permission) throws UnauthorizedException {
        var currentUserId = getCurrentUserId();
        if (currentUserId == null || !currentUserId.equals(permission.userId())) {
            throw new UnauthorizedException("User is not permitted to access this Permission!");
        }
    }
}

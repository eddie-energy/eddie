// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.errors.auth.InvalidUserException;
import energy.eddie.aiida.errors.auth.UnauthorizedException;
import energy.eddie.aiida.models.permission.Permission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);

    public UUID getCurrentUserId() throws InvalidUserException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            LOGGER.error("No authentication present for this context");
            throw new InvalidUserException();
        }
        var principal = authentication.getPrincipal();
        var id = switch (principal) {
            case Jwt jwt -> jwt.getSubject();
            case OidcUser oidcUser -> oidcUser.getSubject();
            case null, default -> {
                LOGGER.error("Could not parse UUID from token, because the user is not an OIDC user");
                throw new InvalidUserException();
            }
        };

        try {
            var uuid = UUID.fromString(id);
            LOGGER.debug("Successfully Parsed UUID ({}) from token!", uuid);
            return uuid;
        } catch (IllegalArgumentException ex) {
            LOGGER.error("Could not parse UUID from token, because the OIDC User has not valid UUID!");
            throw new InvalidUserException();
        }
    }

    public void checkAuthorizationForPermission(Permission permission) throws UnauthorizedException, InvalidUserException {
        var currentUserId = getCurrentUserId();
        if (!currentUserId.equals(permission.userId())) {
            throw new UnauthorizedException("User is not permitted to access this Permission!");
        }
    }
}

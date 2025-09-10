package energy.eddie.aiida.services;

import energy.eddie.aiida.errors.InvalidUserException;
import energy.eddie.aiida.errors.UnauthorizedException;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.api.agnostic.aiida.QrCodeDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {
    private final String uuid = "dc9ff3d3-1f1f-445d-a4ee-85c1faffb715";
    private final UUID userId = UUID.fromString(uuid);
    private final QrCodeDto qrCodeDto = new QrCodeDto(UUID.randomUUID(),
                                                      UUID.randomUUID(),
                                                      "Test Service Name",
                                                      "https://example.org");
    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService();

        OidcUser oidcUser = createOidcUserWithClaims(Map.of("sub", uuid, "email", "test@eddie.eu"));
        configureSecurityContextForPrincipal(oidcUser);
    }

    @Test
    void givenValidPermissionForUser_checkAuthorization_notThrows() {
        // Given
        Permission permission = new Permission(qrCodeDto, userId);

        // When, Then
        assertDoesNotThrow(() -> service.checkAuthorizationForPermission(permission));
    }

    @Test
    void givenInValidPermissionForUser_checkAuthorization_Throws() {
        // Given
        Permission permission = new Permission(qrCodeDto, UUID.randomUUID());

        // When, Then
        assertThrows(UnauthorizedException.class, () -> service.checkAuthorizationForPermission(permission));
    }

    @Test
    void givenValidToken_getCurrentUserId() throws InvalidUserException {
        // Given (configured in setUp)

        // When
        var userIdFromToken = service.getCurrentUserId();

        // Then
        assertEquals(userIdFromToken, userId);
    }

    @Test
    void givenInvalidUUID_getCurrentUserId_throws() {
        // Given
        OidcUser oidcUser = createOidcUserWithClaims(Map.of("email", "test@eddie.eu", "sub", "invalidUUID"));
        configureSecurityContextForPrincipal(oidcUser);

        // When, Then
        assertThrows(InvalidUserException.class, () -> service.getCurrentUserId());
    }

    @Test
    void givenInvalidUser_getCurrentUserId_throws() {
        // Given
        User principal = new User("testUser", "password", new ArrayList<>());
        configureSecurityContextForPrincipal(principal);

        // When, Then
        assertThrows(InvalidUserException.class, () -> service.getCurrentUserId());
    }

    private OidcUser createOidcUserWithClaims(Map<String, Object> claims) {
        return new DefaultOidcUser(List.of(),
                                   new OidcIdToken("tokenValue",
                                                   Instant.now(),
                                                   Instant.now().plusSeconds(60 * 5),
                                                   claims));
    }

    private void configureSecurityContextForPrincipal(Object principal) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);
    }
}
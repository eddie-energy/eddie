package energy.eddie.regionconnector.shared.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletMapping;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthorizationManagerTest {
    @Mock
    private JwtUtil mockJwtUtil;
    @Mock
    private RequestAuthorizationContext mockContext;
    private JwtAuthorizationManager authManager;
    @Mock
    private HttpServletRequest mockRequest;
    @Mock
    private Cookie mockCookie;
    @Mock
    private HttpServletMapping mockServletMapping;

    @BeforeEach
    void setUp() {
        authManager = new JwtAuthorizationManager(mockJwtUtil);
        when(mockContext.getRequest()).thenReturn(mockRequest);
    }

    @Test
    void givenNoCookie_returnsDenied() {
        // Given
        when(mockRequest.getCookies()).thenReturn(null);

        // When, Then
        assertThrows(AccessDeniedException.class, () -> authManager.check(null, mockContext));
    }

    @Test
    void givenInvalidJwt_returnsDenied() {
        // Given
        when(mockRequest.getCookies()).thenReturn(new Cookie[]{mockCookie});
        when(mockCookie.getName()).thenReturn(JwtUtil.JWT_COOKIE_NAME);
        when(mockCookie.getValue()).thenReturn("lkjsdfkjsdjkdsjkhf");
        when(mockRequest.getHttpServletMapping()).thenReturn(mockServletMapping);
        when(mockServletMapping.getServletName()).thenReturn("foo");
        when(mockJwtUtil.getPermissions(anyString())).thenReturn(Collections.emptyMap());

        // When, Then
        assertThrows(AccessDeniedException.class, () -> authManager.check(null, mockContext));
    }

    @Test
    void givenNoPermissionId_returnsDenied() {
        // Given
        when(mockRequest.getCookies()).thenReturn(new Cookie[]{mockCookie});
        when(mockCookie.getName()).thenReturn(JwtUtil.JWT_COOKIE_NAME);
        when(mockCookie.getValue()).thenReturn(
                "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE3MTI1NjAyMTIsInBlcm1pc3Npb25zIjp7InRlc3QtcmMiOlsiZm9vIiwiYmFyIl19fQ.pb9lkYbzK2JTY9HkRlgb8LBZg35baS_F54kAOE4DD_Y");
        when(mockRequest.getHttpServletMapping()).thenReturn(mockServletMapping);
        when(mockServletMapping.getServletName()).thenReturn("es-datadis");
        when(mockContext.getVariables()).thenReturn(Collections.emptyMap());

        // When, Then
        assertThrows(AccessDeniedException.class, () -> authManager.check(null, mockContext));
    }

    @Test
    void givenNoPermittedPermissions_returnsDenied() {
        // Given
        when(mockRequest.getCookies()).thenReturn(new Cookie[]{mockCookie});
        when(mockCookie.getName()).thenReturn(JwtUtil.JWT_COOKIE_NAME);
        when(mockCookie.getValue()).thenReturn(
                "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE3MTI1NjAyMTIsInBlcm1pc3Npb25zIjp7InRlc3QtcmMiOlsiZm9vIiwiYmFyIl19fQ.pb9lkYbzK2JTY9HkRlgb8LBZg35baS_F54kAOE4DD_Y");
        when(mockRequest.getHttpServletMapping()).thenReturn(mockServletMapping);
        when(mockServletMapping.getServletName()).thenReturn("es-datadis");
        when(mockContext.getVariables()).thenReturn(Collections.emptyMap());
        when(mockContext.getVariables()).thenReturn(Map.of("permissionId", "NotPermitted"));
        when(mockJwtUtil.getPermissions(anyString())).thenReturn(Map.of("es-datadis", Collections.emptyList()));

        // When, Then
        assertThrows(AccessDeniedException.class, () -> authManager.check(null, mockContext));
    }

    @Test
    void givenPermissionNotPermitted_returnsDenied() {
        // Given
        when(mockRequest.getCookies()).thenReturn(new Cookie[]{mockCookie});
        when(mockCookie.getName()).thenReturn(JwtUtil.JWT_COOKIE_NAME);
        when(mockCookie.getValue()).thenReturn(
                "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE3MTI1NjAyMTIsInBlcm1pc3Npb25zIjp7InRlc3QtcmMiOlsiZm9vIiwiYmFyIl19fQ.pb9lkYbzK2JTY9HkRlgb8LBZg35baS_F54kAOE4DD_Y");
        when(mockRequest.getHttpServletMapping()).thenReturn(mockServletMapping);
        when(mockServletMapping.getServletName()).thenReturn("es-datadis");
        when(mockContext.getVariables()).thenReturn(Map.of("permissionId", "NotPermitted"));
        when(mockJwtUtil.getPermissions(anyString())).thenReturn(Map.of("es-datadis", List.of("foo", "bar")));

        // When, Then
        assertThrows(AccessDeniedException.class, () -> authManager.check(null, mockContext));
    }

    @Test
    void givenValidJwt_returnsGranted() {
        // Given
        when(mockRequest.getCookies()).thenReturn(new Cookie[]{mockCookie});
        when(mockCookie.getName()).thenReturn(JwtUtil.JWT_COOKIE_NAME);
        when(mockCookie.getValue()).thenReturn(
                "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE3MTI1NjAyMTIsInBlcm1pc3Npb25zIjp7InRlc3QtcmMiOlsiZm9vIiwiYmFyIl19fQ.pb9lkYbzK2JTY9HkRlgb8LBZg35baS_F54kAOE4DD_Y");
        when(mockRequest.getHttpServletMapping()).thenReturn(mockServletMapping);
        when(mockServletMapping.getServletName()).thenReturn("es-datadis");
        when(mockContext.getVariables()).thenReturn(Map.of("permissionId", "foo"));
        when(mockJwtUtil.getPermissions(anyString())).thenReturn(Map.of("es-datadis", List.of("foo", "bar")));

        // When, Then
        assertTrue(authManager.check(null, mockContext).isGranted());
    }
}

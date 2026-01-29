package energy.eddie.regionconnector.shared.security;

import jakarta.servlet.http.HttpServletMapping;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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
    @Mock
    private HttpServletRequest mockRequest;
    @Mock
    private HttpServletMapping mockServletMapping;
    private JwtAuthorizationManager headerAuthManager;

    @BeforeEach
    void setUp() {
        headerAuthManager = new JwtAuthorizationManager(mockJwtUtil);
        when(mockContext.getRequest()).thenReturn(mockRequest);
    }

    public static Stream<Arguments> invalidAuthorizationHeaderValues() {
        return Stream.of(
                Arguments.of("Not Bearer Prefix SomeJwt"),
                Arguments.of("Bearer "),
                Arguments.of("Bearer    "),
                Arguments.of((String) null)
        );
    }

    @ParameterizedTest
    @MethodSource("invalidAuthorizationHeaderValues")
    void givenInvalidOrEmptyJwt_headerAuthorizationManager_returnsDenied(String headerValue) {
        // Given
        when(mockRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(headerValue);

        // When, Then
        assertThrows(AccessDeniedException.class, () -> headerAuthManager.authorize(null, mockContext));
    }

    @Test
    void givenValidJwt_headerAuthorizationManager_returnsGranted() {
        // Given
        when(mockRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(
                "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE3MTI1NjAyMTIsInBlcm1pc3Npb25zIjp7ImFpaWRhIjpbIm15VGVzdElkIl19fQ.fL56r0SCqjtB3nroP4fsVHL3_GfmAXk2sNLfMLRtXCg");
        when(mockRequest.getHttpServletMapping()).thenReturn(mockServletMapping);
        when(mockServletMapping.getServletName()).thenReturn("aiida");
        when(mockContext.getVariables()).thenReturn(Map.of("permissionId", "myTestId"));
        when(mockJwtUtil.getPermissions(anyString())).thenReturn(Map.of("aiida",
                                                                        List.of("myTestId"),
                                                                        "es-datadis",
                                                                        List.of("foo", "bar")));

        // When, Then
        assertTrue(headerAuthManager.authorize(null, mockContext).isGranted());
    }
}

// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.security;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletMapping;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.BOOLEAN;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthorizationManagerTest {
    @Mock
    private JwtUtil mockJwtUtil;
    @InjectMocks
    private JwtAuthorizationManager headerAuthManager;

    @ParameterizedTest
    @MethodSource("invalidAuthorizationHeaderValues")
    void givenInvalidJwt_headerAuthorizationManager_returnsDenied(String headerValue) {
        // Given
        var mockRequest = new MockHttpServletRequest();
        mockRequest.addHeader(HttpHeaders.AUTHORIZATION, headerValue);
        var mockContext = new RequestAuthorizationContext(mockRequest);

        // When, Then
        assertThrows(AccessDeniedException.class, () -> headerAuthManager.authorize(null, mockContext));
    }

    @Test
    void givenEmptyJwt_headerAuthorizationManager_returnsDenied() {
        // Given
        var mockRequest = new MockHttpServletRequest();
        var mockContext = new RequestAuthorizationContext(mockRequest);

        // When, Then
        assertThrows(AccessDeniedException.class, () -> headerAuthManager.authorize(null, mockContext));
    }

    @Test
    void givenNoAuthorizationContext_headerAuthorizationManager_returnsDenied() {
        // Given, When
        var res = headerAuthManager.authorize(null, null);
        // Then
        assertThat(res).isNotNull()
                       .extracting(AuthorizationResult::isGranted, BOOLEAN)
                       .isFalse();
    }

    @Test
    void givenValidJwtWithMismatchedPermissionId_headerAuthorizationManager_throwsAccessDeniedException() {
        // Given
        var mockRequest = createMockRequest("aiida");
        var mockContext = new RequestAuthorizationContext(mockRequest, Map.of("permissionId", "invalid-pm-id"));
        when(mockJwtUtil.getPermissions(anyString()))
                .thenReturn(Map.of("aiida", List.of("myTestId"), "es-datadis", List.of("foo", "bar")));

        // When, Then
        assertThrows(AccessDeniedException.class, () -> headerAuthManager.authorize(null, mockContext));
    }

    @Test
    void givenValidJwtWithoutRequestedPermissionIds_headerAuthorizationManager_throwsAccessDeniedException() {
        // Given
        var mockRequest = createMockRequest("aiida");
        var mockContext = new RequestAuthorizationContext(mockRequest, Map.of());
        when(mockJwtUtil.getPermissions(anyString()))
                .thenReturn(Map.of("aiida", List.of("myTestId"), "es-datadis", List.of("foo", "bar")));

        // When, Then
        assertThrows(AccessDeniedException.class, () -> headerAuthManager.authorize(null, mockContext));
    }

    @Test
    void givenValidJwtWithoutServletName_headerAuthorizationManager_throwsAccessDeniedException() {
        // Given
        var mockRequest = createMockRequest(null);
        var mockContext = new RequestAuthorizationContext(mockRequest, Map.of("permissionId", "foo"));
        Map<String, List<String>> rcPermissions = new HashMap<>();
        rcPermissions.put("aiida", List.of("myTestId"));
        rcPermissions.put("es-datadis", List.of("foo", "bar"));
        when(mockJwtUtil.getPermissions(anyString()))
                .thenReturn(rcPermissions);

        // When, Then
        assertThrows(AccessDeniedException.class, () -> headerAuthManager.authorize(null, mockContext));
    }

    @Test
    void givenValidJwtPermissionsInJwt_headerAuthorizationManager_throwsAccessDeniedException() {
        // Given
        var mockRequest = createMockRequest("aiida");
        var mockContext = new RequestAuthorizationContext(mockRequest, Map.of("permissionId", "foo"));
        when(mockJwtUtil.getPermissions(anyString()))
                .thenReturn(Map.of());

        // When, Then
        assertThrows(AccessDeniedException.class, () -> headerAuthManager.authorize(null, mockContext));
    }

    @Test
    void givenValidJwtAndPermissionIdInPath_headerAuthorizationManager_returnsGranted() {
        // Given
        var mockRequest = createMockRequest("aiida");
        var mockContext = new RequestAuthorizationContext(mockRequest, Map.of("permissionId", "myTestId"));
        when(mockJwtUtil.getPermissions(anyString())).thenReturn(Map.of("aiida",
                                                                        List.of("myTestId"),
                                                                        "es-datadis",
                                                                        List.of("foo", "bar")));

        // When
        var res = headerAuthManager.authorize(null, mockContext);

        // Then
        assertThat(res)
                .isNotNull()
                .extracting(AuthorizationResult::isGranted, InstanceOfAssertFactories.BOOLEAN)
                .isTrue();
    }

    @Test
    void givenValidJwtAndPermissionIdsInQueryParameters_headerAuthorizationManager_returnsGranted() {
        // Given
        var mockRequest = createMockRequest("aiida");
        mockRequest.addParameter("permission-id", "myTestId1", "myTestId2");
        var mockContext = new RequestAuthorizationContext(mockRequest, Map.of());
        when(mockJwtUtil.getPermissions(anyString()))
                .thenReturn(Map.of("aiida", List.of("myTestId1", "myTestId2"),
                                   "es-datadis", List.of("foo", "bar")));

        // When
        var res = headerAuthManager.authorize(null, mockContext);

        // Then
        assertThat(res)
                .isNotNull()
                .extracting(AuthorizationResult::isGranted, InstanceOfAssertFactories.BOOLEAN)
                .isTrue();
    }

    @Test
    void givenValidJwtAndCoreDispatcherServlet_headerAuthorizationManager_returnsGranted() {
        // Given
        var mockRequest = createMockRequest("dispatcherServlet");
        var mockContext = new RequestAuthorizationContext(mockRequest, Map.of("permissionId", "foo"));
        when(mockJwtUtil.getPermissions(anyString()))
                .thenReturn(Map.of("aiida", List.of("myTestId"), "es-datadis", List.of("foo", "bar")));

        // When
        var res = headerAuthManager.authorize(null, mockContext);

        // Then
        assertThat(res)
                .isNotNull()
                .extracting(AuthorizationResult::isGranted, InstanceOfAssertFactories.BOOLEAN)
                .isTrue();
    }

    private static Stream<Arguments> invalidAuthorizationHeaderValues() {
        return Stream.of(
                Arguments.of("Not Bearer Prefix SomeJwt"),
                Arguments.of("Bearer "),
                Arguments.of("Bearer    ")
        );
    }

    @SuppressWarnings("DataFlowIssue") // Allow nullable for the servletName
    private static MockHttpServletRequest createMockRequest(@Nullable String servletName) {
        var mockRequest = new MockHttpServletRequest();
        var servletMapping = new MockHttpServletMapping("", "", servletName, null);
        mockRequest.setHttpServletMapping(servletMapping);
        mockRequest.addHeader(HttpHeaders.AUTHORIZATION,
                              "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE3MTI1NjAyMTIsInBlcm1pc3Npb25zIjp7ImFpaWRhIjpbIm15VGVzdElkIl19fQ.fL56r0SCqjtB3nroP4fsVHL3_GfmAXk2sNLfMLRtXCg");
        return mockRequest;
    }
}

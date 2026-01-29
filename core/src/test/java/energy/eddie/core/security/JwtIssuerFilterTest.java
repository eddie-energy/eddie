package energy.eddie.core.security;

import energy.eddie.regionconnector.shared.security.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.ContentCachingResponseWrapper;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class JwtIssuerFilterTest {
    private final JwtUtil jwtUtil = new JwtUtil("YzQxOGJjNTM3NTI4MWRlMTYzNmFmOTQ2ZjkwZDE0ZTA=", 300);
    private final JwtIssuerFilter filter = new JwtIssuerFilter(
            new ObjectMapper(),
            jwtUtil
    );

    @Test
    void shouldNotFilter_forNonPermissionRequestPath_returnsTrue() {
        // Given
        var request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/region-connectors/cds/ce.js");

        // When
        var res = filter.shouldNotFilter(request);

        // Then
        assertTrue(res);
    }

    @Test
    void shouldNotFilter_forNonPostRequest_returnsTrue() {
        // Given
        var request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/region-connectors/cds/" + PATH_PERMISSION_REQUEST);

        // When
        var res = filter.shouldNotFilter(request);

        // Then
        assertTrue(res);
    }

    @Test
    void shouldNotFilter_forCorrectRequest_returnsFalse() {
        // Given
        var request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/region-connectors/cds/" + PATH_PERMISSION_REQUEST);

        // When
        var res = filter.shouldNotFilter(request);

        // Then
        assertFalse(res);
    }

    @Test
    void doFilterInternal_forInvalidReturnStatus_keepsOriginalPayload() throws IOException, ServletException {
        // Given
        var request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/region-connectors/cds/" + PATH_PERMISSION_REQUEST);
        var response = new MockHttpServletResponse();
        response.setStatus(400);
        var payload = "{\"permissionId\": \"pid\"}";
        response.getWriter().write(payload);
        var chain = new MockFilterChain();

        // When
        filter.doFilterInternal(request, response, chain);

        // Then
        assertAll(
                () -> assertInstanceOf(ContentCachingResponseWrapper.class, chain.getResponse()),
                () -> assertEquals(payload, response.getContentAsString())
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "region-connectors"})
    void doFilterInternal_forInvalidPath_keepsOriginalPayload(String path) throws IOException, ServletException {
        // Given
        var request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI(path);
        var response = new MockHttpServletResponse();
        response.setStatus(201);
        var payload = "{\"permissionId\": \"pid\"}";
        response.getWriter().write(payload);
        var chain = new MockFilterChain();

        // When
        filter.doFilterInternal(request, response, chain);

        // Then
        assertAll(
                () -> assertInstanceOf(ContentCachingResponseWrapper.class, chain.getResponse()),
                () -> assertEquals(payload, response.getContentAsString())
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "<permissionId>pid</permissionId>", "{\"connectionId\": \"asdf\"}", "{\"permissionId\": []}", "[]"})
    void doFilterInternal_forInvalidContent_keepsOriginalPayload(String payload) throws IOException, ServletException {
        // Given
        var request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/region-connectors/cds/" + PATH_PERMISSION_REQUEST);
        var response = new MockHttpServletResponse();
        response.setStatus(201);
        var chain = new MockFilterChain(new MockHttpServlet(payload), filter);

        // When
        chain.doFilter(request, response);

        // Then
        assertEquals(payload, response.getContentAsString());
    }

    @Test
    void doFilterInternal_forValidContent_addsBearerToken() throws IOException, ServletException {
        // Given
        var request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/region-connectors/cds/" + PATH_PERMISSION_REQUEST);
        var response = new MockHttpServletResponse();
        response.setStatus(201);
        var payload = "{\"permissionId\": \"pid\"}";
        var chain = new MockFilterChain(new MockHttpServlet(payload), filter);

        // When
        chain.doFilter(request, response);

        // Then
        assertThat(response.getContentAsString())
                .contains("\"bearerToken\"");
    }

    private static class MockHttpServlet extends HttpServlet {
        private final String payload;

        private MockHttpServlet(String payload) {this.payload = payload;}

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
            res.setStatus(201);
            res.getWriter().write(payload);
            res.setContentLength(payload.getBytes(StandardCharsets.UTF_8).length);
        }
    }
}
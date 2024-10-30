package energy.eddie.core;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ManagementApiConfigInternalEndpointsFilterTest {
    @ParameterizedTest
    @ValueSource(strings = {"/management", "/outbound-connectors", "/data-needs/management"})
    void testDoFilter_forManagementPortOnManagementUrl_continuesFilterChain(String reqUri) throws ServletException, IOException {
        // Given
        var req = new MockHttpServletRequest();
        req.setRequestURI(reqUri);
        req.setServerPort(9090);
        var resp = new MockHttpServletResponse();
        var chain = new MockFilterChain();
        var config = new ManagementApiConfig(9090, "management");
        var filter = config.trustedEndpointsFilter().getFilter();

        // When
        filter.doFilter(req, resp, chain);

        // Then
        assertAll(
                () -> assertEquals(req, chain.getRequest()),
                () -> assertEquals(resp, chain.getResponse())
        );
    }

    @Test
    void testDoFilter_forAllowedApisOnNormalPort_continuesFilterChain() throws ServletException, IOException {
        // Given
        var req = new MockHttpServletRequest();
        req.setRequestURI("/region-connectors/us-green-button/callback");
        req.setServerPort(8080);
        var resp = new MockHttpServletResponse();
        var chain = new MockFilterChain();
        var config = new ManagementApiConfig(9090, "management");
        var filter = config.trustedEndpointsFilter().getFilter();

        // When
        filter.doFilter(req, resp, chain);

        // Then
        assertAll(
                () -> assertEquals(req, chain.getRequest()),
                () -> assertEquals(resp, chain.getResponse())
        );
    }

    @Test
    void testDoFilter_forManagementApiOnNormalPort_errors() throws ServletException, IOException {
        // Given
        var req = new MockHttpServletRequest();
        req.setRequestURI("/management");
        req.setServerPort(8080);
        var resp = new MockHttpServletResponse();
        var chain = new MockFilterChain();
        var config = new ManagementApiConfig(9090, "management");
        var filter = config.trustedEndpointsFilter().getFilter();

        // When
        filter.doFilter(req, resp, chain);

        // Then
        assertEquals(HttpServletResponse.SC_NOT_FOUND, resp.getStatus());
    }

    @Test
    void testDoFilter_forNormalApiOnManagementPort_errors() throws ServletException, IOException {
        // Given
        var req = new MockHttpServletRequest();
        req.setRequestURI("/region-connectors/us-green-button/callback");
        req.setServerPort(9090);
        var resp = new MockHttpServletResponse();
        var chain = new MockFilterChain();
        var config = new ManagementApiConfig(9090, "management");
        var filter = config.trustedEndpointsFilter().getFilter();

        // When
        filter.doFilter(req, resp, chain);

        // Then
        assertEquals(HttpServletResponse.SC_NOT_FOUND, resp.getStatus());
    }
}
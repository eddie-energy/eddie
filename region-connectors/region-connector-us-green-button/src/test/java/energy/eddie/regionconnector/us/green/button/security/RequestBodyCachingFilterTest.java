// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.security;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

class RequestBodyCachingFilterTest {

    @Test
    void testDoFilter_wrapsRequestInCachedRequest() throws ServletException, IOException {
        // Given
        var mockRequest = new MockHttpServletRequest();
        mockRequest.setContent("CONTENT".getBytes(StandardCharsets.UTF_8));
        var mockResponse = new MockHttpServletResponse();
        var mockChain = new MockFilterChain();
        var filter = new RequestBodyCachingFilter();

        // When
        filter.doFilter(mockRequest, mockResponse, mockChain);

        // Then
        var res = mockChain.getRequest();
        assertThat(res, instanceOf(ContentCachingRequestWrapper.class));
    }
}
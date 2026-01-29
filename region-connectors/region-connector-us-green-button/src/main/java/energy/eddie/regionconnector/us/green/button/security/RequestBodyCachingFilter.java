package energy.eddie.regionconnector.us.green.button.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

/**
 * Wraps the request in a cached request, since it is not able to get the request body twice.
 */
public class RequestBodyCachingFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        var wrappedRequest = new ContentCachingRequestWrapper(httpRequest, 0);
        chain.doFilter(wrappedRequest, response);
    }
}
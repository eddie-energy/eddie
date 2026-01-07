package energy.eddie.core;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import org.apache.catalina.connector.Connector;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.servlet.ServletWebServerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;


/**
 * This Spring Configuration modifies the embedded webserver by opening a second port for the management api. Because
 * adding a second independent server and activating all spring boot features is error-prone (possibly using additional
 * container scopes), the default server is used and an additional port is opened. To separate management api requests
 * from the other ones, a HttpServletFilter is used and the management api uses a specific url prefix. The spring data
 * repositories are also moved to the management api path.
 * <p>
 * The following configuration properties are used:
 * <ul>
 *      <li>eddie.management.server.port=9090</li>
 *      <li>eddie.management.server.urlprefix=management</li>
 *      <li>spring.data.rest.basepath=${eddie.management.server.urlprefix}</li>
 * </ul>
 *
 * @see <a
 * href="https://dmytro-lazarenko.hashnode.dev/spring-boot-2-multiple-ports-for-internal-and-external-rest-apis-jetty">Spring
 * boot 2: multiple ports for Internal and External REST APIs + Jetty</a>
 */
@Configuration
public class ManagementApiConfig {

    private final int managementPort;
    private final String managementUrlPrefix;

    public ManagementApiConfig(
            @Value("${eddie.management.server.port}") int managementPort,
            @Value("${eddie.management.server.urlprefix}") String managementUrlPrefix
    ) {
        this.managementPort = managementPort;
        this.managementUrlPrefix = managementUrlPrefix.startsWith("/") ? managementUrlPrefix : "/" + managementUrlPrefix;
    }

    @Bean
    public ServletWebServerFactory servletContainer() {
        Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        connector.setScheme("http");
        connector.setPort(managementPort);

        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
        tomcat.addAdditionalConnectors(connector);
        return tomcat;
    }

    @Bean
    public FilterRegistrationBean<@NotNull InternalEndpointsFilter> trustedEndpointsFilter() {
        return new FilterRegistrationBean<>(new InternalEndpointsFilter());
    }

    public class InternalEndpointsFilter extends GenericFilterBean {
        // use different logger name to avoid sonar issues regarding the baseclass "logger"
        private static final Logger IEF_LOGGER = org.slf4j.LoggerFactory.getLogger(InternalEndpointsFilter.class);

        @Override
        public void doFilter(
                ServletRequest request,
                ServletResponse response,
                FilterChain chain
        ) throws IOException, ServletException {
            var httpRequest = (HttpServletRequest) request;
            var isRequestOnManagementPort = httpRequest.getServerPort() == managementPort;
            var requestURI = httpRequest.getRequestURI();
            var isRequestOnManagementUrl = requestURI.startsWith(CoreSpringConfig.DATA_NEEDS_URL_MAPPING_PREFIX + managementUrlPrefix)
                                           || requestURI.startsWith("/outbound-connectors")
                                           || requestURI.startsWith(managementUrlPrefix)
                                           || requestURI.matches("/region-connectors/[\\w\\-]+%s/.*".formatted(managementUrlPrefix));
            IEF_LOGGER.debug("{} requested on port {}, managementPort: {}, managementUrl: {}",
                             requestURI,
                             request.getLocalPort(),
                             isRequestOnManagementPort,
                             isRequestOnManagementUrl);
            if (isRequestOnManagementPort && isRequestOnManagementUrl) {
                chain.doFilter(request, response); // this request should be considered accepted
            } else if (!isRequestOnManagementPort && !isRequestOnManagementUrl) {
                chain.doFilter(request, response); // this request should be processed by the ordinary security filters
            } else {
                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        }
    }
}

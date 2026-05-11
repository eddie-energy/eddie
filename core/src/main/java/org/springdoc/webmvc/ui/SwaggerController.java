// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package org.springdoc.webmvc.ui;

import io.swagger.v3.oas.models.servers.Server;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigParameters;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.providers.SpringWebProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;


/**
 * Overwrites the SwaggerController path to consider the url mapping of a DispatcherServlet, instead of always using the
 * same URL.
 *
 * @see <a href="https://github.com/springdoc/springdoc-openapi/issues/2164">https://github.com/springdoc/springdoc-openapi/issues/2164</a>
 */
@Configuration
public class SwaggerController extends SwaggerWelcomeWebMvc {
    private static final Logger LOGGER = LoggerFactory.getLogger(SwaggerController.class);
    private final int managementPort;
    private final String publicUrl;
    private final String managementUrl;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public SwaggerController(
            SwaggerUiConfigProperties swaggerUiConfig,
            SpringDocConfigProperties springDocConfigProperties,
            ObjectProvider<SpringWebProvider> springWebProvider,
            @Value("${eddie.management.server.port}") int managementPort,
            @Value("${eddie.public.url}") String publicUrl,
            @Value("${eddie.management.url}") String managementUrl
    ) {
        super(swaggerUiConfig, springDocConfigProperties, springWebProvider);
        this.managementPort = managementPort;
        this.publicUrl = publicUrl;
        this.managementUrl = managementUrl;
    }

    @Bean
    public OpenApiCustomizer dynamicServerUrlCustomizer(
    ) {
        return openApi -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes)
                            RequestContextHolder.getRequestAttributes();

            if (attributes == null) {
                return;
            }

            HttpServletRequest request = attributes.getRequest();
            LOGGER.trace(
                    "Got request with servlet path {}, context path {}, path info {}, translated path {}, server port {}, local port {}",
                    request.getServletPath(),
                    request.getContextPath(),
                    request.getPathInfo(),
                    request.getPathTranslated(),
                    request.getServerPort(),
                    request.getLocalPort());

            String baseUri = findBasePath(request.getLocalPort());

            String serverUrl = UriComponentsBuilder.fromUriString(baseUri)
                                                   .path(request.getServletPath())
                                                   .build()
                                                   .toUriString();
            LOGGER.trace("Resolved server URL: {}", serverUrl);

            Server server = new Server();
            server.setUrl(serverUrl);

            openApi.setServers(List.of(server));
        };
    }

    @Override
    protected void buildFromCurrentContextPath(
            SwaggerUiConfigParameters swaggerUiConfigParameters,
            HttpServletRequest request
    ) {
        super.init(swaggerUiConfigParameters);

        // Determine the base URL based on the incoming request port
        var swaggerBaseUrl = findBasePath(request.getServerPort());

        LOGGER.trace(
                "Configuring Swagger UI based on incoming port. Resolved Swagger base URL: '{}', " +
                "Request port: {}, Context path: '{}'",
                swaggerBaseUrl, request.getServerPort(), request.getServletPath()
        );

        // Dynamically build the server configuration
        swaggerUiConfigParameters.setUrl(swaggerBaseUrl);
        swaggerUiConfigParameters.setContextPath(UriComponentsBuilder.fromUriString(swaggerBaseUrl)
                                                                     .path(request.getServletPath())
                                                                     .build()
                                                                     .getPath());
        buildConfigUrl(swaggerUiConfigParameters, UriComponentsBuilder.fromUriString(swaggerBaseUrl));
    }

    private String findBasePath(int serverPort) {
        return serverPort == managementPort
                ? managementUrl
                : publicUrl;
    }
}

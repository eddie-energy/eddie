package org.springdoc.webmvc.ui;

import jakarta.servlet.http.HttpServletRequest;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigParameters;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.providers.SpringWebProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;


/**
 * Overwrites the SwaggerController path to consider the url mapping of a DispatcherServlet, instead of always using the
 * same URL.
 */
@Configuration
public class SwaggerController extends SwaggerWelcomeWebMvc {
    public SwaggerController(
            SwaggerUiConfigProperties swaggerUiConfig,
            SpringDocConfigProperties springDocConfigProperties,
            SwaggerUiConfigParameters swaggerUiConfigParameters,
            SpringWebProvider springWebProvider
    ) {
        super(swaggerUiConfig, springDocConfigProperties, swaggerUiConfigParameters, springWebProvider);
    }

    @Override
    void buildFromCurrentContextPath(HttpServletRequest request) {
        super.init();
        // default is request.getContextPath()
        contextPath = request.getServletPath();
        buildConfigUrl(ServletUriComponentsBuilder.fromCurrentContextPath());
    }
}

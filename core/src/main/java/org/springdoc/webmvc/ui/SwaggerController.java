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
 * @see <a href="https://github.com/springdoc/springdoc-openapi/issues/2164">https://github.com/springdoc/springdoc-openapi/issues/2164</a>
 */
@Configuration
public class SwaggerController extends SwaggerWelcomeWebMvc {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public SwaggerController(
            SwaggerUiConfigProperties swaggerUiConfig,
            SpringDocConfigProperties springDocConfigProperties,
            SpringWebProvider springWebProvider
    ) {
        super(swaggerUiConfig, springDocConfigProperties, springWebProvider);
    }

    @Override
    void buildFromCurrentContextPath(SwaggerUiConfigParameters swaggerUiConfigParameters, HttpServletRequest request) {
        super.init(swaggerUiConfigParameters);
        // default is request.getContextPath()
        swaggerUiConfigParameters.setContextPath(request.getServletPath());
        buildConfigUrl(swaggerUiConfigParameters, ServletUriComponentsBuilder.fromCurrentContextPath());
    }
}

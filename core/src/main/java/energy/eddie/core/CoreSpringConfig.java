package energy.eddie.core;

import eddie.energy.europeanmasterdata.EuropeanMasterDataSpringConfig;
import energy.eddie.OpenApiDocs;
import energy.eddie.admin.console.AdminConsoleSpringConfig;
import energy.eddie.api.utils.Shared;
import energy.eddie.dataneeds.DataNeedsSpringConfig;
import energy.eddie.spring.RegionConnectorRegistrationBeanPostProcessor;
import energy.eddie.spring.SharedBeansRegistrar;
import energy.eddie.spring.regionconnector.extensions.RegionConnectorsCommonControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigParameters;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.webmvc.core.configuration.MultipleOpenApiSupportConfiguration;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springdoc.webmvc.ui.SwaggerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static energy.eddie.spring.RegionConnectorRegistrationBeanPostProcessor.enableSpringDoc;

@SpringBootApplication(
        // do not automatically register these beans, as otherwise they are duplicate in the child context
        // and each child context needs their own instance of them to provide openAPI doc
        exclude = {
                SpringDocWebMvcConfiguration.class,
                MultipleOpenApiSupportConfiguration.class,
                SwaggerConfig.class,
                SwaggerUiConfigProperties.class,
                SwaggerUiConfigParameters.class,
                SwaggerUiOAuthProperties.class,
                SpringDocConfiguration.class,
                SpringDocConfigProperties.class,
        }
)
public class CoreSpringConfig implements WebMvcConfigurer {
    public static final String DATA_NEEDS_URL_MAPPING_PREFIX = "/data-needs";
    private static final Logger LOGGER = LoggerFactory.getLogger(CoreSpringConfig.class);

    private final String allowedCorsOrigins;

    protected CoreSpringConfig(@Value("${eddie.cors.allowed-origins:}") String allowedCorsOrigins) {
        this.allowedCorsOrigins = allowedCorsOrigins;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // add a resource handler that serves all public files of this region connector
        registry.addResourceHandler("/lib/**")
                .addResourceLocations("classpath:/public/lib/");
    }

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        if (allowedCorsOrigins.isEmpty()) {
            LOGGER.info("No CORS origins configured, will not set any CORS headers.");
            return;
        }

        registry.addMapping("/**")
                .allowedOriginPatterns(allowedCorsOrigins)
                // Location header is not a "simple header", therefore needs to be explicitly exposed, otherwise JS on frontend cannot access it
                .exposedHeaders("Location")
                .allowedMethods("GET", "POST", "PATCH", "PUT", "DELETE");
    }

    /**
     * Beans returning a {@link org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor} need to
     * be static for Spring to be able to "enhance @Configuration bean definition".
     */
    @Bean
    static RegionConnectorRegistrationBeanPostProcessor regionConnectorRegistrationBeanPostProcessor(Environment environment) {
        return new RegionConnectorRegistrationBeanPostProcessor(environment);
    }

    /**
     * Data needs related services have their own context, to be able to properly create and serve JavaDoc. Beans that
     * may be required by other region connectors should be annotated with {@link Shared}, so they will be automatically
     * registered in this core context by {@link SharedBeansRegistrar}.
     */
    @Bean
    public ServletRegistrationBean<DispatcherServlet> dataNeedsDispatcherServlet() {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.register(DataNeedsSpringConfig.class);
        context.register(SharedBeansRegistrar.class);
        context.register(RegionConnectorsCommonControllerAdvice.class);
        enableSpringDoc(context);
        context.register(OpenApiDocs.class);

        DispatcherServlet dispatcherServlet = new DispatcherServlet(context);
        String urlMapping = DATA_NEEDS_URL_MAPPING_PREFIX + "/*";
        ServletRegistrationBean<DispatcherServlet> connectorServletBean = new ServletRegistrationBean<>(
                dispatcherServlet,
                urlMapping
        );

        connectorServletBean.setName("data-needs");
        connectorServletBean.setLoadOnStartup(2);

        LOGGER.info("Created ServletRegistrationBean for data needs, urlMapping is {}", urlMapping);
        return connectorServletBean;
    }

    @Bean
    public ServletRegistrationBean<DispatcherServlet> europeanMasterDataDispatcherServlet() {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.register(EuropeanMasterDataSpringConfig.class);
        enableSpringDoc(context);

        DispatcherServlet dispatcherServlet = new DispatcherServlet(context);
        String urlMapping = "/european-masterdata/*";
        ServletRegistrationBean<DispatcherServlet> connectorServletBean = new ServletRegistrationBean<>(
                dispatcherServlet,
                urlMapping
        );

        connectorServletBean.setName("european-masterdata");
        connectorServletBean.setLoadOnStartup(2);

        LOGGER.info("Created ServletRegistrationBean for european-masterdata, urlMapping is {}", urlMapping);
        return connectorServletBean;
    }

    @Bean
    public ServletRegistrationBean<DispatcherServlet> adminConsoleDispatcherServlet() {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.register(AdminConsoleSpringConfig.class);
        enableSpringDoc(context);

        DispatcherServlet dispatcherServlet = new DispatcherServlet(context);
        String urlMapping = "/admin-console/*";
        ServletRegistrationBean<DispatcherServlet> connectorServletBean = new ServletRegistrationBean<>(
                dispatcherServlet,
                urlMapping
        );

        connectorServletBean.setName("admin-console");
        connectorServletBean.setLoadOnStartup(2);

        LOGGER.info("Created ServletRegistrationBean for admin-console, urlMapping is {}", urlMapping);
        return connectorServletBean;
    }
}

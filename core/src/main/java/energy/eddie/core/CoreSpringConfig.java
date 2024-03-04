package energy.eddie.core;

import energy.eddie.api.agnostic.DataNeed;
import energy.eddie.api.agnostic.DataNeedsService;
import energy.eddie.spring.RegionConnectorRegistrationBeanPostProcessor;
import energy.eddie.spring.regionconnector.extensions.RegionConnectorsCommonControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Optional;
import java.util.Set;

@SpringBootApplication
public class CoreSpringConfig implements WebMvcConfigurer {
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
     * Beans returning a {@link org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor}
     * need to be static for Spring to be able to "enhance @Configuration bean definition".
     */
    @Bean
    static RegionConnectorRegistrationBeanPostProcessor regionConnectorRegistrationBeanPostProcessor() {
        return new RegionConnectorRegistrationBeanPostProcessor();
    }

    /**
     * Explicitly register the common controller advice as it's only automatically registered for region connectors.
     */
    @Bean
    public RegionConnectorsCommonControllerAdvice regionConnectorsCommonControllerAdvice() {
        return new RegionConnectorsCommonControllerAdvice();
    }

    @Bean
    public DataNeedsService temporaryService() {
        return new DataNeedsService() {
            @Override
            public Optional<DataNeed> getDataNeed(String id) {
                return Optional.empty();
            }

            @Override
            public Set<String> getAllDataNeedIds() {
                return Set.of();
            }
        };
    }
}

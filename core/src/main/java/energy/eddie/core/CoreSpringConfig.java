package energy.eddie.core;

import energy.eddie.core.dataneeds.DataNeedsConfig;
import energy.eddie.spring.RegionConnectorRegistrationBeanPostProcessor;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@EnableConfigurationProperties(DataNeedsConfig.class)
public class CoreSpringConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // add a resource handler that serves all public files of this region connector
        registry.addResourceHandler("/lib/**")
                .addResourceLocations("classpath:/public/lib/");
    }

    /**
     * Beans returning a {@link org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor}
     * need to be static for Spring to be able to "enhance @Configuration bean definition".
     */
    @Bean
    static RegionConnectorRegistrationBeanPostProcessor regionConnectorRegistrationBeanPostProcessor() {
        return new RegionConnectorRegistrationBeanPostProcessor();
    }
}

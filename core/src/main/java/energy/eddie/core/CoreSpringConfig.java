package energy.eddie.core;

import energy.eddie.core.dataneeds.DataNeedsConfig;
import energy.eddie.core.spring.RegionConnectorRegistrationBeanPostProcessor;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(DataNeedsConfig.class)
public class CoreSpringConfig {
    protected CoreSpringConfig() {
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

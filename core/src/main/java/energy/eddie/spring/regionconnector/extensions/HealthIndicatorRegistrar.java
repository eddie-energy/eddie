package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.spring.SharedBeansRegistrar;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthIndicator;

import static java.util.Objects.requireNonNull;

/**
 * Registry that adds all health indicators from child contexts to the parent health indicator contributor registry.
 *
 * @see SharedBeansRegistrar
 */
@SuppressWarnings("unused") // Scanned by RegionConnectorRegistrationBeanPostProcessor
@RegionConnectorExtension
public class HealthIndicatorRegistrar implements BeanPostProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(HealthIndicatorRegistrar.class);
    private final ConfigurableListableBeanFactory localBeanFactory;

    public HealthIndicatorRegistrar(ConfigurableListableBeanFactory localBeanFactory) {
        this.localBeanFactory = localBeanFactory;
    }

    @Override
    public Object postProcessAfterInitialization(
            @Nullable Object bean,
            @Nullable String beanName
    ) throws BeansException {
        var parentBeanFactory = getParentBeanFactory();
        requireNonNull(parentBeanFactory, "This post processor can only be called in a child context");
        if (bean instanceof HealthIndicator healthIndicator) {
            var registry = parentBeanFactory.getBean(HealthContributorRegistry.class);
            if (containsHealthIndicator(beanName, registry)) return bean;
            LOGGER.info("Registering health indicator {}", beanName);
            registry.registerContributor(beanName, healthIndicator);
        }
        return bean;
    }

    @Nullable
    private ConfigurableListableBeanFactory getParentBeanFactory() {
        BeanFactory parent = localBeanFactory.getParentBeanFactory();
        return (parent instanceof ConfigurableListableBeanFactory factory) ? factory : null;
    }

    private static boolean containsHealthIndicator(
            @Nullable String beanName,
            HealthContributorRegistry registry
    ) {
        if (beanName == null) return false;
        for (var healthContributorNamedContributor : registry) {
            if (beanName.startsWith(healthContributorNamedContributor.getName())) {
                return true;
            }
        }
        return false;
    }
}

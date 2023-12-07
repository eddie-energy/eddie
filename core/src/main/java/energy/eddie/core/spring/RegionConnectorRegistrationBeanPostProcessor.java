package energy.eddie.core.spring;

import energy.eddie.api.agnostic.SpringRegionConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.Ordered;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.lang.NonNull;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import java.util.Set;

public class RegionConnectorRegistrationBeanPostProcessor implements BeanDefinitionRegistryPostProcessor, Ordered {
    private static final String SCAN_BASE_PACKAGE = "energy.eddie.regionconnector";
    private static final Logger LOGGER = LoggerFactory.getLogger(RegionConnectorRegistrationBeanPostProcessor.class);

    /**
     * Creates a {@link DispatcherServlet} for the region connector which will be mapped to {@code regionConnectorName}
     * and returns an {@link AbstractBeanDefinition} that wraps this servlet and which can be used to register the
     * servlet in the root context.
     *
     * @param regionConnectorContext Context of the region connector.
     * @param regionConnectorName    Unique name of the region connector, as defined in {@link SpringRegionConnector}.
     * @return AbstractBeanDefinition that can be registered in the root context (i.e. the EDDIE core context).
     */
    @NonNull
    private static AbstractBeanDefinition createBeanDefinition(
            AnnotationConfigWebApplicationContext regionConnectorContext,
            String regionConnectorName) {
        String urlMapping = "/%s/*".formatted(regionConnectorName);
        LOGGER.info("Registering new region connector with URL mapping {}", urlMapping);
        DispatcherServlet dispatcherServlet = new DispatcherServlet(regionConnectorContext);

        ServletRegistrationBean<DispatcherServlet> connectorServletBean = new ServletRegistrationBean<>(dispatcherServlet, urlMapping);
        // use unique name
        connectorServletBean.setName(urlMapping);
        // start all region connector servlets with same priority
        connectorServletBean.setLoadOnStartup(2);

        return BeanDefinitionBuilder
                .genericBeanDefinition(ServletRegistrationBean.class, () -> connectorServletBean)
                .getBeanDefinition();
    }

    /**
     * Creates a separate application web context for the region connector using the passed configuration class.
     * This context won't explicitly have a parent set, this will be done by the {@link DispatcherServlet}.
     *
     * @param regionConnectorConfigClass Configuration class containing bean definitions of the region connector.
     * @param regionConnectorName        Unique name of the region connector, as defined in {@link SpringRegionConnector}.
     * @return AnnotationConfigWebApplicationContext for the region connector.
     */
    @NonNull
    private static AnnotationConfigWebApplicationContext createWebContext(Class<?> regionConnectorConfigClass, String regionConnectorName) {
        // DispatcherServlet will set the parent automatically and do initialization work like calling refresh
        // see https://web.archive.org/web/20231207072642/https://ccbill.com/blog/spring-boot-and-context-handling
        AnnotationConfigWebApplicationContext applicationContext = new AnnotationConfigWebApplicationContext();
        applicationContext.register(regionConnectorConfigClass);
//                applicationContext.register(RegionConnectorServiceBinder.class);
        applicationContext.setId(regionConnectorName);
        return applicationContext;
    }

    /**
     * Scans the {@value SCAN_BASE_PACKAGE} package for any classes that are annotated with {@link SpringRegionConnector} and
     * creates a separate context and servlet for each found class, which will be registered with the registry passed to this processor.
     * The servlet has its URL mapping set to {@link SpringRegionConnector#name()}.
     *
     * @param registry the bean definition registry used by the application context
     */
    @Override
    public void postProcessBeanDefinitionRegistry(@NonNull BeanDefinitionRegistry registry) {
        LOGGER.info("Starting scan for classes on the classpath annotated with @RegionConnector");

        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(SpringRegionConnector.class));

        Set<BeanDefinition> beanDefs = scanner.findCandidateComponents(SCAN_BASE_PACKAGE);
        for (BeanDefinition bd : beanDefs) {
            try {
                Class<?> regionConnectorConfigClass = Class.forName(bd.getBeanClassName());
                String regionConnectorName = regionConnectorConfigClass.getAnnotation(SpringRegionConnector.class).name();

                var applicationContext = createWebContext(regionConnectorConfigClass, regionConnectorName);
                var beanDefinition = createBeanDefinition(applicationContext, regionConnectorName);

                registry.registerBeanDefinition(regionConnectorName, beanDefinition);
            } catch (ClassNotFoundException e) {
                LOGGER.error("Found a region connector bean definition {}, but couldn't get the class for it", bd, e);
            }
        }
    }

    @Override
    public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory unusedBeanFactory) throws BeansException {
        // not needed by this processor
    }

    /**
     * Run this processor as last one.
     * Spring documentation recommends to implement {@link Ordered} interface for bean post processors.
     *
     * @return Integer.MAX_VALUE
     */
    @Override
    public int getOrder() {
        return Integer.MAX_VALUE;
    }
}

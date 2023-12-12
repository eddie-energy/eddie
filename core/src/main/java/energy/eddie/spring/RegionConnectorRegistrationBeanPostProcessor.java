package energy.eddie.spring;

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
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RegionConnectorRegistrationBeanPostProcessor implements BeanDefinitionRegistryPostProcessor, Ordered, EnvironmentAware {
    public static final String ALL_REGION_CONNECTORS_BASE_URL_PATH = "region-connectors";
    private static final String REGION_CONNECTORS_SCAN_BASE_PACKAGE = "energy.eddie.regionconnector";
    private static final String REGION_CONNECTOR_PROCESSORS_SCAN_BASE_PACKAGE = "energy.eddie.spring.rcprocessors";
    private static final Logger LOGGER = LoggerFactory.getLogger(RegionConnectorRegistrationBeanPostProcessor.class);
    @Nullable
    private Environment environment;

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
            String regionConnectorName
    ) {
        String urlMapping = "/%s/%s/*".formatted(ALL_REGION_CONNECTORS_BASE_URL_PATH, regionConnectorName);
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
     * @param regionConnectorConfigClass      Configuration class containing bean definitions of the region connector.
     * @param regionConnectorName             Unique name of the region connector, as defined in {@link SpringRegionConnector}.
     * @param regionConnectorProcessorClasses List of classes that should be registered in the newly created WebContext.
     * @return AnnotationConfigWebApplicationContext for the region connector.
     */
    @NonNull
    private static AnnotationConfigWebApplicationContext createWebContext(
            Class<?> regionConnectorConfigClass,
            String regionConnectorName,
            List<Class<?>> regionConnectorProcessorClasses) {
        // DispatcherServlet will set the parent automatically and do initialization work like calling refresh
        // see https://web.archive.org/web/20231207072642/https://ccbill.com/blog/spring-boot-and-context-handling
        AnnotationConfigWebApplicationContext applicationContext = new AnnotationConfigWebApplicationContext();
        applicationContext.setId(regionConnectorName);
        applicationContext.register(regionConnectorConfigClass);

        // register any region connector processor
        regionConnectorProcessorClasses.forEach(applicationContext::register);
        return applicationContext;
    }

    @Override
    public void setEnvironment(@NonNull Environment environment) {
        this.environment = environment;
    }

    /**
     * Scans the {@value REGION_CONNECTORS_SCAN_BASE_PACKAGE} package for any classes that are annotated with {@link SpringRegionConnector}
     * and creates a separate context and {@link DispatcherServlet} for each found class,
     * which will be registered with the registry passed to this processor.
     * <p>
     * <b>Important:</b> Only region connectors that have their property <i>region-connector.RC-NAME.enabled</i> explicitly set to <i>true</i> are loaded.
     * </p>
     * <p>
     * The DispatcherServlet has its URL mapping set to "/{@link #ALL_REGION_CONNECTORS_BASE_URL_PATH}/{RC-NAME}/*"
     * whereas {@code RC-NAME} is specified by {@link SpringRegionConnector#name()}.
     * </p>
     *
     * @param registry the bean definition registry used by the application context
     * @throws InitializationException If no region connector has been enabled in the {@code application.properties} file or any other unexpected initialization error occurs.
     */
    @Override
    public void postProcessBeanDefinitionRegistry(@NonNull BeanDefinitionRegistry registry) throws InitializationException {
        LOGGER.info("Starting scan for classes on the classpath annotated with @RegionConnector");

        if (environment == null)
            throw new IllegalStateException("environment must not be null");

        Set<BeanDefinition> regionConnectorBeanDefinitions = findAllSpringRegionConnectorBeanDefinitions();
        List<Class<?>> regionConnectorProcessorClasses = findAllRegionConnectorProcessorClasses();

        for (BeanDefinition rcDefinition : regionConnectorBeanDefinitions) {
            try {
                Class<?> regionConnectorConfigClass = Class.forName(rcDefinition.getBeanClassName());
                String regionConnectorName = regionConnectorConfigClass.getAnnotation(SpringRegionConnector.class).name();

                var propertyName = "region-connector.%s.enabled".formatted(regionConnectorName.replace('-', '.'));

                if (Boolean.FALSE.equals(environment.getProperty(propertyName, Boolean.class, false))) {
                    LOGGER.info("Region connector {} not explicitly enabled by property, will not load it.", regionConnectorName);
                } else {
                    var applicationContext = createWebContext(regionConnectorConfigClass, regionConnectorName, regionConnectorProcessorClasses);
                    var beanDefinition = createBeanDefinition(applicationContext, regionConnectorName);

                    registry.registerBeanDefinition(regionConnectorName, beanDefinition);
                }
            } catch (ClassNotFoundException e) {
                LOGGER.error("Found a region connector bean definition {}, but couldn't get the class for it", rcDefinition, e);
            }
        }
    }

    private Set<BeanDefinition> findAllSpringRegionConnectorBeanDefinitions() {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(SpringRegionConnector.class));

        return scanner.findCandidateComponents(REGION_CONNECTORS_SCAN_BASE_PACKAGE);
    }

    // Ignore warning to use .toList because it doesn't return a List<Class<?>> but a List<? extends Class<?>>
    @SuppressWarnings("java:S6204")
    private List<Class<?>> findAllRegionConnectorProcessorClasses() throws InitializationException {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RegionConnectorProcessor.class));

        var beanDefinitions = scanner.findCandidateComponents(REGION_CONNECTOR_PROCESSORS_SCAN_BASE_PACKAGE);

        if (LOGGER.isInfoEnabled()) {
            String[] processorNames = beanDefinitions.stream().map(BeanDefinition::getBeanClassName).toArray(String[]::new);
            LOGGER.info("Found {} region connector processors which will be registered for each region connector individually: {}", beanDefinitions.size(), processorNames);
        }

        return beanDefinitions.stream().map(beanDefinition -> {
            try {
                return Class.forName(beanDefinition.getBeanClassName());
            } catch (ClassNotFoundException e) {
                throw new InitializationException("Couldn't find class for RegionConnectorProcessor. %s".formatted(e.getMessage()));
            }
        }).collect(Collectors.toList());
    }

    @Override
    public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory unusedBeanFactory) throws BeansException {
        // not needed by this processor
    }

    /**
     * Run this processor as the last one.
     * Spring documentation recommends to implement {@link Ordered} interface for bean post processors.
     *
     * @return Integer.MAX_VALUE
     */
    @Override
    public int getOrder() {
        return Integer.MAX_VALUE;
    }
}

package energy.eddie.spring;

import energy.eddie.api.agnostic.RegionConnector;
import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.regionconnector.shared.utils.CommonPaths;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.webmvc.core.configuration.MultipleOpenApiSupportConfiguration;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springdoc.webmvc.ui.SwaggerConfig;
import org.springdoc.webmvc.ui.SwaggerController;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RegionConnectorRegistrationBeanPostProcessor implements BeanDefinitionRegistryPostProcessor, Ordered {
    /**
     * Name of the Bean registered in the core context that holds a list of the IDs of the enabled region connectors.
     */
    public static final String ENABLED_REGION_CONNECTOR_BEAN_NAME = "enabledRegionConnectors";
    /**
     * Base package under which all region connectors are located.
     */
    public static final String REGION_CONNECTORS_SCAN_BASE_PACKAGE = "energy.eddie.regionconnector";
    public static final String OUTBOUND_CONNECTORS_SCAN_BASE_PACKAGE = "energy.eddie.outbound";
    private static final Logger LOGGER = LoggerFactory.getLogger(RegionConnectorRegistrationBeanPostProcessor.class);
    private final Environment environment;

    public RegionConnectorRegistrationBeanPostProcessor(Environment environment) {this.environment = environment;}

    public static Class<?> classForBeanDefinition(BeanDefinition definition) {
        try {
            return Class.forName(definition.getBeanClassName());
        } catch (ClassNotFoundException e) {
            throw new InitializationException(
                    "Found region connector bean definition for class '%s', but couldn't get the class for it, this must not happen, will abort launch".formatted(
                            definition.getBeanClassName()),
                    e);
        }
    }

    /**
     * Enables SpringDoc OpenAPI in the specified {@code applicationContext} by in registering the necessary Spring and
     * Swagger Beans. The documentation will be available at the {@code urlMapping} of the context's dispatcher servlet,
     * e.g. "/region-connectors/dk-energinet/v3/api-docs".
     *
     * @param applicationContext Context for which to enable the SpringDoc support.
     */
    public static void enableSpringDoc(AnnotationConfigWebApplicationContext applicationContext) {
        applicationContext.register(
                SwaggerConfig.class,
                SwaggerUiConfigProperties.class,
                SwaggerUiOAuthProperties.class,
                SpringDocWebMvcConfiguration.class,
                MultipleOpenApiSupportConfiguration.class,
                SpringDocConfiguration.class,
                SpringDocConfigProperties.class,
                JacksonAutoConfiguration.class,
                SwaggerController.class
        );
    }

    /**
     * Scans the {@value REGION_CONNECTORS_SCAN_BASE_PACKAGE} package for any classes that are annotated with
     * {@link RegionConnector} and creates a separate context and {@link DispatcherServlet} for each found class, which
     * will be registered with the registry passed to this processor.
     * <p>
     * <b>Important:</b> Only region connectors that have their property <i>region-connector.RC-NAME.enabled</i>
     * explicitly set to <i>true</i> are loaded.
     * </p>
     * <p>
     * The DispatcherServlet has its URL mapping set to
     * "/{@value CommonPaths#ALL_REGION_CONNECTORS_BASE_URL_PATH}/{RC-NAME}/*" whereas {@code RC-NAME} is specified by
     * {@link RegionConnector#name()}.
     * </p>
     *
     * @param registry the bean definition registry used by the application context
     * @throws InitializationException If no region connector has been enabled in the {@code application.properties}
     *                                 file or any other unexpected initialization error occurs.
     */
    @Override
    public void postProcessBeanDefinitionRegistry(@NonNull BeanDefinitionRegistry registry) throws InitializationException {
        LOGGER.info("Starting scan for classes on the classpath annotated with @RegionConnector");

        List<Class<?>> regionConnectorProcessorClasses = findAllRegionConnectorProcessorClasses(environment);

        List<String> enabledRegionConnectorNames = new ArrayList<>();

        for (Class<?> configClass : getEnabledRegionConnectorConfigClasses()) {
            var regionConnectorName = configClass.getAnnotation(RegionConnector.class).name();
            enabledRegionConnectorNames.add(regionConnectorName);

            var metadata = findMetadataClass(configClass, regionConnectorName);
            var applicationContext = createWebContext(configClass,
                                                      regionConnectorName,
                                                      regionConnectorProcessorClasses,
                                                      metadata);
            var beanDefinition = createRegionConnectorBeanDefinition(applicationContext, regionConnectorName);

            registry.registerBeanDefinition(regionConnectorName, beanDefinition);
        }

        registerEnabledRegionConnectorsBeanDefinition(registry, enabledRegionConnectorNames);
    }

    @Override
    public void postProcessBeanFactory(@org.jspecify.annotations.NonNull ConfigurableListableBeanFactory unusedBeanFactory) throws BeansException {
        // not needed by this processor
    }

    /**
     * Run this processor as the last one. Spring documentation recommends to implement {@link Ordered} interface for
     * bean post processors.
     *
     * @return Integer.MAX_VALUE
     */
    @Override
    public int getOrder() {
        return Integer.MAX_VALUE;
    }

    private Class<?> findMetadataClass(Class<?> configClass, String regionConnectorName) {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(RegionConnectorMetadata.class));
        scanner.setEnvironment(environment);
        var candidates = scanner.findCandidateComponents(configClass.getPackage().getName());
        if (candidates.size() != 1) {
            var names = candidates.stream()
                                  .map(BeanDefinition::getBeanClassName)
                                  .collect(Collectors.joining(", "));
            throw new BeanCreationException(regionConnectorName + " contains less or more than one metadata definition! " + names);
        }
        try {
            return Class.forName(candidates.iterator().next().getBeanClassName());
        } catch (ClassNotFoundException e) {
            throw new InitializationException("Couldn't find class for RegionConnectorMetadata. %s".formatted(e.getMessage()),
                                              e);
        }
    }

    // Ignore warning to use .toList because it doesn't return a List<Class<?>> but a List<? extends Class<?>>
    @SuppressWarnings("java:S6204")
    private List<Class<?>> findAllRegionConnectorProcessorClasses(Environment scannerEnvironment) throws InitializationException {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RegionConnectorExtension.class));
        // use environment of parent to properly evaluate @Conditional annotations
        scanner.setEnvironment(scannerEnvironment);

        // scan in all packages for extensions (e.g. data needs controller advice is in a different package than the other extensions)
        var beanDefinitions = scanner.findCandidateComponents("energy.eddie");

        if (LOGGER.isInfoEnabled()) {
            String[] processorNames = beanDefinitions.stream()
                                                     .map(BeanDefinition::getBeanClassName)
                                                     .toArray(String[]::new);
            LOGGER.info(
                    "Found {} region connector processors which will be registered for each region connector individually: {}",
                    beanDefinitions.size(),
                    processorNames);
        }

        return beanDefinitions.stream().map(beanDefinition -> {
            try {
                return Class.forName(beanDefinition.getBeanClassName());
            } catch (ClassNotFoundException e) {
                throw new InitializationException("Couldn't find class for RegionConnectorExtension. %s".formatted(e.getMessage()),
                                                  e);
            }
        }).collect(Collectors.toList());
    }

    /**
     * Returns a Set containing the config classes (i.e. the classes annotated with {@link RegionConnector}) of all
     * enabled region connectors.
     */
    private Set<Class<?>> getEnabledRegionConnectorConfigClasses() {
        return findAllSpringRegionConnectorBeanDefinitions()
                .stream()
                .map(RegionConnectorRegistrationBeanPostProcessor::classForBeanDefinition)
                .filter(this::isRegionConnectorEnabled)
                .collect(Collectors.toSet());
    }

    /**
     * Creates a separate application web context for the region connector using the passed configuration class. This
     * context won't explicitly have a parent set, this will be done by the {@link DispatcherServlet}.
     *
     * @param regionConnectorConfigClass      Configuration class containing bean definitions of the region connector.
     * @param regionConnectorName             Unique name of the region connector, as defined in
     *                                        {@link RegionConnector}.
     * @param regionConnectorProcessorClasses List of classes that should be registered in the newly created
     *                                        WebContext.
     * @param metadata                        Automatically registers the metadata as a bean, since it has to be present in every region-connector
     * @return AnnotationConfigWebApplicationContext for the region connector.
     */
    @org.jspecify.annotations.NonNull
    private static AnnotationConfigWebApplicationContext createWebContext(
            Class<?> regionConnectorConfigClass,
            String regionConnectorName,
            List<Class<?>> regionConnectorProcessorClasses,
            Class<?> metadata
    ) {
        // DispatcherServlet will set the parent automatically and do initialization work like calling refresh
        // see https://web.archive.org/web/20231207072642/https://ccbill.com/blog/spring-boot-and-context-handling
        AnnotationConfigWebApplicationContext applicationContext = new AnnotationConfigWebApplicationContext();
        applicationContext.setId(regionConnectorName);
        applicationContext.register(regionConnectorConfigClass);
        applicationContext.register(metadata);

        enableSpringDoc(applicationContext);

        // register any region connector processor
        regionConnectorProcessorClasses.forEach(applicationContext::register);
        return applicationContext;
    }

    /**
     * Creates a {@link DispatcherServlet} for the region connector which will be mapped to {@code regionConnectorName}
     * and returns an {@link AbstractBeanDefinition} that wraps this servlet and which can be used to register the
     * servlet in the root context.
     *
     * @param regionConnectorContext Context of the region connector.
     * @param regionConnectorName    Unique name of the region connector, as defined in {@link RegionConnector}.
     * @return AbstractBeanDefinition that can be registered in the root context (i.e. the EDDIE core context).
     */
    @org.jspecify.annotations.NonNull
    private static AbstractBeanDefinition createRegionConnectorBeanDefinition(
            AnnotationConfigWebApplicationContext regionConnectorContext,
            String regionConnectorName
    ) {
        String urlMapping = CommonPaths.getServletPathForRegionConnector(regionConnectorName);
        LOGGER.info("Registering new region connector with URL mapping {}", urlMapping);
        DispatcherServlet dispatcherServlet = new DispatcherServlet(regionConnectorContext);

        ServletRegistrationBean<@org.jspecify.annotations.NonNull DispatcherServlet> connectorServletBean = new ServletRegistrationBean<>(
                dispatcherServlet,
                urlMapping);
        // use unique name
        connectorServletBean.setName(regionConnectorName);
        // start all region connector servlets with same priority
        connectorServletBean.setLoadOnStartup(Integer.MAX_VALUE);

        return BeanDefinitionBuilder
                .genericBeanDefinition(ServletRegistrationBean.class, () -> connectorServletBean)
                .getBeanDefinition();
    }

    /**
     * Registers a List&lt;String&gt; containing the names of the enabled region connectors.
     */
    private void registerEnabledRegionConnectorsBeanDefinition(
            BeanDefinitionRegistry registry,
            List<String> enabledRegionConnectorNames
    ) {
        // copy list to prevent that modifications of enabledRegionConnectorNames influence the Bean and thereby other application parts
        List<String> copy = new ArrayList<>(enabledRegionConnectorNames);
        AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
                .genericBeanDefinition(List.class, () -> copy)
                .getBeanDefinition();
        registry.registerBeanDefinition(ENABLED_REGION_CONNECTOR_BEAN_NAME, beanDefinition);
    }

    private Set<BeanDefinition> findAllSpringRegionConnectorBeanDefinitions() {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RegionConnector.class));

        return scanner.findCandidateComponents(REGION_CONNECTORS_SCAN_BASE_PACKAGE);
    }

    // False positive for nonnull boolean
    @SuppressWarnings("java:S5411")
    private boolean isRegionConnectorEnabled(Class<?> configClass) {
        var regionConnectorName = configClass.getAnnotation(RegionConnector.class).name();
        var propertyName = "region-connector.%s.enabled".formatted(regionConnectorName.replace('-', '.'));

        var isEnabled = environment.getProperty(propertyName, Boolean.class, false);

        if (!isEnabled) {
            LOGGER.info("Region connector {} not explicitly enabled by property, will not load it.",
                        regionConnectorName);
        }

        return isEnabled;
    }
}

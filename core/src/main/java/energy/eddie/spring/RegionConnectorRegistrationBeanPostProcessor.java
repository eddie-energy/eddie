package energy.eddie.spring;

import energy.eddie.api.agnostic.RegionConnector;
import energy.eddie.api.agnostic.RegionConnectorExtension;
import org.flywaydb.core.Flyway;
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
import org.springdoc.webmvc.ui.SwaggerController;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RegionConnectorRegistrationBeanPostProcessor implements BeanDefinitionRegistryPostProcessor, Ordered, EnvironmentAware {
    public static final String ALL_REGION_CONNECTORS_BASE_URL_PATH = "region-connectors";
    /**
     * Name of the Bean registered in the core context that holds a list of the IDs of the enabled region connectors.
     */
    public static final String ENABLED_REGION_CONNECTOR_BEAN_NAME = "enabledRegionConnectors";
    private static final String REGION_CONNECTORS_SCAN_BASE_PACKAGE = "energy.eddie.regionconnector";
    private static final String REGION_CONNECTOR_PROCESSORS_SCAN_BASE_PACKAGE = "energy.eddie.spring.regionconnector.extensions";
    private static final Logger LOGGER = LoggerFactory.getLogger(RegionConnectorRegistrationBeanPostProcessor.class);
    @Nullable
    private Environment environment;

    /**
     * Creates a {@link DispatcherServlet} for the region connector which will be mapped to {@code regionConnectorName}
     * and returns an {@link AbstractBeanDefinition} that wraps this servlet and which can be used to register the
     * servlet in the root context.
     *
     * @param regionConnectorContext Context of the region connector.
     * @param regionConnectorName    Unique name of the region connector, as defined in {@link RegionConnector}.
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
        connectorServletBean.setName(regionConnectorName);
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
     * @param regionConnectorName             Unique name of the region connector, as defined in {@link RegionConnector}.
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

        enableSpringDoc(applicationContext);

        // register any region connector processor
        regionConnectorProcessorClasses.forEach(applicationContext::register);
        return applicationContext;
    }

    @Override
    public void setEnvironment(@NonNull Environment environment) {
        this.environment = environment;
    }

    /**
     * Scans the {@value REGION_CONNECTORS_SCAN_BASE_PACKAGE} package for any classes that are annotated with {@link RegionConnector}
     * and creates a separate context and {@link DispatcherServlet} for each found class,
     * which will be registered with the registry passed to this processor.
     * <p>
     * <b>Important:</b> Only region connectors that have their property <i>region-connector.RC-NAME.enabled</i> explicitly set to <i>true</i> are loaded.
     * </p>
     * <p>
     * The DispatcherServlet has its URL mapping set to "/{@link #ALL_REGION_CONNECTORS_BASE_URL_PATH}/{RC-NAME}/*"
     * whereas {@code RC-NAME} is specified by {@link RegionConnector#name()}.
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
        List<Class<?>> regionConnectorProcessorClasses = findAllRegionConnectorProcessorClasses(environment);

        List<String> regionConnectorNames = new ArrayList<>();
        List<String> enabledRegionConnectorNames = new ArrayList<>();

        for (BeanDefinition rcDefinition : regionConnectorBeanDefinitions) {
            try {
                Class<?> regionConnectorConfigClass = Class.forName(rcDefinition.getBeanClassName());
                String regionConnectorName = regionConnectorConfigClass.getAnnotation(RegionConnector.class).name();
                regionConnectorNames.add(regionConnectorName);
                var propertyName = "region-connector.%s.enabled".formatted(regionConnectorName.replace('-', '.'));

                if (Boolean.FALSE.equals(environment.getProperty(propertyName, Boolean.class, false))) {
                    LOGGER.info("Region connector {} not explicitly enabled by property, will not load it.", regionConnectorName);
                } else {
                    enabledRegionConnectorNames.add(regionConnectorName);
                    var applicationContext = createWebContext(regionConnectorConfigClass, regionConnectorName, regionConnectorProcessorClasses);
                    var beanDefinition = createBeanDefinition(applicationContext, regionConnectorName);

                    registry.registerBeanDefinition(regionConnectorName, beanDefinition);
                }
            } catch (ClassNotFoundException e) {
                LOGGER.error("Found a region connector bean definition {}, but couldn't get the class for it", rcDefinition, e);
            }
        }

        registerEnabledRegionConnectorsBeanDefinition(registry, enabledRegionConnectorNames);
        registerFlywayStrategy(registry, regionConnectorNames);
    }

    /**
     * Creates a {@link FlywayMigrationStrategy} that creates the schemas for all region connectors and
     * the {@code core}, as well as executing any migration scripts found in the respective folders on the classpath.
     * The folder pattern is: "db/migration/&lt;region-connector-name&gt;".
     * Any minus ('-') in the region connector's name will be replaced by an underscore ('_') for the schema name.
     *
     * @param registry                    BeanDefinitionRegistry where the {@link FlywayMigrationStrategy} is registered.
     * @param enabledRegionConnectorNames List of the names of the enabled region connectors for which the migrations will be run.
     */
    private void registerFlywayStrategy(BeanDefinitionRegistry registry, List<String> enabledRegionConnectorNames) {
        FlywayMigrationStrategy strategy = flyway -> {
            // also execute flyway migration for core
            enabledRegionConnectorNames.add("core");
            enabledRegionConnectorNames.forEach(regionConnectorName -> {
                var schemaName = regionConnectorName.replace('-', '_');
                Flyway.configure()
                        .configuration(flyway.getConfiguration())
                        .schemas(schemaName)
                        .locations("db/migration/" + regionConnectorName)
                        .load()
                        .migrate();
            });
        };

        registry.registerBeanDefinition("flywayMigrationStrategy", BeanDefinitionBuilder
                .genericBeanDefinition(FlywayMigrationStrategy.class, () -> strategy)
                .getBeanDefinition());
    }

    private Set<BeanDefinition> findAllSpringRegionConnectorBeanDefinitions() {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RegionConnector.class));

        return scanner.findCandidateComponents(REGION_CONNECTORS_SCAN_BASE_PACKAGE);
    }

    // Ignore warning to use .toList because it doesn't return a List<Class<?>> but a List<? extends Class<?>>
    @SuppressWarnings("java:S6204")
    private List<Class<?>> findAllRegionConnectorProcessorClasses(Environment scannerEnvironment) throws InitializationException {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RegionConnectorExtension.class));
        // use environment of parent to properly evaluate @Conditional annotations
        scanner.setEnvironment(scannerEnvironment);

        var beanDefinitions = scanner.findCandidateComponents(REGION_CONNECTOR_PROCESSORS_SCAN_BASE_PACKAGE);

        if (LOGGER.isInfoEnabled()) {
            String[] processorNames = beanDefinitions.stream().map(BeanDefinition::getBeanClassName).toArray(String[]::new);
            LOGGER.info("Found {} region connector processors which will be registered for each region connector individually: {}", beanDefinitions.size(), processorNames);
        }

        return beanDefinitions.stream().map(beanDefinition -> {
            try {
                return Class.forName(beanDefinition.getBeanClassName());
            } catch (ClassNotFoundException e) {
                throw new InitializationException("Couldn't find class for RegionConnectorExtension. %s".formatted(e.getMessage()));
            }
        }).collect(Collectors.toList());
    }

    private void registerEnabledRegionConnectorsBeanDefinition(
            BeanDefinitionRegistry registry,
            List<String> enabledRegionConnectorNames
    ) {
        AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
                .genericBeanDefinition(List.class, () -> enabledRegionConnectorNames)
                .getBeanDefinition();
        registry.registerBeanDefinition(ENABLED_REGION_CONNECTOR_BEAN_NAME, beanDefinition);
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
                SwaggerUiConfigParameters.class,
                SwaggerUiOAuthProperties.class,
                SpringDocWebMvcConfiguration.class,
                MultipleOpenApiSupportConfiguration.class,
                SpringDocConfiguration.class,
                SpringDocConfigProperties.class,
                JacksonAutoConfiguration.class,
                SwaggerController.class
        );
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

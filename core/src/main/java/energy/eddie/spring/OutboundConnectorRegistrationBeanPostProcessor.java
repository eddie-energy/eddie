package energy.eddie.spring;

import energy.eddie.api.agnostic.OutboundConnector;
import energy.eddie.api.agnostic.OutboundConnectorExtension;
import energy.eddie.api.agnostic.RegionConnector;
import energy.eddie.regionconnector.shared.utils.CommonPaths;
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
import org.springframework.core.env.Environment;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.lang.NonNull;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class OutboundConnectorRegistrationBeanPostProcessor implements BeanDefinitionRegistryPostProcessor, Ordered {
    /**
     * Name of the Bean registered in the core context that holds a list of the IDs of the enabled outbound-connectors.
     */
    public static final String ENABLED_OUTBOUND_CONNECTOR_BEAN_NAME = "enabledOutboundConnectors";
    /**
     * Base package under which all outbound-connectors are located.
     */
    public static final String OUTBOUND_CONNECTORS_SCAN_BASE_PACKAGE = "energy.eddie";
    private static final Logger LOGGER = LoggerFactory.getLogger(OutboundConnectorRegistrationBeanPostProcessor.class);
    private final Environment environment;

    public OutboundConnectorRegistrationBeanPostProcessor(Environment environment) {this.environment = environment;}

    /**
     * Scans the {@value OUTBOUND_CONNECTORS_SCAN_BASE_PACKAGE} package for any classes that are annotated with
     * {@link OutboundConnector} and creates a separate context and {@link DispatcherServlet} for each found class, which
     * will be registered with the registry passed to this processor.
     * <p>
     * <b>Important:</b> Only outbound-connectors that have their property <i>outbound-connector.OC-NAME.enabled</i>
     * explicitly set to <i>true</i> are loaded.
     * </p>
     * <p>
     * The DispatcherServlet has its URL mapping set to
     * "/{@value CommonPaths#ALL_REGION_CONNECTORS_BASE_URL_PATH}/{RC-NAME}/*" whereas {@code RC-NAME} is specified by
     * {@link OutboundConnector#name()}.
     * </p>
     *
     * @param registry the bean definition registry used by the application context
     * @throws InitializationException If no outbound-connector has been enabled in the {@code application.properties}
     *                                 file or any other unexpected initialization error occurs.
     */
    @Override
    public void postProcessBeanDefinitionRegistry(@NonNull BeanDefinitionRegistry registry) throws InitializationException {
        LOGGER.info("Starting scan for classes on the classpath annotated with @OutboundConnector");

        List<Class<?>> outboundConnectorProcessorClasses = findAllOutboundConnectorProcessorClasses(environment);

        List<String> enabledOutboundConnectorNames = new ArrayList<>();

        for (Class<?> configClass : getEnabledOutboundConnectorConfigClasses()) {
            var outboundConnectorName = configClass.getAnnotation(OutboundConnector.class).name();
            enabledOutboundConnectorNames.add(outboundConnectorName);

            var applicationContext = createWebContext(
                    configClass,
                    outboundConnectorName,
                    outboundConnectorProcessorClasses
            );
            var beanDefinition = createOutboundConnectorBeanDefinition(applicationContext, outboundConnectorName);

            registry.registerBeanDefinition(outboundConnectorName, beanDefinition);
        }
        registerEnabledOutboundConnectorsBeanDefinition(registry, enabledOutboundConnectorNames);
    }

    @Override
    public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory unusedBeanFactory) throws BeansException {
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

    private static Class<?> classForBeanDefinition(BeanDefinition definition) {
        try {
            return Class.forName(definition.getBeanClassName());
        } catch (ClassNotFoundException e) {
            throw new InitializationException(
                    "Found outbound-connector bean definition for class '%s', but couldn't get the class for it, this must not happen, will abort launch"
                            .formatted(definition.getBeanClassName()),
                    e
            );
        }
    }

    // Ignore warning to use .toList because it doesn't return a List<Class<?>> but a List<? extends Class<?>>
    @SuppressWarnings("java:S6204")
    private List<Class<?>> findAllOutboundConnectorProcessorClasses(Environment scannerEnvironment) throws InitializationException {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(OutboundConnectorExtension.class));
        // use environment of parent to properly evaluate @Conditional annotations
        scanner.setEnvironment(scannerEnvironment);

        // scan in all packages for extensions (e.g. data needs controller advice is in a different package than the other extensions)
        var beanDefinitions = scanner.findCandidateComponents(OUTBOUND_CONNECTORS_SCAN_BASE_PACKAGE);
        LOGGER.atInfo()
              .addArgument(beanDefinitions::size)
              .addArgument(() -> beanDefinitions.stream()
                                                .map(BeanDefinition::getBeanClassName)
                                                .toArray(String[]::new))
              .log("Found {} outbound-connector processors which will be registered for each outbound-connector individually: {}");
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
     * Returns a Set containing the config classes (i.e. the classes annotated with {@link OutboundConnector}) of all
     * enabled outbound-connectors.
     */
    private Set<Class<?>> getEnabledOutboundConnectorConfigClasses() {
        return findAllSpringOutboundConnectorBeanDefinitions()
                .stream()
                .map(OutboundConnectorRegistrationBeanPostProcessor::classForBeanDefinition)
                .filter(this::isOutboundConnectorEnabled)
                .collect(Collectors.toSet());
    }

    /**
     * Creates a separate application web context for the outbound-connector using the passed configuration class. This
     * context won't explicitly have a parent set, this will be done by the {@link DispatcherServlet}.
     *
     * @param outboundConnectorConfigClass      Configuration class containing bean definitions of the outbound-connector.
     * @param outboundConnectorName             Unique name of the outbound-connector, as defined in
     *                                          {@link RegionConnector}.
     * @param outboundConnectorProcessorClasses List of classes that should be registered in the newly created
     *                                          WebContext.
     * @return AnnotationConfigWebApplicationContext for the outbound-connector.
     */
    @NonNull
    private static AnnotationConfigWebApplicationContext createWebContext(
            Class<?> outboundConnectorConfigClass,
            String outboundConnectorName,
            List<Class<?>> outboundConnectorProcessorClasses
    ) {
        // DispatcherServlet will set the parent automatically and do initialization work like calling refresh
        // see https://web.archive.org/web/20231207072642/https://ccbill.com/blog/spring-boot-and-context-handling
        AnnotationConfigWebApplicationContext applicationContext = new AnnotationConfigWebApplicationContext();
        applicationContext.setId(outboundConnectorName);
        applicationContext.register(outboundConnectorConfigClass);

        // register any outbound-connector processor
        outboundConnectorProcessorClasses.forEach(applicationContext::register);
        return applicationContext;
    }

    /**
     * Creates a {@link DispatcherServlet} for the outbound-connector which will be mapped to {@code outboundConnectorName}
     * and returns an {@link AbstractBeanDefinition} that wraps this servlet and which can be used to register the
     * servlet in the root context.
     *
     * @param outboundConnectorContext Context of the outbound-connector.
     * @param outboundConnectorName    Unique name of the outbound-connector, as defined in {@link RegionConnector}.
     * @return AbstractBeanDefinition that can be registered in the root context (i.e. the EDDIE core context).
     */
    @NonNull
    private static AbstractBeanDefinition createOutboundConnectorBeanDefinition(
            AnnotationConfigWebApplicationContext outboundConnectorContext,
            String outboundConnectorName
    ) {
        String urlMapping = CommonPaths.getServletPathForOutboundConnector(outboundConnectorName);
        LOGGER.info("Registering new outbound-connector with URL mapping {}", urlMapping);
        DispatcherServlet dispatcherServlet = new DispatcherServlet(outboundConnectorContext);

        var connectorServletBean = new ServletRegistrationBean<>(
                dispatcherServlet,
                urlMapping
        );
        // use unique name
        connectorServletBean.setName(outboundConnectorName);
        // start all outbound-connector servlets with same priority
        connectorServletBean.setLoadOnStartup(2);

        return BeanDefinitionBuilder
                .genericBeanDefinition(ServletRegistrationBean.class, () -> connectorServletBean)
                .getBeanDefinition();
    }

    /**
     * Registers a List&lt;String&gt; containing the names of the enabled outbound-connectors.
     */
    private void registerEnabledOutboundConnectorsBeanDefinition(
            BeanDefinitionRegistry registry,
            List<String> enabledRegionConnectorNames
    ) {
        // copy list to prevent that modifications of enabledOutboundConnectorNames influence the Bean and thereby other application parts
        List<String> copy = new ArrayList<>(enabledRegionConnectorNames);
        AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
                .genericBeanDefinition(List.class, () -> copy)
                .getBeanDefinition();
        registry.registerBeanDefinition(ENABLED_OUTBOUND_CONNECTOR_BEAN_NAME, beanDefinition);
    }

    private Set<BeanDefinition> findAllSpringOutboundConnectorBeanDefinitions() {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(OutboundConnector.class));

        return scanner.findCandidateComponents(OUTBOUND_CONNECTORS_SCAN_BASE_PACKAGE);
    }

    private boolean isOutboundConnectorEnabled(Class<?> configClass) {
        var outboundConnectorName = configClass.getAnnotation(OutboundConnector.class).name();
        var propertyName = "outbound-connector.%s.enabled".formatted(outboundConnectorName.replace('-', '.'));
        var isEnabled = Boolean.TRUE.equals(environment.getProperty(propertyName, Boolean.class, false));
        if (!isEnabled) {
            LOGGER.info("Outbound connector {} not explicitly enabled by property, will not load it.",
                        outboundConnectorName);
        }
        return isEnabled;
    }
}

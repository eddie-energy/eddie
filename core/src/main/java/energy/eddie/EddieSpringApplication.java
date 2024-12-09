package energy.eddie;

import energy.eddie.api.agnostic.outbound.OutboundConnectorSecurityConfig;
import energy.eddie.api.agnostic.RegionConnectorSecurityConfig;
import energy.eddie.core.CoreSpringConfig;
import energy.eddie.spring.OutboundConnectorRegistrationBeanPostProcessor;
import energy.eddie.spring.RegionConnectorRegistrationBeanPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.List;
import java.util.stream.Collectors;

import static energy.eddie.spring.RegionConnectorRegistrationBeanPostProcessor.OUTBOUND_CONNECTORS_SCAN_BASE_PACKAGE;
import static energy.eddie.spring.RegionConnectorRegistrationBeanPostProcessor.REGION_CONNECTORS_SCAN_BASE_PACKAGE;

public class EddieSpringApplication {
    public static void main(String[] args) {
        // apparently it does not work if Spring Security config classes are added by a BeanPostProcessor, but they have
        // to be present at startup already, so we scan the classpath and add all relevant security config classes to be instantiated in the core's application context
        var configClasses = findSecurityConfigsForEnabledRegionConnectors();
        configClasses.addAll(findSecurityConfigsForEnabledOutboundConnectors());
        configClasses.addFirst(CoreSpringConfig.class);

        SpringApplication.run(configClasses.toArray(Class[]::new), args);
    }

    /**
     * Scans the classpath for any class annotated with {@link RegionConnectorSecurityConfig} and returns a list
     * containing them.
     */
    @SuppressWarnings("java:S6204") // we want to return a modifiable list
    private static List<Class<?>> findSecurityConfigsForEnabledRegionConnectors() {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RegionConnectorSecurityConfig.class));

        return scanner.findCandidateComponents(REGION_CONNECTORS_SCAN_BASE_PACKAGE).stream()
                      .map(RegionConnectorRegistrationBeanPostProcessor::classForBeanDefinition)
                      .collect(Collectors.toList());
    }

    /**
     * Scans the classpath for any class annotated with {@link OutboundConnectorSecurityConfig} and returns a list
     * containing them.
     */
    @SuppressWarnings("java:S6204") // we want to return a modifiable list
    private static List<Class<?>> findSecurityConfigsForEnabledOutboundConnectors() {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(OutboundConnectorSecurityConfig.class));

        var candidates = scanner.findCandidateComponents(OUTBOUND_CONNECTORS_SCAN_BASE_PACKAGE);

        return candidates.stream()
                .map(OutboundConnectorRegistrationBeanPostProcessor::classForBeanDefinition)
                .collect(Collectors.toList());
    }
}

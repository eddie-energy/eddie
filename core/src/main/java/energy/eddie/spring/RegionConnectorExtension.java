package energy.eddie.spring;


import energy.eddie.spring.regionconnector.extensions.RegionConnectorNameExtension;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that indicates that the annotated class should be included in each context of a region connector.
 * It can be used to extend the capabilities of each region connector. An example would be the
 * {@link RegionConnectorNameExtension} which makes the name of RegionConnector
 * available as a Bean in its context.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Component
public @interface RegionConnectorExtension {
}

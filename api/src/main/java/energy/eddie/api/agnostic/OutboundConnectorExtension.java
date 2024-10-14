package energy.eddie.api.agnostic;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that indicates that the annotated class should be included in each context of a region connector.
 * It can be used to extend the capabilities of each region connector. An example would be the
 * {@code RegionConnectorNameExtension} in the core module, which makes the name of a region connector
 * available as a Bean in the region connector's context.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface OutboundConnectorExtension {
}

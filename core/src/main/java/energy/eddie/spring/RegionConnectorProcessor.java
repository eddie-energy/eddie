package energy.eddie.spring;


import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that indicates that the annotated class should be included in each context of a region connector.
 * <br/>
 * It can be used to define additional capabilities that each region connector should have or to e.g. register
 * a component of the region connector with a service of the parent.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Component
public @interface RegionConnectorProcessor {
}

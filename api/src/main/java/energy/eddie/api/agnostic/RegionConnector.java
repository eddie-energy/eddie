package energy.eddie.api.agnostic;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation allows the core to find region connector implementations during classpath scanning.
 * It is essential for region connectors to have this annotation on the base class of its module.
 * Should only be used in combination with {@code @SpringBootApplication}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface RegionConnector {
    /**
     * The name is the ID of the region connector.
     * It is used to identify the region connector in messages, for REST requests, etc.
     * Naming convention is {@code <two-letter country-code>-<permission administrator name>}, for example, {@code at-eda}.
     *
     * @return the ID of the region connector.
     */
    String name();
}

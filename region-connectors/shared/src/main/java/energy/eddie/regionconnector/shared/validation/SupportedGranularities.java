package energy.eddie.regionconnector.shared.validation;

import energy.eddie.api.agnostic.Granularity;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to validate that a given granularity is allowed.
 */
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SupportedGranularitiesValidator.class)
public @interface SupportedGranularities {

    Granularity[] value();

    String message() default "Value is not allowed";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
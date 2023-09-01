package energy.eddie.aiida.constraints;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotated <code>Permission</code> object's expirationTime has to be after
 * the startTime to be a valid permission.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {ExpirationTimeAfterStartTimeValidator.class})
public @interface ExpirationTimeAfterStartTime {
    String message() default "expirationTime has to be after startTime.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

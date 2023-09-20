package energy.eddie.aiida.constraints;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotated object's expirationTime has to be after the startTime.
 */
@Target({ElementType.TYPE, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {ExpirationTimeAfterStartTimePermissionDtoValidator.class, ExpirationTimeAfterStartTimePermissionValidator.class})
public @interface ExpirationTimeAfterStartTime {
    String message() default "expirationTime has to be after startTime.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

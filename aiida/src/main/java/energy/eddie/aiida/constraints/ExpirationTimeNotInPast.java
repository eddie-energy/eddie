package energy.eddie.aiida.constraints;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotated object's expirationTime must not be in the past.
 */
@Target({ElementType.TYPE, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {ExpirationTimeNotInPastPermissionDtoValidator.class})
public @interface ExpirationTimeNotInPast {
    String message() default "expirationTime must not lie in the past.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

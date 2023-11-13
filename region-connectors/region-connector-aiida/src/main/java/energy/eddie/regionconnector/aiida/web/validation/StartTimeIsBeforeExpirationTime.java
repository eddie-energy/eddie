package energy.eddie.regionconnector.aiida.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {StartTimeIsBeforeExpirationTimeValidator.class})
public @interface StartTimeIsBeforeExpirationTime {
    String message() default "startTime must be before expirationTime";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

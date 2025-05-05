package energy.eddie.aiida.models.record;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = RequireDataTagOrSourceKeyValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireDataTagOrSourceKey {
    String message() default "Either a data tag or a source key must be provided.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

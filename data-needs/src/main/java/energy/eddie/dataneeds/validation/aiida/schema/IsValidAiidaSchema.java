package energy.eddie.dataneeds.validation.aiida.schema;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = IsValidAiidaSchemaValidator.class)
public @interface IsValidAiidaSchema {
    String message() default "Validation for schema failed.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
package energy.eddie.dataneeds.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = IsValidValidatedHistoricalDataDataNeedValidator.class)
public @interface IsValidValidatedHistoricalDataDataNeed {
    String message() default "Validation for validated historical data data need failed.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

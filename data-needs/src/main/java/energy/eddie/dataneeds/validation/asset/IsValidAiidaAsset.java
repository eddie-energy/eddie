package energy.eddie.dataneeds.validation.asset;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = IsValidAiidaAssetValidator.class)
public @interface IsValidAiidaAsset {
    String message() default "Validation for schema failed.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

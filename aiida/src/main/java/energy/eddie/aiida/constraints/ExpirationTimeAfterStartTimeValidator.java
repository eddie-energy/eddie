package energy.eddie.aiida.constraints;

import energy.eddie.aiida.model.permission.Permission;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.Instant;

public class ExpirationTimeAfterStartTimeValidator implements ConstraintValidator<ExpirationTimeAfterStartTime, Permission> {
    @Override
    public boolean isValid(Permission permission, ConstraintValidatorContext context) {
        Instant exp = permission.expirationTime();
        Instant start = permission.startTime();

        // null check is required, because it's not guaranteed, that @NotNull annotation is validated before this one
        if (exp == null || start == null) {
            // disable default error message and set a custom one
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("startTime and expirationTime mustn't be null.")
                    .addConstraintViolation();
            return false;
        }

        return exp.isAfter(start);
    }
}

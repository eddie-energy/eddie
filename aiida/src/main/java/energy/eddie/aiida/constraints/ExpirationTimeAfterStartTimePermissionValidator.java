package energy.eddie.aiida.constraints;

import energy.eddie.aiida.model.permission.Permission;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ExpirationTimeAfterStartTimePermissionValidator implements ConstraintValidator<ExpirationTimeAfterStartTime, Permission> {
    @Override
    public boolean isValid(Permission permission, ConstraintValidatorContext context) {
        return ExpirationTimeAfterStartTimePermissionDtoValidator.validate(permission.startTime(), permission.expirationTime(), context);
    }
}

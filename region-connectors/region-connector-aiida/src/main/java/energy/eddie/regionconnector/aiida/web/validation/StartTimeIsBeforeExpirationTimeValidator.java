package energy.eddie.regionconnector.aiida.web.validation;

import energy.eddie.regionconnector.aiida.dtos.PermissionRequestForCreation;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StartTimeIsBeforeExpirationTimeValidator
        implements ConstraintValidator<StartTimeIsBeforeExpirationTime, PermissionRequestForCreation> {
    @Override
    public boolean isValid(PermissionRequestForCreation value, ConstraintValidatorContext context) {
        ElementIsNullConstraintViolation violation = new ElementIsNullConstraintViolation(context);
        if (value == null) {
            return violation.elementIsNull("Permission request must not be empty");
        }
        if (value.startTime() == null || value.expirationTime() == null) {
            return violation.elementIsNull("Permission request must have a startTime and expirationTime");
        }
        return value.startTime().isBefore(value.expirationTime());
    }
}

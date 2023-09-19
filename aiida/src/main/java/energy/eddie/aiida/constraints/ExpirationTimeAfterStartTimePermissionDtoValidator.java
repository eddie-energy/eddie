package energy.eddie.aiida.constraints;

import energy.eddie.aiida.dtos.PermissionDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.Instant;

public class ExpirationTimeAfterStartTimePermissionDtoValidator implements ConstraintValidator<ExpirationTimeAfterStartTime, PermissionDto> {
    public static boolean validate(Instant start, Instant expiration, ConstraintValidatorContext context) {
        // null check is required, because it is not guaranteed, that @NotNull annotation is validated before this one
        if (expiration == null || start == null) {
            // disable default error message and set a custom one
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("startTime and expirationTime must not be null.")
                    .addConstraintViolation();
            return false;
        }

        return expiration.isAfter(start);
    }

    @Override
    public boolean isValid(PermissionDto dto, ConstraintValidatorContext context) {
        return validate(dto.startTime(), dto.expirationTime(), context);
    }
}

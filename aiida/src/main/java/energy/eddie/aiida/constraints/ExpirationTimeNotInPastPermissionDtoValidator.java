package energy.eddie.aiida.constraints;

import energy.eddie.aiida.dtos.PermissionDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.Instant;

public class ExpirationTimeNotInPastPermissionDtoValidator implements ConstraintValidator<ExpirationTimeNotInPast, PermissionDto> {
    public static boolean validate(Instant expiration, ConstraintValidatorContext context) {
        // null check is required, because it is not guaranteed, that @NotNull annotation is validated before this one
        if (expiration == null) {
            // disable default error message and set a custom one
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("expirationTime must not be null.")
                    .addConstraintViolation();
            return false;
        }

        return expiration.isAfter(Instant.now());
    }

    @Override
    public boolean isValid(PermissionDto dto, ConstraintValidatorContext context) {
        return validate(dto.expirationTime(), context);
    }
}

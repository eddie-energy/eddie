package energy.eddie.dataneeds.validation;

import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class IsValidValidatedHistoricalDataDataNeedValidator implements ConstraintValidator<IsValidValidatedHistoricalDataDataNeed, ValidatedHistoricalDataDataNeed> {

    @Override
    public boolean isValid(ValidatedHistoricalDataDataNeed value, ConstraintValidatorContext context) {
        if ((value.maxGranularity().minutes() - value.minGranularity().minutes()) < 0) {
            context.buildConstraintViolationWithTemplate("maxGranularity must be higher or equal to minGranularity.")
                   .addConstraintViolation()
                   .disableDefaultConstraintViolation();
            return false;
        }
        return true;
    }
}

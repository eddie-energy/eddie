package energy.eddie.aiida.models.record;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RequireDataTagOrSourceKeyValidator implements ConstraintValidator<RequireDataTagOrSourceKey, AiidaRecordValue> {
    @Override
    public boolean isValid(AiidaRecordValue aiidaRecordValue, ConstraintValidatorContext context) {
        if (aiidaRecordValue == null) return true;

        boolean dataTagPresent = aiidaRecordValue.dataTag() != null;
        boolean sourceKeyPresent = aiidaRecordValue.sourceKey() != null;

        return dataTagPresent ^ sourceKeyPresent; // true if exactly one is true
    }
}

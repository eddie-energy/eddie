package energy.eddie.dataneeds.validation.aiida.schema;

import energy.eddie.dataneeds.needs.aiida.AiidaDataNeed;
import energy.eddie.dataneeds.needs.aiida.AiidaSchema;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;

public class IsValidAiidaSchemaValidator implements ConstraintValidator<IsValidAiidaSchema, AiidaDataNeed> {
    @Override
    public boolean isValid(AiidaDataNeed value, ConstraintValidatorContext context) {
        Set<AiidaSchema> schemas = value.schemas();

        return schemas != null && !schemas.isEmpty();
    }
}

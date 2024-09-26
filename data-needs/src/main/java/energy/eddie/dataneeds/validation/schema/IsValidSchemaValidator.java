package energy.eddie.dataneeds.validation.schema;

import energy.eddie.dataneeds.needs.aiida.AiidaDataNeed;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class IsValidSchemaValidator implements ConstraintValidator<IsValidSchema, AiidaDataNeed> {

    private static final Set<String> ALLOWED_SCHEMAS = new HashSet<>(Arrays.asList(Schemas.SMART_METER_P1_RAW, Schemas.SMART_METER_P1_CIM));

    @Override
    public boolean isValid(AiidaDataNeed value, ConstraintValidatorContext context) {
        Set<String> schemas = value.schemas();

        return schemas != null && !schemas.isEmpty() && ALLOWED_SCHEMAS.containsAll(schemas);
    }
}

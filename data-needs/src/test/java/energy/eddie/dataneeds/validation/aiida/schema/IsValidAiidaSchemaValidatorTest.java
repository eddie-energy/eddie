package energy.eddie.dataneeds.validation.aiida.schema;

import energy.eddie.dataneeds.needs.aiida.AiidaDataNeed;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IsValidAiidaSchemaValidatorTest {
    private final IsValidAiidaSchemaValidator validator = new IsValidAiidaSchemaValidator();

    @Mock
    private ConstraintValidatorContext mockContext;

    @Test
    void givenDataNeeds_doExpectOneOrMultipleSchemas_validationSucceeds() {
        var aiidaDataNeedWithOneSchema = mock(AiidaDataNeed.class);
        var aiidaDataNeedWithMultipleSchemas = mock(AiidaDataNeed.class);
        when(aiidaDataNeedWithOneSchema.schemas()).thenReturn(Set.of(AiidaSchema.SMART_METER_P1_RAW));
        when(aiidaDataNeedWithMultipleSchemas.schemas()).thenReturn(Set.of(AiidaSchema.SMART_METER_P1_RAW,
                                                                           AiidaSchema.SMART_METER_P1_CIM));

        assertTrue(validator.isValid(aiidaDataNeedWithOneSchema, mockContext));
        assertTrue(validator.isValid(aiidaDataNeedWithMultipleSchemas, mockContext));
    }

    @Test
    void givenDataNeeds_doExpectOneOrMultipleSchemas_validationFails() {
        var aiidaDataNeedWithNoSchema = mock(AiidaDataNeed.class);
        var aiidaDataNeedWhereSchemasNull = mock(AiidaDataNeed.class);
        when(aiidaDataNeedWithNoSchema.schemas()).thenReturn(Set.of());
        when(aiidaDataNeedWhereSchemasNull.schemas()).thenReturn(null);

        assertFalse(validator.isValid(aiidaDataNeedWithNoSchema, mockContext));
        assertFalse(validator.isValid(aiidaDataNeedWhereSchemasNull, mockContext));
    }
}

package energy.eddie.dataneeds.validation.asset;

import energy.eddie.dataneeds.needs.aiida.AiidaDataNeed;
import energy.eddie.dataneeds.needs.aiida.GenericAiidaDataNeed;
import energy.eddie.dataneeds.needs.aiida.SmartMeterAiidaDataNeed;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IsValidAiidaAssetValidatorTest {
    private final IsValidAiidaAssetValidator validator = new IsValidAiidaAssetValidator();

    @Mock
    private ConstraintValidatorContext mockContext;

    @Test
    void givenDataNeeds_doExpectConnectionAgreementPoint_validationSucceeds() {
        var genericAiidaDataNeed = mock(GenericAiidaDataNeed.class);
        var smartMeterAiidaDataNeed = mock(SmartMeterAiidaDataNeed.class);
        when(genericAiidaDataNeed.asset()).thenReturn(AiidaAsset.CONNECTION_AGREEMENT_POINT);
        when(smartMeterAiidaDataNeed.asset()).thenReturn(AiidaAsset.CONNECTION_AGREEMENT_POINT);

        assertEquals("CONNECTION-AGREEMENT-POINT", genericAiidaDataNeed.asset().toString());
        assertEquals("CONNECTION-AGREEMENT-POINT", smartMeterAiidaDataNeed.asset().toString());
        assertTrue(validator.isValid(genericAiidaDataNeed, mockContext));
        assertTrue(validator.isValid(smartMeterAiidaDataNeed, mockContext));
    }

    @Test
    void givenDataNeeds_doExpectConnectionAgreementPoint_validationFails() {
        GenericAiidaDataNeed genericAiidaDataNeed = mock(GenericAiidaDataNeed.class);
        var smartMeterAiidaDataNeed = mock(SmartMeterAiidaDataNeed.class);
        when(genericAiidaDataNeed.asset()).thenReturn(AiidaAsset.SUBMETER);
        when(smartMeterAiidaDataNeed.asset()).thenReturn(AiidaAsset.CONTROLLABLE_UNIT);


        assertEquals("SUBMETER", genericAiidaDataNeed.asset().toString());
        assertEquals("CONTROLLABLE-UNIT", smartMeterAiidaDataNeed.asset().toString());
        assertFalse(validator.isValid(genericAiidaDataNeed, mockContext));
        assertFalse(validator.isValid(smartMeterAiidaDataNeed, mockContext));
    }

    @Test
    void givenDataNeed_notGenericOrSmarmeterDataNeed_validationFails() {
        var aiidaDataNeed = mock(AiidaDataNeed.class);
        assertFalse(validator.isValid(aiidaDataNeed, mockContext));
    }
}

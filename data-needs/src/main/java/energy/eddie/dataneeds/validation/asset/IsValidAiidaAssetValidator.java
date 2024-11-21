package energy.eddie.dataneeds.validation.asset;

import energy.eddie.dataneeds.needs.aiida.AiidaDataNeed;
import energy.eddie.dataneeds.needs.aiida.GenericAiidaDataNeed;
import energy.eddie.dataneeds.needs.aiida.SmartMeterAiidaDataNeed;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class IsValidAiidaAssetValidator implements ConstraintValidator<IsValidAiidaAsset, AiidaDataNeed> {
    @Override
    public boolean isValid(AiidaDataNeed value, ConstraintValidatorContext context) {
        switch (value) {
            case GenericAiidaDataNeed genericAiidaDataNeed -> {
                return genericAiidaDataNeed.asset().equals(AiidaAsset.CONNECTION_AGREEMENT_POINT);
            }
            case SmartMeterAiidaDataNeed smartMeterAiidaDataNeed -> {
                return smartMeterAiidaDataNeed.asset().equals(AiidaAsset.CONNECTION_AGREEMENT_POINT);
            }
            default -> {
                return false;
            }
        }
    }
}

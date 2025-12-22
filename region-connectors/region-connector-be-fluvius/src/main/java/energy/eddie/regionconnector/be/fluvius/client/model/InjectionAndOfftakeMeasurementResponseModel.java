package energy.eddie.regionconnector.be.fluvius.client.model;

public interface InjectionAndOfftakeMeasurementResponseModel extends MeasurementResponseModel {
    Double injectionValue();

    ValidationState injectionValidationState();

    @Override
    default Double value() {
        return offtakeValue() - injectionValue();
    }

    @Override
    boolean isInjectionPresent();

    @Override
    default ValidationState[] validationStates() {
        return new ValidationState[]{offtakeValidationState(), injectionValidationState()};
    }
}

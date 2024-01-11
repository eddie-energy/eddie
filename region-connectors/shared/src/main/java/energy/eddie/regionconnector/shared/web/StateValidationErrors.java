package energy.eddie.regionconnector.shared.web;

import energy.eddie.api.agnostic.EddieApiError;
import energy.eddie.api.v0.process.model.validation.AttributeError;
import energy.eddie.api.v0.process.model.validation.ValidationException;

import java.util.List;

public class StateValidationErrors implements ErrorMapper {
    private final ValidationException exception;

    public StateValidationErrors(ValidationException exception) {
        this.exception = exception;
    }

    @Override
    public List<EddieApiError> asErrorsList() {
        return exception.errors()
                .stream()
                .map(this::mapAttributeErrorToMessage)
                .toList();
    }

    private EddieApiError mapAttributeErrorToMessage(AttributeError attributeError) {
        return new EddieApiError("%s: %s".formatted(attributeError.name(), attributeError.message()));
    }
}

package energy.eddie.regionconnector.shared.web;

import energy.eddie.api.v0.process.model.validation.AttributeError;
import energy.eddie.api.v0.process.model.validation.ValidationException;

import java.util.Map;
import java.util.stream.Collectors;

public class StateValidationErrors implements ErrorMapper {
    private final ValidationException exception;

    public StateValidationErrors(ValidationException exception) {
        this.exception = exception;
    }

    @Override
    public Map<String, String> asMap() {
        return exception.errors()
                .stream()
                .collect(Collectors.toMap(AttributeError::name, AttributeError::message));
    }
}

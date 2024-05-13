package energy.eddie.regionconnector.at.eda.services;

import energy.eddie.api.agnostic.process.model.validation.AttributeError;

import java.util.List;

public class EdaValidationException extends Exception {
    private final transient List<AttributeError> errors;

    public EdaValidationException(List<AttributeError> errors) {
        this.errors = errors;
    }

    public List<AttributeError> errors() {
        return errors;
    }
}

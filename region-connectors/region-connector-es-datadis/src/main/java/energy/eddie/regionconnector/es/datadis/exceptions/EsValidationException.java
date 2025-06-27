package energy.eddie.regionconnector.es.datadis.exceptions;

import energy.eddie.api.agnostic.process.model.validation.AttributeError;

public class EsValidationException extends Exception {
    @SuppressWarnings("java:S1948") // False positive
    private final AttributeError error;
    public EsValidationException(AttributeError error) {
        this.error = error;
    }

    public AttributeError error() {
        return error;
    }
}

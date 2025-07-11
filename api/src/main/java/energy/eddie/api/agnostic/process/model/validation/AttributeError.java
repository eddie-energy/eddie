package energy.eddie.api.agnostic.process.model.validation;

import java.io.Serializable;

public record AttributeError(String name, String message) implements Serializable {
}
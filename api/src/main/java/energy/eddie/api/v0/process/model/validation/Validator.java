package energy.eddie.api.v0.process.model.validation;

import java.util.List;

public interface Validator<T> {
    List<AttributeError> validate(T value);

}

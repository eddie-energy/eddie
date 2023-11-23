package energy.eddie.api.v0.process.model.validation;

import energy.eddie.api.v0.process.model.PermissionRequestState;
import energy.eddie.api.v0.process.model.StateTransitionException;

import java.util.List;
import java.util.stream.Collectors;

public class ValidationException extends StateTransitionException {

    private final transient List<AttributeError> errors;

    public ValidationException(PermissionRequestState permissionRequestState, String field, String message) {
        this(permissionRequestState, List.of(new AttributeError(field, message)));
    }

    public ValidationException(PermissionRequestState permissionRequestState, List<AttributeError> errors) {
        super(permissionRequestState);
        this.errors = errors;
    }

    @Override
    public String getMessage() {
        String errorStr = errors.stream()
                .map(error -> "%s: %s".formatted(error.name(), error.message()))
                .collect(Collectors.joining("\n"));
        return "Validation errors in %s for following fields: %s".formatted(this.permissionRequestStateClass, errorStr);
    }

    public List<AttributeError> errors() {
        return errors;
    }
}

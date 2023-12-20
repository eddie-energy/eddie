package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.TimeframedPermissionRequest;
import energy.eddie.api.v0.process.model.states.MalformedPermissionRequestState;
import energy.eddie.api.v0.process.model.validation.AttributeError;

import java.util.List;


public class FrEnedisMalformedState
        extends ContextualizedPermissionRequestState<TimeframedPermissionRequest>
        implements MalformedPermissionRequestState {
    private final List<AttributeError> errors;

    public FrEnedisMalformedState(TimeframedPermissionRequest permissionRequest, List<AttributeError> errors) {
        super(permissionRequest);
        this.errors = errors;
    }

    @Override
    public String toString() {
        return "MalformedState{" +
                "errors=" + errors +
                '}';
    }
}
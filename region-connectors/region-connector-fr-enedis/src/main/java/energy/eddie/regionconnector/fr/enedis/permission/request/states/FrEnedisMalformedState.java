package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.TimeframedPermissionRequest;
import energy.eddie.api.v0.process.model.states.MalformedPermissionRequestState;
import io.javalin.validation.ValidationError;

import java.util.List;
import java.util.Map;

public class FrEnedisMalformedState
        extends ContextualizedPermissionRequestState<TimeframedPermissionRequest>
        implements MalformedPermissionRequestState {
    private final Map<String, List<ValidationError<?>>> errors;

    public FrEnedisMalformedState(TimeframedPermissionRequest permissionRequest, Map<String, List<ValidationError<?>>> errors) {
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
package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.MalformedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;

import java.util.List;


public class FrEnedisMalformedState
        extends ContextualizedPermissionRequestState<FrEnedisPermissionRequest>
        implements MalformedPermissionRequestState {
    private final List<AttributeError> errors;

    public FrEnedisMalformedState(FrEnedisPermissionRequest permissionRequest, List<AttributeError> errors) {
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
package energy.eddie.regionconnector.es.datadis.permission.request.state;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.states.MalformedPermissionRequestState;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import io.javalin.validation.ValidationError;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The state of a permission request if it was malformed could not be validated.
 */
public class MalformedState extends ContextualizedPermissionRequestState<EsPermissionRequest> implements MalformedPermissionRequestState {
    private final Map<String, List<ValidationError<?>>> errors;

    public MalformedState(EsPermissionRequest permissionRequest, Map<String, List<ValidationError<?>>> errors) {
        super(permissionRequest);
        this.errors = errors;
    }


    @Override
    public String toString() {
        return "MalformedState{" +
                "errors=" + errors.keySet().stream().map(key -> key + "=" + errors.get(key))
                .collect(Collectors.joining(", ", "{", "}")) +
                ", permissionRequest=" + permissionRequest +
                '}';
    }
}

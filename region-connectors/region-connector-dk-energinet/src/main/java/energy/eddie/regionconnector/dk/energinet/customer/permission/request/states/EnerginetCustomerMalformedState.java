package energy.eddie.regionconnector.dk.energinet.customer.permission.request.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.states.MalformedPermissionRequestState;
import energy.eddie.regionconnector.dk.energinet.customer.api.DkEnerginetCustomerPermissionRequest;
import io.javalin.validation.ValidationError;

import java.util.List;
import java.util.Map;

public class EnerginetCustomerMalformedState extends ContextualizedPermissionRequestState<DkEnerginetCustomerPermissionRequest>
        implements MalformedPermissionRequestState {
    private final Map<String, List<ValidationError<?>>> errors;

    public EnerginetCustomerMalformedState(DkEnerginetCustomerPermissionRequest permissionRequest, Map<String, List<ValidationError<?>>> errors) {
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

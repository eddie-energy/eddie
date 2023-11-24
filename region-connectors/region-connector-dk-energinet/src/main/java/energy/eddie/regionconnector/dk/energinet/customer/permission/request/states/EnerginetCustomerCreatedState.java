package energy.eddie.regionconnector.dk.energinet.customer.permission.request.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.states.CreatedPermissionRequestState;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.EnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequest;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public class EnerginetCustomerCreatedState extends ContextualizedPermissionRequestState<DkEnerginetCustomerPermissionRequest>
        implements CreatedPermissionRequestState {
    private final EnerginetConfiguration configuration;

    public EnerginetCustomerCreatedState(EnerginetCustomerPermissionRequest request, EnerginetConfiguration configuration) {
        super(request);
        this.configuration = configuration;
    }

    @Override
    public void validate() {
        if (permissionRequest.connectionId() == null || permissionRequest.connectionId().isBlank())
            validationFailed("connectionId must not be blank");

        if (permissionRequest.refreshToken() == null || permissionRequest.refreshToken().isBlank())
            validationFailed("refreshToken must not be blank");

        if (permissionRequest.meteringPoint() == null || permissionRequest.meteringPoint().isBlank())
            validationFailed("meteringPoint must not be blank");

        if (permissionRequest.start() == null)
            validationFailed("start must not be null");

        if (permissionRequest.end() == null)
            validationFailed("end must not be null");

        var now = ZonedDateTime.now(ZoneId.of("Europe/Copenhagen"));

        if (permissionRequest.start().isBefore(now.minusMonths(24))) {
            validationFailed("start must not be older than 24 months");
        }

        if (permissionRequest.end().isBefore(permissionRequest.start())) {
            validationFailed("end must be after start");
        }

        if (!permissionRequest.end().isBefore(now.minusDays(1))) {
            validationFailed("end must be in the past");
        }

        permissionRequest.changeState(new EnerginetCustomerValidatedState(permissionRequest, configuration));
    }

    private void validationFailed(String message) {
        // TODO change state permissionRequest.changeState(new EnerginetCustomerMalformedState(permissionRequest, message));
        // TODO throw appropriate error     throw new ValidationException(this, List.of(message));
    }
}

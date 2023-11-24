package energy.eddie.regionconnector.dk.energinet.customer.permission.request.states;

import energy.eddie.api.v0.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.v0.process.model.states.CreatedPermissionRequestState;
import energy.eddie.api.v0.process.model.validation.AttributeError;
import energy.eddie.api.v0.process.model.validation.ValidationException;
import energy.eddie.api.v0.process.model.validation.Validator;
import energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnector;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.client.EnerginetCustomerApiClient;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.EnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.validation.CompletelyInThePastValidator;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.validation.NotOlderThanValidator;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.validation.StartIsBeforeOrEqualEndValidator;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

public class EnerginetCustomerCreatedState
        extends ContextualizedPermissionRequestState<DkEnerginetCustomerPermissionRequest>
        implements CreatedPermissionRequestState {
    private static final Set<Validator<DkEnerginetCustomerPermissionRequest>> VALIDATORS = Set.of(
            new NotOlderThanValidator(ChronoUnit.MONTHS, EnerginetRegionConnector.MAXIMUM_MONTHS_IN_THE_PAST),
            new CompletelyInThePastValidator(),
            new StartIsBeforeOrEqualEndValidator()
    );
    private final EnerginetConfiguration configuration;

    public EnerginetCustomerCreatedState(EnerginetCustomerPermissionRequest request, EnerginetConfiguration configuration) {
        super(request);
        this.configuration = configuration;
    }

    @Override
    public void validate() throws ValidationException {
        validateAttributes();

        var apiClient = new EnerginetCustomerApiClient(configuration);
        permissionRequest.changeState(new EnerginetCustomerValidatedState(permissionRequest, apiClient));
    }

    private void validateAttributes() throws ValidationException {
        List<AttributeError> errors = VALIDATORS.stream()
                .flatMap(validator -> validator.validate(this.permissionRequest).stream())
                .toList();

        if (!errors.isEmpty()) {
            ValidationException exception = new ValidationException(this, errors);
            changeToMalformedState(exception);
            throw exception;
        }
    }

    private void changeToMalformedState(Exception e) {
        permissionRequest.changeState(new EnerginetCustomerMalformedState(permissionRequest, e));
    }
}

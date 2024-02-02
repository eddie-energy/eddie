package energy.eddie.regionconnector.dk.energinet.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.CreatedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.agnostic.process.model.validation.ValidationException;
import energy.eddie.api.agnostic.process.model.validation.Validator;
import energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnector;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.permission.request.validation.NotOlderThanValidator;
import energy.eddie.regionconnector.shared.permission.requests.validation.CompletelyInThePastOrInTheFutureValidator;
import energy.eddie.regionconnector.shared.permission.requests.validation.StartIsBeforeOrEqualEndValidator;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

public class EnerginetCustomerCreatedState
        extends ContextualizedPermissionRequestState<DkEnerginetCustomerPermissionRequest>
        implements CreatedPermissionRequestState {
    private static final Set<Validator<DkEnerginetCustomerPermissionRequest>> VALIDATORS = Set.of(
            new NotOlderThanValidator(ChronoUnit.MONTHS, EnerginetRegionConnector.MAXIMUM_MONTHS_IN_THE_PAST),
            new CompletelyInThePastOrInTheFutureValidator<>(),
            new StartIsBeforeOrEqualEndValidator<>()
    );

    public EnerginetCustomerCreatedState(EnerginetCustomerPermissionRequest request) {
        super(request);
    }

    @Override
    public void validate() throws ValidationException {
        validateAttributes();
        permissionRequest.changeState(new EnerginetCustomerValidatedState(permissionRequest));
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
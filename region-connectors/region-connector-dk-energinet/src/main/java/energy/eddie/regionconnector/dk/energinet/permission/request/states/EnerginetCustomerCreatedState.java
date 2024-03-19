package energy.eddie.regionconnector.dk.energinet.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.CreatedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.agnostic.process.model.validation.ValidationException;
import energy.eddie.api.agnostic.process.model.validation.Validator;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.energinet.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.permission.request.validation.NotOlderThanValidator;
import energy.eddie.regionconnector.shared.permission.requests.validation.CompletelyInThePastOrInTheFutureValidator;
import energy.eddie.regionconnector.shared.permission.requests.validation.StartIsBeforeOrEqualEndValidator;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata.MAXIMUM_MONTHS_IN_THE_PAST;

public class EnerginetCustomerCreatedState
        extends ContextualizedPermissionRequestState<DkEnerginetCustomerPermissionRequest>
        implements CreatedPermissionRequestState {
    private static final Set<Validator<DkEnerginetCustomerPermissionRequest>> VALIDATORS = Set.of(
            new NotOlderThanValidator(ChronoUnit.MONTHS, MAXIMUM_MONTHS_IN_THE_PAST),
            new CompletelyInThePastOrInTheFutureValidator<>(),
            new StartIsBeforeOrEqualEndValidator<>()
    );
    private final StateBuilderFactory factory;

    public EnerginetCustomerCreatedState(DkEnerginetCustomerPermissionRequest permissionRequest, StateBuilderFactory factory) {
        super(permissionRequest);
        this.factory = factory;
    }

    @Override
    public void validate() throws ValidationException {
        validateAttributes();
        permissionRequest.changeState(
                factory.create(permissionRequest, PermissionProcessStatus.VALIDATED).build()
        );
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
        permissionRequest.changeState(
                factory.create(permissionRequest, PermissionProcessStatus.MALFORMED)
                        .withCause(e)
                        .build()
        );
    }
}

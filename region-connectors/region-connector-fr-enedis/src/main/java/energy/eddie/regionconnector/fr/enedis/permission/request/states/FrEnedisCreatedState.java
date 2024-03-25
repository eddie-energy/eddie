package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.agnostic.process.model.states.CreatedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.agnostic.process.model.validation.ValidationException;
import energy.eddie.api.agnostic.process.model.validation.Validator;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.validation.NotFurtherThanValidator;
import energy.eddie.regionconnector.shared.permission.requests.validation.StartIsBeforeOrEqualEndValidator;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata.MAXIMUM_MONTHS_IN_THE_PAST;

public class FrEnedisCreatedState
        extends ContextualizedPermissionRequestState<FrEnedisPermissionRequest>
        implements CreatedPermissionRequestState {
    private static final Set<Validator<PermissionRequest>> VALIDATORS = Set.of(
            new StartIsBeforeOrEqualEndValidator<>(),
            new NotFurtherThanValidator(ChronoUnit.MONTHS, MAXIMUM_MONTHS_IN_THE_PAST)
    );
    private final StateBuilderFactory factory;

    public FrEnedisCreatedState(FrEnedisPermissionRequest permissionRequest, StateBuilderFactory factory) {
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
                                                .flatMap(val -> val.validate(permissionRequest).stream())
                                                .toList();

        if (!errors.isEmpty()) {
            ValidationException exception = new ValidationException(this, errors);
            permissionRequest.changeState(
                    factory.create(permissionRequest, PermissionProcessStatus.MALFORMED)
                           .withCause(exception)
                           .build()
            );
            throw exception;
        }
    }
}

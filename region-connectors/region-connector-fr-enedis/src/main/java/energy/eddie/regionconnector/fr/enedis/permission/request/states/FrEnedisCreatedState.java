package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import energy.eddie.api.agnostic.process.model.states.CreatedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.agnostic.process.model.validation.ValidationException;
import energy.eddie.api.agnostic.process.model.validation.Validator;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.validation.NotFurtherThanValidator;
import energy.eddie.regionconnector.shared.permission.requests.validation.StartIsBeforeOrEqualEndValidator;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

public class FrEnedisCreatedState
        extends ContextualizedPermissionRequestState<FrEnedisPermissionRequest>
        implements CreatedPermissionRequestState {
    private static final Set<Validator<TimeframedPermissionRequest>> VALIDATORS = Set.of(
            new StartIsBeforeOrEqualEndValidator<>(),
            new NotFurtherThanValidator(ChronoUnit.YEARS, 3)
    );

    public FrEnedisCreatedState(FrEnedisPermissionRequest permissionRequest) {
        super(permissionRequest);
    }

    @Override
    public void validate() throws ValidationException {
        validateAttributes();
        permissionRequest.changeState(new FrEnedisValidatedState(permissionRequest));
    }

    private void validateAttributes() throws ValidationException {
        List<AttributeError> errors = VALIDATORS.stream()
                .flatMap(val -> val.validate(permissionRequest).stream())
                .toList();
        if (!errors.isEmpty()) {
            permissionRequest.changeState(new FrEnedisMalformedState(permissionRequest, errors));
            throw new ValidationException(this, errors);
        }
    }
}
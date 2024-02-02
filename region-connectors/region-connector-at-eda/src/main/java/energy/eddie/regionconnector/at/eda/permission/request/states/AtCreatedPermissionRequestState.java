package energy.eddie.regionconnector.at.eda.permission.request.states;

import at.ebutilities.schemata.customerconsent.cmrequest._01p10.CMRequest;
import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.CreatedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.agnostic.process.model.validation.ValidationException;
import energy.eddie.api.agnostic.process.model.validation.Validator;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.EdaRegionConnector;
import energy.eddie.regionconnector.at.eda.permission.request.validation.NotOlderThanValidator;
import energy.eddie.regionconnector.at.eda.permission.request.validation.StartIsBeforeOrEqualEndValidator;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import energy.eddie.regionconnector.at.eda.requests.InvalidDsoIdException;
import energy.eddie.regionconnector.shared.permission.requests.validation.CompletelyInThePastOrInTheFutureValidator;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

/**
 * The first state a PermissionRequest is in.
 * After it is constructed the PermissionRequest is in a created state.
 */
public class AtCreatedPermissionRequestState
        extends ContextualizedPermissionRequestState<AtPermissionRequest>
        implements CreatedPermissionRequestState {
    private static final Set<Validator<AtPermissionRequest>> VALIDATORS = Set.of(
            new NotOlderThanValidator(ChronoUnit.MONTHS, EdaRegionConnector.MAXIMUM_MONTHS_IN_THE_PAST),
            new CompletelyInThePastOrInTheFutureValidator<>(),
            new StartIsBeforeOrEqualEndValidator()
    );
    private final CCMORequest ccmoRequest;
    private final EdaAdapter edaAdapter;

    public AtCreatedPermissionRequestState(AtPermissionRequest permissionRequest, CCMORequest ccmoRequest, EdaAdapter edaAdapter) {
        super(permissionRequest);
        this.ccmoRequest = ccmoRequest;
        this.edaAdapter = edaAdapter;
    }

    @Override
    public void validate() throws ValidationException {
        validateAttributes();
        try {
            CMRequest cmRequest = ccmoRequest.toCMRequest();
            permissionRequest.changeState(new AtValidatedPermissionRequestState(permissionRequest, cmRequest, edaAdapter));
        } catch (InvalidDsoIdException e) {
            changeToMalformedState(e);
            throw new ValidationException(this, "dsoId", e.getMessage() == null ? "" : e.getMessage());
        }
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
        permissionRequest.changeState(new AtMalformedPermissionRequestState(permissionRequest, e));
    }

}
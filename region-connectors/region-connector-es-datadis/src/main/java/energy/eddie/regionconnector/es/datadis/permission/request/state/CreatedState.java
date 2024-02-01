package energy.eddie.regionconnector.es.datadis.permission.request.state;


import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.CreatedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.agnostic.process.model.validation.ValidationException;
import energy.eddie.api.agnostic.process.model.validation.Validator;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.validation.NotOlderThanValidator;
import energy.eddie.regionconnector.es.datadis.permission.request.validation.StartIsBeforeEndValidator;
import energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

public class CreatedState extends ContextualizedPermissionRequestState<EsPermissionRequest> implements CreatedPermissionRequestState {
    private static final Set<Validator<EsPermissionRequest>> VALIDATORS = Set.of(
            new NotOlderThanValidator(ChronoUnit.MONTHS, DatadisSpecificConstants.MAXIMUM_MONTHS_IN_THE_PAST),
            new StartIsBeforeEndValidator()
    );
    private final AuthorizationApi authorizationApi;

    public CreatedState(EsPermissionRequest permissionRequest, AuthorizationApi authorizationApi) {
        super(permissionRequest);
        this.authorizationApi = authorizationApi;
    }

    @Override
    public void validate() throws ValidationException {
        validateAttributes();

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                permissionRequest.permissionStart().toLocalDate(),
                permissionRequest.permissionEnd().toLocalDate(),
                permissionRequest.nif(),
                List.of(permissionRequest.meteringPointId())
        );

        permissionRequest.changeState(new ValidatedState(permissionRequest, authorizationRequest, authorizationApi));
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
        permissionRequest.changeState(new MalformedState(permissionRequest, e));
    }
}
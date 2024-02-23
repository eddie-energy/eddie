package energy.eddie.regionconnector.es.datadis.permission.request.state;


import energy.eddie.api.agnostic.process.model.ContextualizedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.CreatedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.agnostic.process.model.validation.ValidationException;
import energy.eddie.api.agnostic.process.model.validation.Validator;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.permission.request.StateBuilderFactory;
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
    private final StateBuilderFactory factory;

    public CreatedState(EsPermissionRequest permissionRequest,
                        StateBuilderFactory factory
    ) {
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
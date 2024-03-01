package energy.eddie.regionconnector.at.eda.services;

import energy.eddie.api.agnostic.process.model.states.MalformedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.agnostic.process.model.validation.ValidationException;
import energy.eddie.api.agnostic.process.model.validation.Validator;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.eda.EdaRegionConnector;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.permission.request.EdaDataSourceInformation;
import energy.eddie.regionconnector.at.eda.permission.request.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.at.eda.permission.request.events.CreatedEvent;
import energy.eddie.regionconnector.at.eda.permission.request.events.MalformedEvent;
import energy.eddie.regionconnector.at.eda.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.at.eda.permission.request.validation.CompletelyInThePastOrInTheFutureEventValidator;
import energy.eddie.regionconnector.at.eda.permission.request.validation.MeteringPointMatchesDsoIdValidator;
import energy.eddie.regionconnector.at.eda.permission.request.validation.NotOlderThanValidator;
import energy.eddie.regionconnector.at.eda.permission.request.validation.StartIsBeforeOrEqualEndValidator;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import energy.eddie.regionconnector.at.eda.requests.CCMOTimeFrame;
import energy.eddie.regionconnector.at.eda.requests.DsoIdAndMeteringPoint;
import energy.eddie.regionconnector.at.eda.requests.RequestDataType;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.TRANSMISSION_CYCLE;
import static energy.eddie.regionconnector.at.eda.utils.DateTimeConstants.AT_ZONE_ID;

@Component
public class PermissionRequestCreationAndValidationService {
    private static final Set<Validator<CreatedEvent>> VALIDATORS = Set.of(
            new NotOlderThanValidator(ChronoUnit.MONTHS, EdaRegionConnector.MAXIMUM_MONTHS_IN_THE_PAST),
            new CompletelyInThePastOrInTheFutureEventValidator(),
            new StartIsBeforeOrEqualEndValidator(),
            new MeteringPointMatchesDsoIdValidator()
    );

    private final AtConfiguration configuration;
    private final Outbox outbox;

    public PermissionRequestCreationAndValidationService(
            AtConfiguration configuration, Outbox outbox
    ) {
        this.configuration = configuration;
        this.outbox = outbox;
    }

    private static List<AttributeError> validateAttributes(CreatedEvent permissionEvent) {
        return VALIDATORS.stream()
                .flatMap(validator -> validator.validate(permissionEvent).stream())
                .toList();
    }

    /**
     * Creates and validates a permission request.
     * This will emit a <code>CreatedEvent</code>, and a <code>ValidatedEvent</code> or a <code>MalformedEvent</code>.
     *
     * @param permissionRequest the DTO that is the base for the created and validated permission request.
     * @return a DTO with the id of the created and validated permission request
     * @throws ValidationException if the permission request is invalid, a <code>ValidationException</code> is thrown containing all the erroneous fields and a description for each one.
     */
    public CreatedPermissionRequest createAndValidatePermissionRequest(
            PermissionRequestForCreation permissionRequest) throws ValidationException {
        ZonedDateTime created = ZonedDateTime.now(AT_ZONE_ID);
        CCMORequest ccmoRequest = new CCMORequest(
                new DsoIdAndMeteringPoint(permissionRequest.dsoId(), permissionRequest.meteringPointId()),
                new CCMOTimeFrame(permissionRequest.start(), permissionRequest.end()),
                configuration,
                RequestDataType.METERING_DATA,
                permissionRequest.granularity(),
                TRANSMISSION_CYCLE,
                created
        );
        String permissionId = UUID.randomUUID().toString();
        CreatedEvent event = new CreatedEvent(
                permissionId,
                permissionRequest.connectionId(),
                permissionRequest.dataNeedId(),
                new EdaDataSourceInformation(permissionRequest.dsoId()),
                created,
                permissionRequest.start(),
                permissionRequest.end(),
                permissionRequest.meteringPointId(),
                permissionRequest.granularity(),
                ccmoRequest.cmRequestId(),
                ccmoRequest.messageId()
        );
        outbox.commit(event);
        var errors = validateAttributes(event);
        if (errors.isEmpty()) {
            outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.VALIDATED));
            return new CreatedPermissionRequest(permissionId, ccmoRequest.cmRequestId());
        } else {
            outbox.commit(new MalformedEvent(permissionId, errors));
            throw new ValidationException(new MalformedPermissionRequestState() {
            }, errors);
        }
    }
}
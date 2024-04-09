package energy.eddie.regionconnector.at.eda.services;

import energy.eddie.api.agnostic.process.model.states.MalformedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.agnostic.process.model.validation.ValidationException;
import energy.eddie.api.agnostic.process.model.validation.Validator;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata;
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
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.*;

@Component
public class PermissionRequestCreationAndValidationService {
    private static final Set<Validator<CreatedEvent>> VALIDATORS = Set.of(
            new NotOlderThanValidator(ChronoUnit.MONTHS, MAXIMUM_MONTHS_IN_THE_PAST),
            new CompletelyInThePastOrInTheFutureEventValidator(),
            new StartIsBeforeOrEqualEndValidator(),
            new MeteringPointMatchesDsoIdValidator()
    );

    private final AtConfiguration configuration;
    private final Outbox outbox;
    private final DataNeedsService dataNeedsService;

    public PermissionRequestCreationAndValidationService(
            AtConfiguration configuration,
            Outbox outbox,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")  // defined in core
            DataNeedsService dataNeedsService
    ) {
        this.configuration = configuration;
        this.outbox = outbox;
        this.dataNeedsService = dataNeedsService;
    }

    /**
     * Creates and validates a permission request. This will emit a <code>CreatedEvent</code>, and a
     * <code>ValidatedEvent</code> or a <code>MalformedEvent</code>.
     *
     * @param permissionRequest the DTO that is the base for the created and validated permission request.
     * @return a DTO with the id of the created and validated permission request
     * @throws ValidationException if the permission request is invalid, a <code>ValidationException</code> is thrown
     *                             containing all the erroneous fields and a description for each one.
     */
    public CreatedPermissionRequest createAndValidatePermissionRequest(
            PermissionRequestForCreation permissionRequest
    ) throws ValidationException, DataNeedNotFoundException, UnsupportedDataNeedException {
        var referenceDate = LocalDate.now(AT_ZONE_ID);
        var wrapper = dataNeedsService.findDataNeedAndCalculateStartAndEnd(permissionRequest.dataNeedId(),
                                                                           referenceDate,
                                                                           PERIOD_EARLIEST_START,
                                                                           PERIOD_LATEST_END);

        if (!(wrapper.timeframedDataNeed() instanceof ValidatedHistoricalDataDataNeed vhdDataNeed)) {
            throw new UnsupportedDataNeedException(EdaRegionConnectorMetadata.REGION_CONNECTOR_ID,
                                                   wrapper.timeframedDataNeed().id(),
                                                   "This region connector only supports validated historical data data needs.");
        }
        var granularity = switch (vhdDataNeed.minGranularity()) {
            case PT15M -> AllowedGranularity.PT15M;
            case P1D -> AllowedGranularity.P1D;
            default -> throw new UnsupportedDataNeedException(EdaRegionConnectorMetadata.REGION_CONNECTOR_ID,
                                                              vhdDataNeed.id(),
                                                              "Unsupported granularity: '" + vhdDataNeed.minGranularity() + "'");
        };

        ZonedDateTime created = ZonedDateTime.now(AT_ZONE_ID);
        CCMORequest ccmoRequest = new CCMORequest(
                new DsoIdAndMeteringPoint(permissionRequest.dsoId(), permissionRequest.meteringPointId()),
                new CCMOTimeFrame(wrapper.calculatedStart(), wrapper.calculatedEnd()),
                RequestDataType.METERING_DATA,
                granularity,
                TRANSMISSION_CYCLE,
                configuration,
                created
        );
        String permissionId = UUID.randomUUID().toString();
        CreatedEvent event = new CreatedEvent(
                permissionId,
                permissionRequest.connectionId(),
                permissionRequest.dataNeedId(),
                new EdaDataSourceInformation(permissionRequest.dsoId()),
                created,
                wrapper.calculatedStart(),
                wrapper.calculatedEnd(),
                permissionRequest.meteringPointId(),
                granularity,
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

    private static List<AttributeError> validateAttributes(CreatedEvent permissionEvent) {
        return VALIDATORS.stream()
                         .flatMap(validator -> validator.validate(permissionEvent).stream())
                         .toList();
    }
}

package energy.eddie.regionconnector.at.eda.services;

import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.agnostic.process.model.validation.Validator;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.dataneeds.utils.TimeframedDataNeedUtils;
import energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.permission.request.EdaDataSourceInformation;
import energy.eddie.regionconnector.at.eda.permission.request.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.at.eda.permission.request.events.CreatedEvent;
import energy.eddie.regionconnector.at.eda.permission.request.events.MalformedEvent;
import energy.eddie.regionconnector.at.eda.permission.request.events.ValidatedEvent;
import energy.eddie.regionconnector.at.eda.permission.request.validation.MeteringPointMatchesDsoIdValidator;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import energy.eddie.regionconnector.at.eda.requests.CCMOTimeFrame;
import energy.eddie.regionconnector.at.eda.requests.DsoIdAndMeteringPoint;
import energy.eddie.regionconnector.at.eda.requests.RequestDataType;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.validation.GranularityChoice;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.*;

@Component
public class PermissionRequestCreationAndValidationService {
    private static final Set<Validator<CreatedEvent>> VALIDATORS = Set.of(
            new MeteringPointMatchesDsoIdValidator()
    );
    private final GranularityChoice granularityChoice = new GranularityChoice(SUPPORTED_GRANULARITIES);
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
     */
    public CreatedPermissionRequest createAndValidatePermissionRequest(
            PermissionRequestForCreation permissionRequest
    ) throws DataNeedNotFoundException, UnsupportedDataNeedException, EdaValidationException {
        var referenceDate = LocalDate.now(AT_ZONE_ID);
        var dataNeed = dataNeedsService.findById(permissionRequest.dataNeedId())
                                       .orElseThrow(() -> new DataNeedNotFoundException(permissionRequest.dataNeedId()));

        CCMORequest ccmoRequest = switch (dataNeed) {
            case ValidatedHistoricalDataDataNeed vhdDataNeed -> ccmoRequestForVHD(
                    permissionRequest,
                    vhdDataNeed,
                    referenceDate
            );
            case AccountingPointDataNeed ignored -> ccmoRequestForAccountingPoint(permissionRequest, referenceDate);
            default -> throw new UnsupportedDataNeedException(
                    EdaRegionConnectorMetadata.REGION_CONNECTOR_ID,
                    dataNeed.id(),
                    "Unsupported data need type: " + dataNeed.getClass().getSimpleName()
            );
        };

        ZonedDateTime created = ZonedDateTime.now(AT_ZONE_ID);
        String permissionId = UUID.randomUUID().toString();
        CreatedEvent event = new CreatedEvent(
                permissionId,
                permissionRequest.connectionId(),
                permissionRequest.dataNeedId(),
                new EdaDataSourceInformation(permissionRequest.dsoId()),
                created,
                ccmoRequest.start(),
                ccmoRequest.end().orElse(ccmoRequest.start()),
                permissionRequest.meteringPointId(),
                ccmoRequest.granularity(),
                ccmoRequest.cmRequestId(),
                ccmoRequest.messageId()
        );
        outbox.commit(event);
        var errors = validateAttributes(event);
        if (errors.isEmpty()) {
            outbox.commit(new ValidatedEvent(permissionId, ccmoRequest));
            return new CreatedPermissionRequest(permissionId, ccmoRequest.cmRequestId());
        } else {
            outbox.commit(new MalformedEvent(permissionId, errors));
            throw new EdaValidationException(errors);
        }
    }

    private CCMORequest ccmoRequestForVHD(
            PermissionRequestForCreation permissionRequestForCreation,
            ValidatedHistoricalDataDataNeed vhdDataNeed,
            LocalDate referenceDate
    ) throws UnsupportedDataNeedException {
        var wrapper = TimeframedDataNeedUtils.calculateRelativeStartAndEnd(vhdDataNeed,
                                                                           referenceDate,
                                                                           PERIOD_EARLIEST_START,
                                                                           PERIOD_LATEST_END);

        var granularity = switch (granularityChoice.find(vhdDataNeed.minGranularity(),
                                                         vhdDataNeed.maxGranularity())) {
            case PT15M -> AllowedGranularity.PT15M;
            case P1D -> AllowedGranularity.P1D;
            case null, default -> throw new UnsupportedDataNeedException(EdaRegionConnectorMetadata.REGION_CONNECTOR_ID,
                                                                         vhdDataNeed.id(),
                                                              "Unsupported granularity: '" + vhdDataNeed.minGranularity() + "'");
        };
        return new CCMORequest(
                new DsoIdAndMeteringPoint(
                        permissionRequestForCreation.dsoId(),
                        permissionRequestForCreation.meteringPointId()
                ),
                new CCMOTimeFrame(
                        wrapper.calculatedStart(),
                        wrapper.calculatedEnd()
                ),
                RequestDataType.METERING_DATA,
                granularity,
                TRANSMISSION_CYCLE,
                configuration,
                ZonedDateTime.now(AT_ZONE_ID)
        );
    }

    private CCMORequest ccmoRequestForAccountingPoint(
            PermissionRequestForCreation permissionRequestForCreation,
            LocalDate referenceDate
    ) {
        return new CCMORequest(
                new DsoIdAndMeteringPoint(
                        permissionRequestForCreation.dsoId(),
                        permissionRequestForCreation.meteringPointId()
                ),
                new CCMOTimeFrame(
                        referenceDate,
                        null
                ),
                RequestDataType.MASTER_DATA,
                AllowedGranularity.P1D,
                TRANSMISSION_CYCLE,
                configuration,
                ZonedDateTime.now(AT_ZONE_ID)
        );
    }

    private static List<AttributeError> validateAttributes(CreatedEvent permissionEvent) {
        return VALIDATORS.stream()
                         .flatMap(validator -> validator.validate(permissionEvent).stream())
                         .toList();
    }
}

package energy.eddie.regionconnector.cds.services;

import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.cds.client.Scopes;
import energy.eddie.regionconnector.cds.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.cds.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.cds.exceptions.UnknownPermissionAdministratorException;
import energy.eddie.regionconnector.cds.permission.events.*;
import energy.eddie.regionconnector.cds.persistence.CdsServerRepository;
import energy.eddie.regionconnector.cds.services.oauth.OAuthService;
import energy.eddie.regionconnector.cds.services.oauth.par.ErrorParResponse;
import energy.eddie.regionconnector.cds.services.oauth.par.SuccessfulParResponse;
import energy.eddie.regionconnector.cds.services.oauth.par.UnableToSendPar;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static energy.eddie.regionconnector.cds.CdsRegionConnectorMetadata.REGION_CONNECTOR_ID;

@Service
public class PermissionRequestCreationService {
    public static final String AP_UNSUPPORTED_MESSAGE = "Accounting point data need not supported";
    public static final String DATA_NEED_FIELD = "dataNeedId";
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestCreationService.class);
    private final CdsServerRepository cdsServerRepository;
    private final Outbox outbox;
    private final DataNeedCalculationService<DataNeed> calculationService;
    private final OAuthService oAuthService;

    public PermissionRequestCreationService(
            CdsServerRepository cdsServerRepository, Outbox outbox,
            DataNeedCalculationService<DataNeed> calculationService, OAuthService oAuthService
    ) {
        this.cdsServerRepository = cdsServerRepository;
        this.outbox = outbox;
        this.calculationService = calculationService;
        this.oAuthService = oAuthService;
    }

    public CreatedPermissionRequest createPermissionRequest(PermissionRequestForCreation creation) throws UnknownPermissionAdministratorException, UnsupportedDataNeedException, DataNeedNotFoundException {
        var permissionId = UUID.randomUUID().toString();
        var cdsServerId = creation.cdsId();
        LOGGER.info("Creating new permission request {} for {} cds server", permissionId, cdsServerId);
        var dataNeedId = creation.dataNeedId();
        var createdEvent = new CreatedEvent(permissionId,
                                            creation.connectionId(),
                                            dataNeedId,
                                            cdsServerId);
        outbox.commit(createdEvent);
        var cdsServer = cdsServerRepository.findById(cdsServerId);
        if (cdsServer.isEmpty()) {
            LOGGER.info("Malformed permission request {} for unknown CDS server {}", permissionId, cdsServerId);
            outbox.commit(new MalformedEvent(permissionId,
                                             new AttributeError("cdsId", "Unknown permission administrator")));
            throw new UnknownPermissionAdministratorException(cdsServerId);
        }
        var calc = calculationService.calculate(dataNeedId, createdEvent.eventCreated());
        var dataNeedCalc = switch (calc) {
            case DataNeedNotFoundResult ignored -> {
                LOGGER.info("Data need {} not found", dataNeedId);
                outbox.commit(new MalformedEvent(permissionId,
                                                 new AttributeError(DATA_NEED_FIELD, "Data need not found")));
                throw new DataNeedNotFoundException(dataNeedId);
            }
            case DataNeedNotSupportedResult(String message) -> {
                LOGGER.info("Data need {} not supported '{}'", dataNeedId, message);
                throw unsupportedDataNeed(permissionId, message, dataNeedId);
            }
            case AccountingPointDataNeedResult ignored -> {
                LOGGER.info("Permission request {} malformed because accounting point data need {} is not supported",
                            permissionId,
                            dataNeedId);
                throw unsupportedDataNeed(permissionId, AP_UNSUPPORTED_MESSAGE, dataNeedId);
            }
            case ValidatedHistoricalDataDataNeedResult vhdResult -> vhdResult;
        };
        outbox.commit(new ValidatedEvent(permissionId,
                                         dataNeedCalc.granularities().getFirst(),
                                         dataNeedCalc.energyTimeframe().start(),
                                         dataNeedCalc.energyTimeframe().end()));
        var res= oAuthService.pushAuthorization(cdsServer.get(), List.of(Scopes.USAGE_DETAILED_SCOPE));
        switch (res) {
            case ErrorParResponse(String code) -> {
                LOGGER.info("Got error when requesting PAR '{}' for permission request {}", code, permissionId);
                outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.UNABLE_TO_SEND));
                return new CreatedPermissionRequest(permissionId, null);
            }
            case UnableToSendPar ignored -> {
                LOGGER.info("Was not able to send permission request {}", permissionId);
                outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.UNABLE_TO_SEND));
                return new CreatedPermissionRequest(permissionId, null);
            }
            case SuccessfulParResponse(URI redirectUri, ZonedDateTime expiresAt, String state) -> {
                outbox.commit(new SentToPaEvent(permissionId, expiresAt, state));
                return new CreatedPermissionRequest(permissionId, redirectUri);
            }
        }
    }

    private UnsupportedDataNeedException unsupportedDataNeed(String permissionId, String message, String dataNeedId) {
        outbox.commit(new MalformedEvent(permissionId, new AttributeError(DATA_NEED_FIELD, message)));
        return new UnsupportedDataNeedException(REGION_CONNECTOR_ID, dataNeedId, message);
    }
}

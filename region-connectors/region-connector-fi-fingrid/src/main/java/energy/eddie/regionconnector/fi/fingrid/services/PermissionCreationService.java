package energy.eddie.regionconnector.fi.fingrid.services;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.process.model.PermissionStateTransitionException;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.fi.fingrid.FingridRegionConnectorMetadata;
import energy.eddie.regionconnector.fi.fingrid.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.fi.fingrid.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.fi.fingrid.permission.events.CreatedEvent;
import energy.eddie.regionconnector.fi.fingrid.permission.events.MalformedEvent;
import energy.eddie.regionconnector.fi.fingrid.permission.events.SimpleEvent;
import energy.eddie.regionconnector.fi.fingrid.permission.events.ValidatedEvent;
import energy.eddie.regionconnector.fi.fingrid.persistence.FiPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.exceptions.JwtCreationFailedException;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import energy.eddie.regionconnector.shared.security.JwtUtil;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static energy.eddie.regionconnector.fi.fingrid.FingridRegionConnectorMetadata.REGION_CONNECTOR_ID;

@Service
public class PermissionCreationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionCreationService.class);
    private static final String DATA_NEED_ID = "dataNeedId";
    private static final String UNSUPPORTED_DATA_NEED_MESSAGE = "This Region Connector only supports Validated Historical Data Data Needs for Gas and Electricity";
    private static final String UNSUPPORTED_GRANULARITY_MESSAGE = "Required granularity not supported.";
    private final Outbox outbox;
    private final DataNeedCalculationService<DataNeed> dataNeedCalculationService;
    private final DataNeedsService dataNeedsService;
    private final FiPermissionRequestRepository permissionRequestRepository;
    private final JwtUtil jwtUtil;

    public PermissionCreationService(
            Outbox outbox,
            DataNeedCalculationService<DataNeed> dataNeedCalculationService,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService,
            FiPermissionRequestRepository permissionRequestRepository,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") JwtUtil jwtUtil
    ) {
        this.outbox = outbox;
        this.dataNeedCalculationService = dataNeedCalculationService;
        this.dataNeedsService = dataNeedsService;
        this.permissionRequestRepository = permissionRequestRepository;
        this.jwtUtil = jwtUtil;
    }

    public CreatedPermissionRequest createAndValidatePermissionRequest(PermissionRequestForCreation permissionRequest) throws DataNeedNotFoundException, UnsupportedDataNeedException, JwtCreationFailedException {
        String permissionId = UUID.randomUUID().toString();
        LOGGER.info("Creating permission request with id {}", permissionId);
        outbox.commit(new CreatedEvent(permissionId,
                                       permissionRequest.connectionId(),
                                       permissionRequest.dataNeedId(),
                                       permissionRequest.customerIdentification()));
        var optionalDataNeed = dataNeedsService.findById(permissionRequest.dataNeedId());
        if (optionalDataNeed.isEmpty()) {
            outbox.commit(new MalformedEvent(
                    permissionId,
                    List.of(new AttributeError(DATA_NEED_ID, "Unknown dataNeedId"))
            ));
            throw new DataNeedNotFoundException(permissionRequest.dataNeedId());
        }
        var dataNeed = optionalDataNeed.get();
        var calculation = dataNeedCalculationService.calculate(dataNeed);

        if (!calculation.supportsDataNeed()
            || calculation.energyDataTimeframe() == null
            || calculation.permissionTimeframe() == null) {
            outbox.commit(new MalformedEvent(permissionId,
                                             List.of(new AttributeError(DATA_NEED_ID,
                                                                        UNSUPPORTED_DATA_NEED_MESSAGE))));
            throw new UnsupportedDataNeedException(REGION_CONNECTOR_ID,
                                                   dataNeed.id(),
                                                   UNSUPPORTED_DATA_NEED_MESSAGE);
        }
        if (calculation.granularities() == null || calculation.granularities().isEmpty()) {
            outbox.commit(new MalformedEvent(permissionId,
                                             List.of(new AttributeError(DATA_NEED_ID,
                                                                        UNSUPPORTED_GRANULARITY_MESSAGE))));
            throw new UnsupportedDataNeedException(REGION_CONNECTOR_ID,
                                                   dataNeed.id(),
                                                   UNSUPPORTED_GRANULARITY_MESSAGE);
        }

        outbox.commit(new ValidatedEvent(permissionId,
                                         calculation.granularities().getFirst(),
                                         calculation.energyDataTimeframe().start(),
                                         calculation.energyDataTimeframe().end()
        ));

        var accessToken = jwtUtil.createJwt(FingridRegionConnectorMetadata.REGION_CONNECTOR_ID, permissionId);

        return new CreatedPermissionRequest(permissionId, accessToken);
    }

    @Transactional(value = Transactional.TxType.REQUIRES_NEW)
    public void acceptOrReject(
            String permissionId,
            PermissionProcessStatus status
    ) throws PermissionNotFoundException, PermissionStateTransitionException {
        if (status != PermissionProcessStatus.ACCEPTED && status != PermissionProcessStatus.REJECTED) {
            throw new IllegalArgumentException("Invalid permission status: " + status);
        }
        var pr = permissionRequestRepository.findByPermissionId(permissionId)
                                            .orElseThrow(() -> new PermissionNotFoundException(permissionId));
        if (pr.status() != PermissionProcessStatus.VALIDATED) {
            throw new PermissionStateTransitionException(permissionId,
                                                         PermissionProcessStatus.VALIDATED,
                                                         PermissionProcessStatus.VALIDATED,
                                                         pr.status());
        }
        outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR));
        outbox.commit(new SimpleEvent(permissionId, status));
    }
}

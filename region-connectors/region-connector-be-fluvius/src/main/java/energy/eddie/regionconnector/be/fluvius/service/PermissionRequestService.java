package energy.eddie.regionconnector.be.fluvius.service;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.be.fluvius.FluviusRegionConnectorMetadata;
import energy.eddie.regionconnector.be.fluvius.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.be.fluvius.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.be.fluvius.permission.events.CreatedEvent;
import energy.eddie.regionconnector.be.fluvius.permission.events.MalformedEvent;
import energy.eddie.regionconnector.be.fluvius.permission.events.SimpleEvent;
import energy.eddie.regionconnector.be.fluvius.permission.events.ValidatedEvent;
import energy.eddie.regionconnector.be.fluvius.persistence.BePermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PermissionRequestService {
    public static final String DATA_NEED_ID = "dataNeedId";
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestService.class);
    private final DataNeedCalculationService<DataNeed> calculationService;
    private final Outbox outbox;
    private final BePermissionRequestRepository bePermissionRequestRepository;

    public PermissionRequestService(
            DataNeedCalculationService<DataNeed> calculationService, Outbox outbox,
            BePermissionRequestRepository bePermissionRequestRepository
    ) {
        this.calculationService = calculationService;
        this.outbox = outbox;
        this.bePermissionRequestRepository = bePermissionRequestRepository;
    }

    public CreatedPermissionRequest createPermissionRequest(PermissionRequestForCreation permissionRequestForCreation) throws UnsupportedDataNeedException, DataNeedNotFoundException {
        String permissionId = UUID.randomUUID().toString();
        outbox.commit(new CreatedEvent(permissionId,
                                       permissionRequestForCreation.dataNeedId(),
                                       permissionRequestForCreation.connectionId()));

        switch (calculationService.calculate(permissionRequestForCreation.dataNeedId())) {
            case AccountingPointDataNeedResult ignored -> {
                outbox.commit(new MalformedEvent(permissionId,
                                                 new AttributeError(DATA_NEED_ID,
                                                                    "AccountingPointDataNeedResult not supported!")));
                throw new UnsupportedDataNeedException(FluviusRegionConnectorMetadata.REGION_CONNECTOR_ID,
                                                       permissionRequestForCreation.dataNeedId(),
                                                       "AccountingPointDataNeedResult not supported!");
            }
            case DataNeedNotFoundResult ignored -> {
                outbox.commit(new MalformedEvent(permissionId,
                                                 new AttributeError(DATA_NEED_ID, "DataNeed not found!")));
                throw new DataNeedNotFoundException(permissionRequestForCreation.dataNeedId());
            }
            case DataNeedNotSupportedResult(String message) -> {
                outbox.commit(new MalformedEvent(permissionId, new AttributeError(DATA_NEED_ID, message)));
                throw new UnsupportedDataNeedException(FluviusRegionConnectorMetadata.REGION_CONNECTOR_ID,
                                                       permissionRequestForCreation.dataNeedId(),
                                                       message);
            }
            case ValidatedHistoricalDataDataNeedResult(
                    List<Granularity> granularities,
                    Timeframe ignored,
                    Timeframe energyTimeframe
            ) -> {
                LOGGER.info("Created permission request {}", permissionId);
                outbox.commit(new ValidatedEvent(permissionId,
                                                 energyTimeframe.start(),
                                                 energyTimeframe.end(),
                                                 granularities.getFirst(),
                                                 permissionRequestForCreation.flow()));
                return new CreatedPermissionRequest(permissionId);
            }
        }
    }

    public Optional<ConnectionStatusMessage> findConnectionStatusMessageById(String permissionId) {
        return bePermissionRequestRepository.findByPermissionId(permissionId)
                                            .map(ConnectionStatusMessage::new);
    }

    /**
     * Tries to transition the permission request to the desired status.
     * If the permission request is already in the desired state does nothing.
     *
     * @param permissionId  the permissionID of the permission request to be transitioned
     * @param desiredStatus the new status of the permission request, only {@code ACCEPTED} and {@code REJECTED} are allowed
     * @return if the permission request was transitioned to the {@code ACCEPTED} status
     * @throws PermissionNotFoundException if the permissionID does not exist
     */
    public boolean acceptOrRejectPermissionRequest(
            String permissionId,
            PermissionProcessStatus desiredStatus
    ) throws PermissionNotFoundException {
        if (desiredStatus != PermissionProcessStatus.ACCEPTED && desiredStatus != PermissionProcessStatus.REJECTED) {
            throw new IllegalArgumentException();
        }
        var res = bePermissionRequestRepository.findByPermissionId(permissionId);
        if (res.isEmpty()) {
            throw new PermissionNotFoundException(permissionId);
        }
        var pr = res.get();
        permissionId = pr.permissionId();
        var currentStatus = pr.status();
        if (currentStatus != PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR) {
            LOGGER.info("Permission request {} was {} and not {}",
                        permissionId,
                        currentStatus,
                        PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR);
            return isAccepted(currentStatus);
        }
        outbox.commit(new SimpleEvent(permissionId, desiredStatus));
        return isAccepted(desiredStatus);
    }

    private static boolean isAccepted(PermissionProcessStatus status) {
        return status == PermissionProcessStatus.ACCEPTED;
    }
}

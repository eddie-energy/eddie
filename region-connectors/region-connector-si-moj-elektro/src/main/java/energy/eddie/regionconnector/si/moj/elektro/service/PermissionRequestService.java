package energy.eddie.regionconnector.si.moj.elektro.service;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.si.moj.elektro.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.si.moj.elektro.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.si.moj.elektro.permission.events.CreatedEvent;
import energy.eddie.regionconnector.si.moj.elektro.permission.events.MalformedEvent;
import energy.eddie.regionconnector.si.moj.elektro.permission.events.ValidatedEvent;
import energy.eddie.regionconnector.si.moj.elektro.persistence.SiPermissionRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static energy.eddie.regionconnector.si.moj.elektro.MojElektroRegionConnectorMetadata.REGION_CONNECTOR_ID;

@Service
public class PermissionRequestService {

    private final SiPermissionRequestRepository siPermissionRequestRepository;
    private final DataNeedCalculationService<DataNeed> dataNeedCalculationService;
    private final Outbox outbox;

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestService.class);
    private static final String DATA_NEED_ID = "dataNeedId";

    public PermissionRequestService(SiPermissionRequestRepository siPermissionRequestRepository,
                                    DataNeedCalculationService<DataNeed> dataNeedCalculationService,
                                    Outbox outbox) {
        this.siPermissionRequestRepository = siPermissionRequestRepository;
        this.dataNeedCalculationService = dataNeedCalculationService;
        this.outbox = outbox;
    }

    public CreatedPermissionRequest createPermissionRequest(PermissionRequestForCreation requestForCreation)
            throws DataNeedNotFoundException, UnsupportedDataNeedException {
        String permissionId = UUID.randomUUID().toString();
        String dataNeedId = requestForCreation.dataNeedId();

        outbox.commit(new CreatedEvent(
                permissionId,
                requestForCreation.connectionId(),
                dataNeedId,
                requestForCreation.apiToken(),
                requestForCreation.meteringPoint()
        ));

        switch (dataNeedCalculationService.calculate(dataNeedId)) {
            case AccountingPointDataNeedResult ignored -> {
                String message = "AccountingPointDataNeedResult not supported!";
                outbox.commit(new MalformedEvent(permissionId, new AttributeError(DATA_NEED_ID, message)));
                throw new UnsupportedDataNeedException(REGION_CONNECTOR_ID, dataNeedId, message);
            }

            case DataNeedNotFoundResult ignored -> {
                outbox.commit(new MalformedEvent(
                        permissionId,
                        List.of(new AttributeError(DATA_NEED_ID, "Unknown dataNeedId"))
                ));
                throw new DataNeedNotFoundException(dataNeedId);
            }

            case DataNeedNotSupportedResult(String message) -> {
                outbox.commit(new MalformedEvent(permissionId, List.of(new AttributeError(DATA_NEED_ID, message))));
                throw new UnsupportedDataNeedException(REGION_CONNECTOR_ID, dataNeedId, message);
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
                        requestForCreation.apiToken()));
                return new CreatedPermissionRequest(permissionId);
            }
        }
    }

    public Optional<ConnectionStatusMessage> findConnectionStatusMessageById(String permissionId) {
        return siPermissionRequestRepository.findByPermissionId(permissionId).map(ConnectionStatusMessage::new);
    }
}

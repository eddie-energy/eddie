package energy.eddie.regionconnector.at.eda.handlers;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata;
import energy.eddie.regionconnector.at.eda.TransmissionException;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.permission.request.events.ExceptionEvent;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import energy.eddie.regionconnector.at.eda.requests.CCMOTimeFrame;
import energy.eddie.regionconnector.at.eda.requests.DsoIdAndMeteringPoint;
import energy.eddie.regionconnector.at.eda.requests.RequestDataType;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SendingEventHandler implements EventHandler<PermissionEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SendingEventHandler.class);
    private final EdaAdapter edaAdapter;
    private final AtConfiguration configuration;
    private final AtPermissionRequestRepository repository;
    private final Outbox outbox;

    protected SendingEventHandler(
            EventBus eventBus,
            EdaAdapter edaAdapter,
            AtConfiguration configuration,
            AtPermissionRequestRepository repository,
            Outbox outbox
    ) {
        this.edaAdapter = edaAdapter;
        this.configuration = configuration;
        this.repository = repository;
        this.outbox = outbox;
        eventBus.filteredFlux(PermissionProcessStatus.VALIDATED)
                .subscribe(this::accept);
    }

    @Override
    public void accept(PermissionEvent permissionEvent) {
        String permissionId = permissionEvent.permissionId();
        Optional<AtPermissionRequest> optionalPermissionRequest = repository.findByPermissionId(permissionId);
        if (optionalPermissionRequest.isEmpty()) {
            LOGGER.error("Got event for unknown Permission Request {}", permissionId);
            return;
        }
        AtPermissionRequest permissionRequest = optionalPermissionRequest.get();
        CCMORequest ccmoRequest = new CCMORequest(
                new DsoIdAndMeteringPoint(permissionRequest.dataSourceInformation().meteredDataAdministratorId(),
                                          permissionRequest.meteringPointId().orElse(null)),
                new CCMOTimeFrame(permissionRequest.start(), permissionRequest.end()),
                RequestDataType.METERING_DATA,
                permissionRequest.granularity(),
                EdaRegionConnectorMetadata.TRANSMISSION_CYCLE,
                configuration,
                permissionRequest.created()
        );
        try {
            edaAdapter.sendCMRequest(ccmoRequest);
        } catch (TransmissionException e) {
            outbox.commit(new ExceptionEvent(permissionId, PermissionProcessStatus.UNABLE_TO_SEND, e));
        }
    }
}

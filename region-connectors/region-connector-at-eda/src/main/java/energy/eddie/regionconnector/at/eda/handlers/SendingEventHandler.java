package energy.eddie.regionconnector.at.eda.handlers;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.TransmissionException;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.permission.request.events.ExceptionEvent;
import energy.eddie.regionconnector.at.eda.permission.request.events.ValidatedEvent;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import energy.eddie.regionconnector.at.eda.requests.CCMOTimeFrame;
import energy.eddie.regionconnector.at.eda.requests.DsoIdAndMeteringPoint;
import energy.eddie.regionconnector.at.eda.requests.RequestDataType;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedTransmissionCycle;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

@Component
public class SendingEventHandler implements EventHandler<ValidatedEvent> {
    private final AtPermissionRequestRepository repository;
    private final EdaAdapter edaAdapter;
    private final Outbox outbox;
    private final AtConfiguration configuration;
    private final DataNeedsService dataNeedsService;

    protected SendingEventHandler(
            EventBus eventBus,
            AtPermissionRequestRepository repository,
            EdaAdapter edaAdapter,
            Outbox outbox,
            AtConfiguration configuration,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService
    ) {
        this.repository = repository;
        this.edaAdapter = edaAdapter;
        this.outbox = outbox;
        this.configuration = configuration;
        this.dataNeedsService = dataNeedsService;
        eventBus.filteredFlux(ValidatedEvent.class)
                .filter(ValidatedEvent::needsToBeSent)
                .subscribe(this::threadedAccept);
    }

    @Override
    public void accept(ValidatedEvent permissionEvent) {
        String permissionId = permissionEvent.permissionId();
        AtPermissionRequest permissionRequest = repository.getByPermissionId(permissionId);

        CCMORequest ccmoRequest = ccmoRequest(permissionRequest, permissionEvent);
        try {
            edaAdapter.sendCMRequest(ccmoRequest);
        } catch (TransmissionException e) {
            outbox.commit(new ExceptionEvent(permissionId, PermissionProcessStatus.UNABLE_TO_SEND, e));
        }
    }

    private void threadedAccept(ValidatedEvent permissionEvent) {
        Thread.startVirtualThread(() -> accept(permissionEvent));
    }

    private CCMORequest ccmoRequest(AtPermissionRequest permissionRequest, ValidatedEvent event) {
        return new CCMORequest(
                new DsoIdAndMeteringPoint(
                        permissionRequest.dataSourceInformation().permissionAdministratorId(),
                        permissionRequest.meteringPointId().orElse(null)
                ),
                new CCMOTimeFrame(permissionRequest.start(), permissionRequest.end()),
                permissionRequest.cmRequestId(),
                permissionRequest.conversationId(),
                requestDataType(permissionRequest.granularity()),
                event.granularity() == null ? permissionRequest.granularity() : event.granularity(),
                AllowedTransmissionCycle.D,
                configuration,
                permissionRequest.created(),
                dataNeedsService.getById(permissionRequest.dataNeedId()).purpose()
        );
    }

    private static RequestDataType requestDataType(@Nullable AllowedGranularity granularity) {
        return granularity == null ? RequestDataType.MASTER_DATA : RequestDataType.METERING_DATA;
    }
}

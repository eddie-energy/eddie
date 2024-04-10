package energy.eddie.regionconnector.shared.event.sourcing.handlers.integration;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.agnostic.process.model.PermissionRequestRepository;
import energy.eddie.api.agnostic.process.model.events.InternalPermissionEvent;
import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.cmd.ConsentMarketDocument;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import energy.eddie.regionconnector.shared.permission.requests.extensions.v0_82.IntermediateConsentMarketDocument;
import energy.eddie.regionconnector.shared.permission.requests.extensions.v0_82.TransmissionScheduleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Sinks;

import java.time.ZoneId;

public class ConsentMarketDocumentMessageHandler<T extends PermissionRequest> implements EventHandler<PermissionEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsentMarketDocumentMessageHandler.class);
    private final PermissionRequestRepository<T> repository;
    private final Sinks.Many<ConsentMarketDocument> cmdSink;
    private final TransmissionScheduleProvider<T> transmissionScheduleProvider;
    private final String customerIdentifier;
    private final String countryCode;
    private final ZoneId zoneId;

    public ConsentMarketDocumentMessageHandler(EventBus eventBus,
                                               PermissionRequestRepository<T> repository,
                                               Sinks.Many<ConsentMarketDocument> cmdSink,
                                               String eligiblePartyId,
                                               CommonInformationModelConfiguration cimConfig,
                                               TransmissionScheduleProvider<T> transmissionScheduleProvider,
                                               ZoneId zoneId
    ) {
        this.repository = repository;
        this.cmdSink = cmdSink;
        this.customerIdentifier = eligiblePartyId;
        this.countryCode = cimConfig.eligiblePartyNationalCodingScheme().value();
        this.transmissionScheduleProvider = transmissionScheduleProvider;
        this.zoneId = zoneId;
        eventBus.filteredFlux(PermissionEvent.class)
                .subscribe(this::accept);
    }

    @Override
    public void accept(PermissionEvent permissionEvent) {
        if (permissionEvent instanceof InternalPermissionEvent) {
            return;
        }
        String permissionId = permissionEvent.permissionId();
        var optionalRequest = repository.findByPermissionId(permissionId);
        if (optionalRequest.isEmpty()) {
            LOGGER.warn("Got event without permission request for permission id {}", permissionId);
            cmdSink.tryEmitError(new PermissionNotFoundException(permissionId));
            return;
        }
        var permissionRequest = optionalRequest.get();
        try {
            cmdSink.tryEmitNext(
                    new IntermediateConsentMarketDocument<>(
                            permissionRequest,
                            permissionEvent.status(),
                            customerIdentifier,
                            transmissionScheduleProvider,
                            countryCode,
                            zoneId
                    ).toConsentMarketDocument()
            );
        } catch (RuntimeException exception) {
            LOGGER.warn("Error while trying to emit ConsentMarketDocument.", exception);
            cmdSink.tryEmitError(exception);
        }
    }
}

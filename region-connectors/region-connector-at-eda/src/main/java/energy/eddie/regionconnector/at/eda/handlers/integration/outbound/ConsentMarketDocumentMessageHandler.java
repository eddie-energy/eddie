package energy.eddie.regionconnector.at.eda.handlers.integration.outbound;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.cmd.ConsentMarketDocument;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import energy.eddie.regionconnector.shared.permission.requests.extensions.v0_82.IntermediateConsentMarketDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.TRANSMISSION_CYCLE;

@Component
public class ConsentMarketDocumentMessageHandler implements EventHandler<PermissionEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsentMarketDocumentMessageHandler.class);
    private final AtPermissionRequestRepository repository;
    private final Sinks.Many<ConsentMarketDocument> cmdSink;
    private final String customerIdentifier;
    private final String countryCode;

    public ConsentMarketDocumentMessageHandler(EventBus eventBus,
                                               AtPermissionRequestRepository repository,
                                               Sinks.Many<ConsentMarketDocument> cmdSink,
                                               AtConfiguration atConfig,
                                               CommonInformationModelConfiguration cimConfig) {
        this.repository = repository;
        this.cmdSink = cmdSink;
        this.customerIdentifier = atConfig.eligiblePartyId();
        this.countryCode = cimConfig.eligiblePartyNationalCodingScheme().value();
        eventBus.filteredFlux(PermissionEvent.class)
                .subscribe(this::accept);
    }

    @Override
    public void accept(PermissionEvent permissionEvent) {
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
                            pr -> TRANSMISSION_CYCLE.name(),
                            countryCode
                    ).toConsentMarketDocument()
            );
        } catch (RuntimeException exception) {
            LOGGER.warn("Error while trying to emit ConsentMarketDocument.", exception);
            cmdSink.tryEmitError(exception);
        }
    }
}

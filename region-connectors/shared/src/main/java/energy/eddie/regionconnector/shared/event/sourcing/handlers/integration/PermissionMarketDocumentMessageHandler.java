package energy.eddie.regionconnector.shared.event.sourcing.handlers.integration;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.agnostic.process.model.events.InternalPermissionEvent;
import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.agnostic.process.model.persistence.PermissionRequestRepository;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.v0_82.PermissionMarketDocumentProvider;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.shared.cim.v0_82.TransmissionScheduleProvider;
import energy.eddie.regionconnector.shared.cim.v0_82.pmd.IntermediatePermissionMarketDocument;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.ZoneId;

/**
 * An implementation for the {@link PermissionMarketDocumentProvider} that converts a {@link PermissionEvent} to a CIM compliant document.
 * It subscribes to all events present in an {@link EventBus} and based on thos creates PermissionMarketDocuments.
 */
public class PermissionMarketDocumentMessageHandler<T extends PermissionRequest> implements EventHandler<PermissionEvent>, PermissionMarketDocumentProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionMarketDocumentMessageHandler.class);
    private final PermissionRequestRepository<T> repository;
    private final DataNeedsService dataNeedsService;
    private final Sinks.Many<PermissionEnvelope> pmdSink = Sinks.many().multicast().onBackpressureBuffer();
    private final TransmissionScheduleProvider<T> transmissionScheduleProvider;
    private final String customerIdentifier;
    private final String countryCode;
    private final ZoneId zoneId;

    public PermissionMarketDocumentMessageHandler(
            EventBus eventBus,
            PermissionRequestRepository<T> repository,
            DataNeedsService dataNeedsService,
            String eligiblePartyId,
            CommonInformationModelConfiguration cimConfig,
            TransmissionScheduleProvider<T> transmissionScheduleProvider,
            ZoneId zoneId
    ) {
        this.repository = repository;
        this.dataNeedsService = dataNeedsService;
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
            pmdSink.tryEmitError(new PermissionNotFoundException(permissionId));
            return;
        }
        var permissionRequest = optionalRequest.get();
        var dataNeed = dataNeedsService.getById(permissionRequest.dataNeedId());
        try {
            pmdSink.tryEmitNext(
                    new IntermediatePermissionMarketDocument<>(
                            permissionRequest,
                            permissionEvent.status(),
                            customerIdentifier,
                            transmissionScheduleProvider,
                            countryCode,
                            zoneId,
                            dataNeed
                    ).toPermissionMarketDocument()
            );
        } catch (RuntimeException exception) {
            LOGGER.warn("Error while trying to emit PermissionMarketDocument.", exception);
            pmdSink.tryEmitError(exception);
        }
    }

    @Override
    public Flux<PermissionEnvelope> getPermissionMarketDocumentStream() {
        return pmdSink.asFlux();
    }

    @Override
    public void close() {
        pmdSink.tryEmitComplete();
    }
}

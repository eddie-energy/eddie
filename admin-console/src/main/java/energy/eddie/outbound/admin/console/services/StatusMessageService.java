package energy.eddie.outbound.admin.console.services;

import energy.eddie.api.v0_82.outbound.PermissionMarketDocumentOutboundConnector;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_82.pmd.PermissionMarketDocumentComplexType;
import energy.eddie.outbound.admin.console.data.StatusMessage;
import energy.eddie.outbound.admin.console.data.StatusMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;


@Service
public class StatusMessageService implements PermissionMarketDocumentOutboundConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatusMessageService.class);
    private final StatusMessageRepository statusMessageRepository;

    public StatusMessageService(StatusMessageRepository statusMessageRepository) {
        this.statusMessageRepository = statusMessageRepository;
    }

    @Override
    public void setPermissionMarketDocumentStream(Flux<PermissionEnvelope> permissionMarketDocumentStream) {
        permissionMarketDocumentStream
                .publishOn(Schedulers.boundedElastic())
                .doOnError(error -> LOGGER.error("Error receiving messages from the Flux stream: {}",
                                                 error.getMessage(),
                                                 error))
                .subscribe(this::processMessage);
    }

    void processMessage(PermissionEnvelope message) {
        try {
            StatusMessage statusMessage = createStatusMessage(message.getPermissionMarketDocument());
            statusMessageRepository.save(statusMessage);
            LOGGER.debug("Saved status message with MRID: {}", message.getPermissionMarketDocument().getMRID());
        } catch (Exception ex) {
            LOGGER.error("Error saving status message: {}", ex.getMessage(), ex);
        }
    }

    private StatusMessage createStatusMessage(PermissionMarketDocumentComplexType message) {
        // TODO: GH-638 Unknown is actually AIIDA, which does not have a schema
        String country;
        if (message.getReceiverMarketParticipantMRID().getCodingScheme() == null) {
            country = "Unknown";
        } else {
            country = message.getReceiverMarketParticipantMRID().getCodingScheme().value();
        }

        var permission = message.getPermissionList().getPermissions().getFirst()
                                .getMktActivityRecordList().getMktActivityRecords().getFirst();

        return new StatusMessage(
                message.getMRID(),
                permission.getType(),
                message.getDescription(),
                country,
                message.getReceiverMarketParticipantMRID().getValue(),
                permission.getCreatedDateTime(),
                permission.getStatus().toString(),
                permission.getDescription()
        );
    }
}
package energy.eddie.admin.console.data;

import energy.eddie.api.v0_82.PermissionMarketDocumentServiceInterface;
import energy.eddie.cim.v0_82.pmd.PermissionEnveloppe;
import energy.eddie.cim.v0_82.pmd.PermissionMarketDocumentComplexType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;


@Service
public class StatusMessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatusMessageService.class);
    private final StatusMessageRepository statusMessageRepository;
    private final PermissionMarketDocumentServiceInterface permissionMarketDocumentService;

    public StatusMessageService(
            StatusMessageRepository statusMessageRepository,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
            PermissionMarketDocumentServiceInterface permissionMarketDocumentService
    ) {
        this.statusMessageRepository = statusMessageRepository;
        this.permissionMarketDocumentService = permissionMarketDocumentService;
        subscribeToFlux();
    }

    private void subscribeToFlux() {
        var flux = permissionMarketDocumentService.getPermissionMarketDocumentStream();
        flux
                .publishOn(Schedulers.boundedElastic())
                .doOnError(error -> LOGGER.error("Error receiving messages from the Flux stream: {}",
                                                 error.getMessage(),
                                                 error))
                .subscribe(this::processMessage);
    }

    private void processMessage(PermissionEnveloppe message) {
        try {
            StatusMessage statusMessage = createStatusMessage(message.getPermissionMarketDocument());
            statusMessageRepository.save(statusMessage);
            LOGGER.debug("Saved status message with MRID: {}", message.getPermissionMarketDocument().getMRID());
        } catch (Exception ex) {
            LOGGER.error("Error saving status message: {}", ex.getMessage(), ex);
        }
    }

    private StatusMessage createStatusMessage(PermissionMarketDocumentComplexType message) {
        String country;
        if (message.getReceiverMarketParticipantMRID().getCodingScheme() == null) {
            country = "Unknown";
        } else {
            country = message.getReceiverMarketParticipantMRID().getCodingScheme().value();
        }
        return new StatusMessage(
                message.getMRID(),
                message.getPermissionList().getPermissions().getFirst()
                        .getMktActivityRecordList().getMktActivityRecords().getFirst().getType(),
                country,
                message.getReceiverMarketParticipantMRID().getValue(),
                message.getPermissionList().getPermissions().getFirst()
                       .getMktActivityRecordList().getMktActivityRecords().getFirst()
                       .getCreatedDateTime(),
                message.getPermissionList().getPermissions().getFirst()
                       .getMktActivityRecordList().getMktActivityRecords().getFirst()
                       .getStatus().toString()
        );
    }
}
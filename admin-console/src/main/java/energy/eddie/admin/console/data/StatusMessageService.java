package energy.eddie.admin.console.data;

import energy.eddie.api.v0_82.ConsentMarketDocumentServiceInterface;
import energy.eddie.cim.v0_82.cmd.ConsentMarketDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;


@Service
public class StatusMessageService {

    private final StatusMessageRepository statusMessageRepository;
    private final ConsentMarketDocumentServiceInterface consentMarketDocumentService;
    private static final Logger LOGGER = LoggerFactory.getLogger(StatusMessageService.class);

    public StatusMessageService(StatusMessageRepository statusMessageRepository,
                                ConsentMarketDocumentServiceInterface consentMarketDocumentService) {
        this.statusMessageRepository = statusMessageRepository;
        this.consentMarketDocumentService = consentMarketDocumentService;
        subscribeToFlux();
    }

    private void subscribeToFlux() {
        Flux<ConsentMarketDocument> flux = consentMarketDocumentService.getConsentMarketDocumentStream();
        flux
                .publishOn(Schedulers.boundedElastic())
                .doOnError(error -> LOGGER.error("Error receiving messages from the Flux stream: {}", error.getMessage(), error))
                .subscribe(this::processMessage);
    }

    private void processMessage(ConsentMarketDocument message) {
        try {
            StatusMessage statusMessage = createStatusMessage(message);
            statusMessageRepository.save(statusMessage);
            LOGGER.debug("Saved status message with MRID: {}", message.getMRID());
        } catch (Exception ex) {
            LOGGER.error("Error saving status message: {}", ex.getMessage(), ex);
        }
    }

    private StatusMessage createStatusMessage(ConsentMarketDocument message) {
        return new StatusMessage(
                message.getMRID(),
                message.getReceiverMarketParticipantMRID().getCodingScheme().toString(),
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
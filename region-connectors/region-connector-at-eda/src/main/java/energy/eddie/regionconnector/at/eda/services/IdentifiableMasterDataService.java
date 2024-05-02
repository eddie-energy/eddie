package energy.eddie.regionconnector.at.eda.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.dto.EdaMasterData;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableMasterData;
import energy.eddie.regionconnector.at.eda.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class IdentifiableMasterDataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdentifiableMasterDataService.class);
    private final AtPermissionRequestRepository repository;

    private final Flux<IdentifiableMasterData> identifiableMasterDataFlux;
    private final Outbox outbox;

    public IdentifiableMasterDataService(
            Flux<EdaMasterData> masterDataFlux,
            AtPermissionRequestRepository repository,
            Outbox outbox
    ) {
        this.repository = repository;

        this.identifiableMasterDataFlux = masterDataFlux
                .mapNotNull(this::mapToIdentifiableMasterData)
                .publish()
                .refCount();
        this.outbox = outbox;
    }

    private @Nullable IdentifiableMasterData mapToIdentifiableMasterData(EdaMasterData masterData) {

        String conversationId = masterData.conversationId();
        var permissionRequest = repository
                .findByConversationIdOrCMRequestId(conversationId, null);

        if (permissionRequest.isEmpty()) {
            LOGGER.warn("No permission request found for master data with conversation id {}", conversationId);
            return null;
        }

        var request = permissionRequest.get();
        LOGGER.atInfo()
              .addArgument(conversationId)
              .addArgument(request::permissionId)
              .addArgument(request::connectionId)
              .log("Received master data (ConversationId '{}') for permissionId {} and connectionId {}");

        outbox.commit(new SimpleEvent(request.permissionId(), PermissionProcessStatus.FULFILLED));
        return new IdentifiableMasterData(
                masterData,
                request
        );
    }

    public Flux<IdentifiableMasterData> getIdentifiableMasterDataStream() {
        return identifiableMasterDataFlux;
    }
}

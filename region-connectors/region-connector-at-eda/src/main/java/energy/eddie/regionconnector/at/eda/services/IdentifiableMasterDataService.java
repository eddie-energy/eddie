package energy.eddie.regionconnector.at.eda.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.dto.EdaMasterData;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableMasterData;
import energy.eddie.regionconnector.at.eda.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class IdentifiableMasterDataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdentifiableMasterDataService.class);
    private final AtPermissionRequestRepository repository;

    private final Outbox outbox;

    public IdentifiableMasterDataService(
            AtPermissionRequestRepository repository,
            Outbox outbox
    ) {
        this.repository = repository;
        this.outbox = outbox;
    }

    public Optional<IdentifiableMasterData> mapToIdentifiableMasterData(EdaMasterData masterData) {
        var permissionRequest = repository.findByConversationIdAndMeteringPointId(masterData.conversationId(),
                                                                                  masterData.meteringPoint());

        if (permissionRequest.isEmpty()) {
            return Optional.empty();
        }

        var request = permissionRequest.get();
        LOGGER.atInfo()
              .addArgument(masterData::conversationId)
              .addArgument(request::permissionId)
              .addArgument(request::connectionId)
              .log("Received master data (ConversationId '{}') for permissionId {} and connectionId {}");

        outbox.commit(new SimpleEvent(request.permissionId(), PermissionProcessStatus.FULFILLED));
        return Optional.of(new IdentifiableMasterData(
                masterData,
                request
        ));
    }
}

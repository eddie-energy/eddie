package energy.eddie.regionconnector.at.eda.services;

import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.dto.EdaConsumptionRecord;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableConsumptionRecord;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.List;

@Component
public class IdentifiableConsumptionRecordService {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdentifiableConsumptionRecordService.class);
    private final AtPermissionRequestRepository repository;

    private final Flux<IdentifiableConsumptionRecord> identifiableConsumptionRecordFlux;

    public IdentifiableConsumptionRecordService(
            Flux<EdaConsumptionRecord> consumptionRecordFlux,
            AtPermissionRequestRepository repository
    ) {
        this.repository = repository;

        this.identifiableConsumptionRecordFlux = consumptionRecordFlux
                .mapNotNull(this::mapToIdentifiableConsumptionRecord)
                .publish()
                .refCount();
    }

    private @Nullable IdentifiableConsumptionRecord mapToIdentifiableConsumptionRecord(EdaConsumptionRecord consumptionRecord) {
        LocalDate startDate = consumptionRecord.startDate();
        LocalDate endDate = consumptionRecord.endDate();
        String meteringPoint = consumptionRecord.meteringPoint();

        List<AtPermissionRequest> permissionRequests = repository
                .findByConversationIdOrCMRequestId(consumptionRecord.conversationId(), null)
                .map(List::of)
                .orElseGet(() -> repository
                        .findAcceptedAndFulfilledAndSentToPAByMeteringPointIdAndDate(meteringPoint, startDate));

        if (permissionRequests.isEmpty()) {
            LOGGER.warn("No permission requests found for consumption record with date {}", startDate);
            return null;
        }

        permissionRequests.forEach(permissionRequest -> LOGGER.info(
                "Received consumption record (ConversationId '{}') for permissionId {} and connectionId {}",
                permissionRequest.conversationId(),
                permissionRequest.permissionId(),
                permissionRequest.connectionId()));

        return new IdentifiableConsumptionRecord(
                consumptionRecord,
                permissionRequests,
                startDate,
                endDate
        );
    }

    public Flux<IdentifiableConsumptionRecord> getIdentifiableConsumptionRecordStream() {
        return identifiableConsumptionRecordFlux;
    }
}

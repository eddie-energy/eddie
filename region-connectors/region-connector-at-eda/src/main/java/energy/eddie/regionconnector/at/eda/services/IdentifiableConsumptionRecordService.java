package energy.eddie.regionconnector.at.eda.services;

import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.dto.EdaConsumptionRecord;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableConsumptionRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class IdentifiableConsumptionRecordService {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdentifiableConsumptionRecordService.class);
    private final AtPermissionRequestRepository repository;


    public IdentifiableConsumptionRecordService(
            AtPermissionRequestRepository repository
    ) {
        this.repository = repository;
    }

    public Optional<IdentifiableConsumptionRecord> mapToIdentifiableConsumptionRecord(EdaConsumptionRecord consumptionRecord) {
        LocalDate startDate = consumptionRecord.startDate();
        LocalDate endDate = consumptionRecord.endDate();
        String meteringPoint = consumptionRecord.meteringPoint();

        List<AtPermissionRequest> permissionRequests = repository
                .findAcceptedAndFulfilledAndSentToPAByMeteringPointIdAndDate(
                        meteringPoint,
                        startDate
                );

        if (permissionRequests.isEmpty()) {
            LOGGER.warn("No permission requests found for consumption record with date {}", startDate);
            return Optional.empty();
        }

        permissionRequests.forEach(permissionRequest -> LOGGER.info(
                "Received consumption record (ConversationId '{}') for permissionId {} and connectionId {}",
                permissionRequest.conversationId(),
                permissionRequest.permissionId(),
                permissionRequest.connectionId()));

        return Optional.of(new IdentifiableConsumptionRecord(
                consumptionRecord,
                permissionRequests,
                startDate,
                endDate
        ));
    }
}

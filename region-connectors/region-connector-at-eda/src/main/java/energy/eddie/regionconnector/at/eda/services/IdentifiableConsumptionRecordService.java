package energy.eddie.regionconnector.at.eda.services;

import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.ConsumptionRecord;
import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.Energy;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableConsumptionRecord;
import energy.eddie.regionconnector.at.eda.utils.DateTimeConstants;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class IdentifiableConsumptionRecordService {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdentifiableConsumptionRecordService.class);
    private final PermissionRequestService permissionRequestService;

    private final Flux<IdentifiableConsumptionRecord> identifiableConsumptionRecordFlux;

    public IdentifiableConsumptionRecordService(Flux<ConsumptionRecord> consumptionRecordFlux, PermissionRequestService permissionRequestService) {
        this.permissionRequestService = permissionRequestService;

        this.identifiableConsumptionRecordFlux = consumptionRecordFlux
                .mapNotNull(this::mapToIdentifiableConsumptionRecord)
                .publish()
                .refCount();
    }

    public Flux<IdentifiableConsumptionRecord> getIdentifiableConsumptionRecordStream() {
        return identifiableConsumptionRecordFlux;
    }

    private @Nullable IdentifiableConsumptionRecord mapToIdentifiableConsumptionRecord(ConsumptionRecord consumptionRecord) {
        var energyOptional = extractEnergyFromConsumptionRecord(consumptionRecord);
        if (energyOptional.isEmpty()) {
            LOGGER.warn("No Energy found in ProcessDirectory of ConsumptionRecord");
            return null;
        }

        LocalDate date = getMeteringPeriodStartDate(energyOptional.get());
        List<AtPermissionRequest> permissionRequests = permissionRequestService.findByMeteringPointIdAndDate(
                consumptionRecord.getProcessDirectory().getMeteringPoint(),
                date
        );
        if (permissionRequests.isEmpty()) {
            LOGGER.warn("No permission requests found for consumption record with date {}", date);
            return null;
        }

        permissionRequests.forEach(permissionRequest -> LOGGER.info("Received consumption record (ConversationId '{}') for permissionId {} and connectionId {}", permissionRequest.conversationId(), permissionRequest.permissionId(), permissionRequest.connectionId()));

        return new IdentifiableConsumptionRecord(consumptionRecord, permissionRequests);
    }

    private Optional<Energy> extractEnergyFromConsumptionRecord(ConsumptionRecord consumptionRecord) {
        return consumptionRecord.getProcessDirectory().getEnergy().stream().findFirst();
    }

    private LocalDate getMeteringPeriodStartDate(Energy energy) {
        return energy.getMeteringPeriodStart().toGregorianCalendar().toZonedDateTime().withZoneSameLocal(DateTimeConstants.AT_ZONE_ID).toLocalDate();
    }
}
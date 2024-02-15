package energy.eddie.regionconnector.at.eda.services;

import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.ConsumptionRecord;
import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.Energy;
import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.api.agnostic.process.model.TerminalPermissionRequestState;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableConsumptionRecord;
import energy.eddie.regionconnector.at.eda.utils.DateTimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.Optional;

@Component
public class PermissionRequestFulfillmentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestFulfillmentService.class);

    public PermissionRequestFulfillmentService(Flux<IdentifiableConsumptionRecord> consumptionRecordStream) {
        consumptionRecordStream.subscribe(this::checkForFulfillment);
    }

    private Optional<Energy> extractEnergyFromConsumptionRecord(ConsumptionRecord consumptionRecord) {
        return consumptionRecord.getProcessDirectory().getEnergy().stream().findFirst();
    }

    private LocalDate getMeteringPeriodEndDate(Energy energy) {
        return energy.getMeteringPeriodEnd().toGregorianCalendar().toZonedDateTime().withZoneSameLocal(DateTimeConstants.AT_ZONE_ID).toLocalDate();
    }

    private void checkForFulfillment(IdentifiableConsumptionRecord identifiableConsumptionRecord) {
        identifiableConsumptionRecord.permissionRequests().forEach(permissionRequest -> {
            if (isTerminalState(permissionRequest.state())) {
                return;
            }
            var consumptionRecord = identifiableConsumptionRecord.consumptionRecord();

            var permissionEnd = permissionRequest.end();
            if (permissionEnd == null) {
                return;
            }

            var energyOptional = extractEnergyFromConsumptionRecord(consumptionRecord);
            if (energyOptional.isEmpty()) {
                LOGGER.warn("No Energy found in ProcessDirectory of ConsumptionRecord");
                return;
            }

            var meteringPeriodEnd = getMeteringPeriodEndDate(energyOptional.get());

            // if we request quarter hourly data up to the 24.01.2024, the last consumption record we get will have an meteringPeriodStart of 24.01.2024T23:45:00 and an meteringPeriodEnd of 25.01.2024T00:00:00
            // so if the permissionEnd is before the meteringPeriodEnd the permission request is fulfilled
            if (permissionEnd.toLocalDate().isBefore(meteringPeriodEnd)) {
                try {
                    permissionRequest.fulfill();
                } catch (StateTransitionException e) {
                    LOGGER.error("Error while fulfilling permission request", e);
                }
            }
        });
    }

    private boolean isTerminalState(PermissionRequestState state) {
        return state instanceof TerminalPermissionRequestState;
    }
}
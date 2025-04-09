package energy.eddie.regionconnector.at.eda.tasks;

import energy.eddie.regionconnector.at.eda.dto.IdentifiableConsumptionRecord;
import energy.eddie.regionconnector.at.eda.permission.request.events.DataReceivedEvent;
import energy.eddie.regionconnector.at.eda.provider.IdentifiableStreams;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MeterReadingUpdateTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(MeterReadingUpdateTask.class);
    private final Outbox outbox;

    public MeterReadingUpdateTask(Outbox outbox, IdentifiableStreams streams) {
        this.outbox = outbox;
        streams.consumptionRecordStream()
               .subscribe(this::updateMeterReading);
    }

    public void updateMeterReading(IdentifiableConsumptionRecord consumptionRecord) {
        var start = consumptionRecord.meterReadingStartDate();
        var end = consumptionRecord.meterReadingEndDate();
        for (var permissionRequest : consumptionRecord.permissionRequests()) {
            var permissionId = permissionRequest.permissionId();
            LOGGER.info("Updating permission request {} with latest data ranging from {} to {}",
                        permissionId,
                        start,
                        end);
            outbox.commit(new DataReceivedEvent(permissionId, permissionRequest.status(), start, end));
        }
    }
}

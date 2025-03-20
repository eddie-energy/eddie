package energy.eddie.regionconnector.cds.services;

import energy.eddie.regionconnector.cds.permission.events.InternalPollingEvent;
import energy.eddie.regionconnector.cds.providers.IdentifiableDataStreams;
import energy.eddie.regionconnector.cds.providers.vhd.IdentifiableValidatedHistoricalData;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public class LastMeterReadingUpdateService {
    private final Outbox outbox;

    public LastMeterReadingUpdateService(IdentifiableDataStreams identifiableDataStreams, Outbox outbox) {
        this.outbox = outbox;
        identifiableDataStreams.validatedHistoricalData()
                               .subscribe(this::updateLastMeterReadings);
    }

    private void updateLastMeterReadings(IdentifiableValidatedHistoricalData identifiableData) {
        var old = identifiableData.permissionRequest().lastMeterReadings();
        var lastMeterReadings = new HashMap<>(old);
        for (var segment : identifiableData.payload().usageSegments()) {
            var end = segment.getSegmentEnd().toZonedDateTime();
            for (var meter : segment.getRelatedMeterdevices()) {
                if (lastMeterReadings.containsKey(meter)) {
                    var reading = lastMeterReadings.get(meter);
                    if (reading.isBefore(end)) {
                        lastMeterReadings.put(meter, end);
                    }
                } else {
                    lastMeterReadings.put(meter, end);
                }
            }
        }
        outbox.commit(new InternalPollingEvent(identifiableData.permissionRequest().permissionId(), lastMeterReadings));
    }
}

package energy.eddie.regionconnector.de.eta.services;

import energy.eddie.regionconnector.de.eta.permission.events.LatestMeterReadingEvent;
import energy.eddie.regionconnector.de.eta.providers.vhd.IdentifiableValidatedHistoricalData;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.de.eta.streams.ValidatedHistoricalDataStream;
import org.springframework.stereotype.Service;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Comparator;

@Service
public class MeterReadingUpdateService {
    private final Outbox outbox;

    public MeterReadingUpdateService(ValidatedHistoricalDataStream stream, Outbox outbox) {
        this.outbox = outbox;
        stream.validatedHistoricalData()
                .subscribe(this::handleMeterReading);
    }

    public void handleMeterReading(IdentifiableValidatedHistoricalData data) {
        Object rawPayload = data.payload();
        ZonedDateTime latestReadingDate = null;

        if (rawPayload instanceof Collection<?> list && !list.isEmpty()) {

            Object firstItem = list.iterator().next();

            if (firstItem instanceof ReadingObject) {
                latestReadingDate = ((Collection<ReadingObject>) rawPayload).stream()
                        .map(ReadingObject::getTimestamp)
                        .max(Comparator.naturalOrder())
                        .orElse(null);
            }
        }

        if (latestReadingDate != null) {
            LatestMeterReadingEvent event = new LatestMeterReadingEvent(
                    data.permissionRequest().permissionId(),
                    latestReadingDate
            );
            outbox.commit(event);
        }
    }
    interface ReadingObject { ZonedDateTime getTimestamp(); }
}
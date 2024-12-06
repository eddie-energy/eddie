package energy.eddie.regionconnector.us.green.button.services;

import com.rometools.rome.feed.synd.SyndFeed;
import energy.eddie.api.agnostic.IdentifiablePayload;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.us.green.button.atom.feed.Query;
import energy.eddie.regionconnector.us.green.button.permission.events.PollingStatus;
import energy.eddie.regionconnector.us.green.button.permission.events.UsMeterReadingUpdateEvent;
import energy.eddie.regionconnector.us.green.button.permission.events.UsSimpleEvent;
import energy.eddie.regionconnector.us.green.button.permission.request.api.UsGreenButtonPermissionRequest;
import energy.eddie.regionconnector.us.green.button.permission.request.meter.reading.MeterReading;
import energy.eddie.regionconnector.us.green.button.persistence.MeterReadingRepository;
import jakarta.xml.bind.Unmarshaller;
import org.naesb.espi.IntervalBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class PermissionUpdateService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionUpdateService.class);
    private final Outbox outbox;
    private final Unmarshaller unmarshaller;
    private final MeterReadingRepository meterReadingRepository;

    public PermissionUpdateService(
            Outbox outbox,
            PublishService publishService,
            Jaxb2Marshaller marshaller,
            MeterReadingRepository meterReadingRepository
    ) {
        this.outbox = outbox;
        this.unmarshaller = marshaller.createUnmarshaller();
        this.meterReadingRepository = meterReadingRepository;
        publishService.flux()
                      .subscribe(this::updatePermissionRequest);
    }

    public void updatePermissionRequest(IdentifiablePayload<UsGreenButtonPermissionRequest, SyndFeed> payload) {
        var feed = payload.payload();
        var permissionId = payload.permissionRequest().permissionId();
        LOGGER.info("Updating permission request {} with latest polling data", permissionId);
        var query = new Query(feed, unmarshaller);
        var intervalBlocks = query.findAllByTitle("IntervalBlock");
        if (intervalBlocks.isEmpty()) {
            LOGGER.warn("No data found for permission request {}", permissionId);
            outbox.commit(new UsSimpleEvent(permissionId, PermissionProcessStatus.UNFULFILLABLE));
            return;
        }
        List<MeterReading> lastMeterReadings = new ArrayList<>();
        for (var entry : intervalBlocks) {
            var id = Query.intervalBlockSelfToUsagePointId(entry);
            var intervalBlock = query.unmarshal(entry, IntervalBlock.class);
            if (id == null || intervalBlock == null || intervalBlock.getInterval() == null) {
                LOGGER.warn("Unexpected interval block: {}", intervalBlock);
                continue;
            }
            var interval = intervalBlock.getInterval();
            var lastReading = Instant.ofEpochSecond(interval.getStart() + interval.getDuration())
                                     .atZone(ZoneOffset.UTC);
            for (var meterReading : payload.permissionRequest().lastMeterReadings()) {
                if (meterReading.meterUid().equals(id)) {
                    meterReading.setLastMeterReading(lastReading);
                    meterReading.setHistoricalCollectionStatus(PollingStatus.DATA_NOT_READY);
                    meterReadingRepository.save(meterReading);
                    lastMeterReadings.add(meterReading);
                    break;
                }
            }
        }
        outbox.commit(new UsMeterReadingUpdateEvent(permissionId, lastMeterReadings));
    }
}

package energy.eddie.regionconnector.us.green.button.services;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import energy.eddie.api.agnostic.IdentifiablePayload;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.us.green.button.permission.events.UsMeterReadingUpdateEvent;
import energy.eddie.regionconnector.us.green.button.permission.events.UsPollingNotReadyEvent;
import energy.eddie.regionconnector.us.green.button.permission.request.api.UsGreenButtonPermissionRequest;
import jakarta.annotation.Nullable;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.naesb.espi.IntervalBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Service;

import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public class PermissionUpdateService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionUpdateService.class);
    private final Outbox outbox;
    private final Unmarshaller unmarshaller;

    public PermissionUpdateService(Outbox outbox, PublishService publishService, Jaxb2Marshaller marshaller) {
        this.outbox = outbox;
        this.unmarshaller = marshaller.createUnmarshaller();
        publishService.flux()
                      .subscribe(this::updatePermissionRequest);
    }

    public void updatePermissionRequest(IdentifiablePayload<UsGreenButtonPermissionRequest, SyndFeed> payload) {
        var feed = payload.payload();
        var permissionId = payload.permissionRequest().permissionId();
        LOGGER.info("Updating permission request {} with latest polling data", permissionId);
        var intervalBlocks = findIntervalBlocks(feed);
        if (intervalBlocks.isEmpty()) {
            LOGGER.info("No data found for permission request {}", permissionId);
            outbox.commit(new UsPollingNotReadyEvent(permissionId));
            return;
        }
        var lastMeterReadings = new HashMap<String, ZonedDateTime>();
        for (var entry : intervalBlocks) {
            var id = intervalBlockSelfToUsagePointId(entry);
            var intervalBlock = unmarshallToIntervalBlock(entry);
            if (id == null || intervalBlock == null || intervalBlock.getInterval() == null) {
                LOGGER.warn("Unexpected interval block: {}", intervalBlock);
                continue;
            }
            var interval = intervalBlock.getInterval();
            var lastReading = Instant.ofEpochSecond(interval.getStart() + interval.getDuration())
                                     .atZone(ZoneOffset.UTC);
            lastMeterReadings.put(id, lastReading);
        }
        outbox.commit(new UsMeterReadingUpdateEvent(permissionId, lastMeterReadings));
    }

    private static List<SyndEntry> findIntervalBlocks(SyndFeed feed) {
        var list = new ArrayList<SyndEntry>();
        for (var entry : feed.getEntries()) {
            var title = entry.getTitle();
            if (title.equals("IntervalBlock")) {
                list.add(entry);
            }
        }
        return list;
    }

    @Nullable
    private static String intervalBlockSelfToUsagePointId(SyndEntry entry) {
        // UsagePoint self:    https://utilityapi.com/DataCustodian/espi/1_1/resource/Subscription/467189/UsagePoint/1669851
        // IntervalBlock self: https://utilityapi.com/DataCustodian/espi/1_1/resource/Subscription/467189/UsagePoint/1669851/MeterReading/1669851-1725408000-1725321600_kwh_1/IntervalBlock/000001
        for (var link : entry.getLinks()) {
            if (link.getRel().equals("self")) {
                var href = link.getHref();
                var splitted = href.split("/MeterReading", -1);
                if (splitted.length <= 1) {
                    LOGGER.warn("Found invalid self link on IntervalBlock: {}", entry);
                    return null;
                }
                return new File(URI.create(splitted[0]).getPath()).getName();
            }
        }
        return null;
    }

    @Nullable
    private IntervalBlock unmarshallToIntervalBlock(SyndEntry entry) {
        if (entry.getContents().size() != 1) {
            return null;
        }
        var content = entry.getContents().getFirst().getValue().getBytes(StandardCharsets.UTF_8);
        try {
            var obj = unmarshaller.unmarshal(new StreamSource(new ByteArrayInputStream(content)), IntervalBlock.class);
            return obj.getValue();
        } catch (JAXBException e) {
            LOGGER.warn("Error unmarshalling IntervalBlock", e);
            return null;
        }
    }
}

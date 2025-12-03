package energy.eddie.regionconnector.de.eta.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.regionconnector.de.eta.permission.events.LatestMeterReadingEvent;
import energy.eddie.regionconnector.de.eta.streams.ValidatedHistoricalDataStream;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

@Component
public class MeterReadingExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeterReadingExtractor.class);
    private final ObjectMapper objectMapper;
    private final EventBus eventBus;

    public MeterReadingExtractor(
            ValidatedHistoricalDataStream stream,
            ObjectMapper objectMapper,
            EventBus eventBus
    ) {
        this.objectMapper = objectMapper;
        this.eventBus = eventBus;

        stream.validatedHistoricalData()
                .subscribe(this::processData, this::handleError);
    }

    private void processData(energy.eddie.regionconnector.de.eta.providers.vhd.IdentifiableValidatedHistoricalData data) {
        try {
            JsonNode rootNode = objectMapper.valueToTree(data.payload());

            String periodEnd = rootNode.get("ValidatedHistoricalData_MarketDocument")
                    .get("period")
                    .get("timeInterval")
                    .get("end")
                    .asText();

            ZonedDateTime latestReading = ZonedDateTime.parse(periodEnd);

            String permissionId = data.permissionRequest().permissionId();

            eventBus.emit(new LatestMeterReadingEvent(permissionId, latestReading));

            LOGGER.debug("Updated latest reading for {} to {}", permissionId, latestReading);

        } catch (Exception e) {
            LOGGER.error("Failed to extract meter reading timestamp", e);
        }
    }

    private void handleError(Throwable t) {
        LOGGER.error("Error in MeterReadingExtractor stream subscription", t);
    }
}
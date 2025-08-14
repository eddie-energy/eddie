package energy.eddie.regionconnector.at.eda.ponton.messenger.otel;

import energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapterConfiguration;
import energy.eddie.regionconnector.at.eda.ponton.messenger.client.FindMessagesClient;
import energy.eddie.regionconnector.at.eda.ponton.messenger.client.model.FindMessages;
import energy.eddie.regionconnector.at.eda.ponton.messenger.client.model.MessageCategory;
import energy.eddie.regionconnector.at.eda.ponton.messenger.client.model.Messages;
import energy.eddie.regionconnector.at.eda.ponton.messenger.client.model.Status;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Component
public class PontonQueueGauge implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(PontonQueueGauge.class);
    private final PontonXPAdapterConfiguration config;
    private final FindMessagesClient messagesClient;
    private final ObservableLongGauge gauge;

    public PontonQueueGauge(
            PontonXPAdapterConfiguration config,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") OpenTelemetry openTelemetry,
            FindMessagesClient messagesClient
    ) {
        this.config = config;
        this.gauge = asyncQueueSizeGauge(openTelemetry.getMeter(PontonQueueGauge.class.getName()));
        this.messagesClient = messagesClient;
    }

    @Override
    public void close() throws Exception {
        gauge.close();
    }

    private ObservableLongGauge asyncQueueSizeGauge(Meter meter) {
        var adapterId = config.adapterId();
        return meter
                .gaugeBuilder("ponton_%s_queue_size".formatted(adapterId))
                .setDescription("The amount of messages currently in transit in the Ponton X/P Messenger for the adapter: '%s'".formatted(adapterId))
                .ofLongs()
                .buildWithCallback(
                        observableMeasurement -> {
                            LOGGER.debug("Requesting queue size from ponton");
                            var res = getMessagesInQueue();
                            if (res != null) {
                                observableMeasurement.record(res.totalResultCount());
                            }
                        });
    }

    @Nullable
    private Messages getMessagesInQueue() {
        var lastHour = ZonedDateTime.now(ZoneOffset.UTC)
                                    .minusHours(1);
        var body = new FindMessages(config.adapterId(),
                                    Status.IN_TRANSIT,
                                    lastHour,
                                    MessageCategory.PRODUCTION);
        try {
            return messagesClient.findMessages(body)
                                 .block();
        } catch (Exception e) {
            if (e instanceof WebClientResponseException ex) {
                LOGGER.info("Encountered error when requesting messages in queue from ponton with body {}",
                            ex.getResponseBodyAsString(),
                            e);
            } else {
                LOGGER.info("Encountered error when requesting messages in queue from ponton", e);
            }
            return null;
        }
    }
}

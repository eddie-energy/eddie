package energy.eddie.regionconnector.de.eta.providers;

import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.api.agnostic.RawDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Provider for raw data messages from the DE-ETA region connector.
 * This implementation subscribes to the ValidatedHistoricalDataStream and
 * converts each validated historical data payload to a RawDataMessage.
 */
@Component
public class DeRawDataProvider implements RawDataProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeRawDataProvider.class);

    private final Flux<RawDataMessage> messages;

    public DeRawDataProvider(ValidatedHistoricalDataStream stream) {
        this.messages = stream.validatedHistoricalData()
                .map(this::toRawDataMessage)
                .doOnNext(msg -> LOGGER.atDebug()
                        .addArgument(msg::permissionId)
                        .log("Emitting raw data message for permission request {}"));
    }

    @Override
    public Flux<RawDataMessage> getRawDataStream() {
        return messages;
    }

    @Override
    public void close() {
        // No-Op: The underlying stream is managed by ValidatedHistoricalDataStream
    }

    private RawDataMessage toRawDataMessage(IdentifiableValidatedHistoricalData identifiableData) {
        // Use the raw JSON from the ETA Plus API response
        String rawPayload = identifiableData.payload().rawJson();
        return new RawDataMessage(identifiableData.permissionRequest(), rawPayload);
    }
}


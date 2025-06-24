package energy.eddie.aiida.adapters.datasource.inbound;

import energy.eddie.aiida.adapters.datasource.MqttDataSourceAdapter;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.record.InboundRecord;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class InboundAdapter extends MqttDataSourceAdapter<InboundDataSource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(InboundAdapter.class);
    private final Sinks.Many<InboundRecord> inboundRecordSink;

    /**
     * Creates the adapter for the inbound data source. It connects to the specified MQTT broker and expects that the
     * EP publishes its messages on the specified topic.
     *
     * @param dataSource The entity of the data source.
     */
    public InboundAdapter(InboundDataSource dataSource) {
        super(dataSource, LOGGER);
        inboundRecordSink = Sinks.many().unicast().onBackpressureBuffer();
    }

    /**
     * MQTT callback function that is called when a new message from the broker is received. Will store the message in
     * plaintext format in the database, as this datasource is not designed to parse the messages.
     *
     * @param topic   Name of the topic, the message was published to.
     * @param message The actual message.
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) {
        LOGGER.trace("Topic {} new message: {}", topic, message);

        var inboundRecord = new InboundRecord(
                Instant.now(),
                dataSource().asset(),
                dataSource().userId(),
                dataSource().id(),
                new String(message.getPayload(), StandardCharsets.UTF_8)
        );
        inboundRecordSink.tryEmitNext(inboundRecord);
    }

    /**
     * Will always throw {@link UnsupportedOperationException}, as this datasource is not designed to publish data.
     *
     * @param token The delivery token associated with the message.
     * @throws UnsupportedOperationException Always thrown, as this datasource is not designed to publish data.
     */
    @Override
    public void deliveryComplete(IMqttToken token) throws UnsupportedOperationException {
        LOGGER.warn(
                "Got deliveryComplete notification, but {} mustn't publish any MQTT messages but just listen. Token was {}",
                InboundAdapter.class.getName(),
                token);
        throw new UnsupportedOperationException("The " + InboundAdapter.class.getName() + " mustn't publish any MQTT messages");
    }

    public Flux<InboundRecord> inboundRecordFlux() {
        return inboundRecordSink.asFlux();
    }
}
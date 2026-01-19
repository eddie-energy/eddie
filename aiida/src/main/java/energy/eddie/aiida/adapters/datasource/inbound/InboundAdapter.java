package energy.eddie.aiida.adapters.datasource.inbound;

import energy.eddie.aiida.adapters.datasource.MqttDataSourceAdapter;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.errors.SecretLoadingException;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.aiida.services.secrets.SecretsService;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
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
    private final SecretsService secretsService;

    /**
     * Creates the adapter for the inbound data source. It connects to the specified MQTT broker and expects that the
     * EP publishes its messages on the specified topic.
     *
     * @param dataSource     The entity of the data source.
     * @param secretsService The secrets service to load the plaintext password
     */
    public InboundAdapter(
            InboundDataSource dataSource,
            MqttConfiguration mqttConfiguration,
            SecretsService secretsService
    ) {
        super(dataSource, LOGGER, mqttConfiguration);
        this.inboundRecordSink = Sinks.many().unicast().onBackpressureBuffer();
        this.secretsService = secretsService;
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

    public Flux<InboundRecord> inboundRecordFlux() {
        return inboundRecordSink.asFlux();
    }

    @Override
    protected MqttConnectionOptions createConnectOptions() {
        var connectOptions = super.createConnectOptions();

        try {
            connectOptions.setUserName(dataSource().username());
            connectOptions.setPassword(secretsService.loadSecret(dataSource.password())
                                                     .getBytes(StandardCharsets.UTF_8));
        } catch (SecretLoadingException e) {
            LOGGER.error("Could not load secrets to connect to data source.", e);
        }

        return connectOptions;
    }
}